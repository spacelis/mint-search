/**
  * Enumerate graphs based on a given set of nodes and their connectivity
  */
package uk.ac.cdrc.mintsearch.graph

import org.neo4j.graphdb.Path
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext

import scala.annotation.tailrec
import scala.collection.JavaConverters._

/**
  * This class defines a procedure to assemble embeddings from the ranking lists of nodes.
  * The principal in this procedure is to find all connected components of the nodes in
  * the graph store. The connection is defined by the traversalDescription which may not
  * require all the node on the connecting path in the embeddings. So the assembled
  * subgraphs may have extra nodes other than the embeddings to indicate that the nodes in
  * an embeddings are connected.
  */
trait SubGraphEnumeratorContext {
  self: GraphDBContext with TraversalStrategy with NeighbourAwareContext =>

  /**
    * Assemble graphs from dangled nodes
    * @param dangled is a set of nodeIds
    * @return a stream of sub graph stores from the dangled nodes
    */
  @tailrec
  final def composeGraphs(dangled: Set[NodeId], acc: Seq[GraphSnippet] = Seq.empty): Seq[GraphSnippet] = {
    dangled.toList match {
      case x :: xs =>
        val seed = dangled.take(1)
        val subGraph = expandingSubGraph(seed, dangled)
        composeGraphs(dangled -- subGraph.nodeIds, acc :+ subGraph)
      case Nil =>
        acc
    }
  }

  /**
    * Expanding a seed set of nodes to its maximum size of sub graph within the graph store
    * @param seedNodes is a set of nodes
    * @param range is a set of nodes indicating the boundary of neighbour searching, only
    *              nodes within the range will be considered in the returned sub graphs
    * @return return the biggest sub graph expanding from the seed nodes within the range
    */
  def expandingSubGraph(seedNodes: Set[NodeId], range: Set[NodeId]): GraphSnippet = {
    val (nodeIds, path) = stepExpandingSubGraph(seedNodes, Map.empty, range).reduce((_, b) => b)
    val nodes = for (n <- nodeIds) yield db.getNodeById(n)
    val relationships = (for (p <- path.values; r <- p.relationships().asScala) yield r.getId) map db.getRelationshipById
    GraphSnippet(nodes.toList, relationships.toList)
  }

  /**
    * A method defining the intermedia step for expanding a sub graph from a seed set of nodes
    * @param seedNodes is a set of nodes to start from
    * @param seedPaths is a set of paths carried forward for later assembling
    * @param range is the range for sub graph boundaries
    * @return a series steps towards the maxim range of sub graphs
    */
  def stepExpandingSubGraph(seedNodes: Set[NodeId], seedPaths: Map[NodeId, Path], range: Set[NodeId]): Stream[(Set[NodeId], Map[NodeId, Path])] = {
    val pathToNeighbours = (for {
      nid <- seedNodes & range
      p <- db.getNodeById(nid).NeighboursIn(range)
    } yield p.endNode().getId -> p).toMap

    if (pathToNeighbours.isEmpty)
      (seedNodes, seedPaths) #:: Stream.empty
    else
      (seedNodes, seedPaths) #:: stepExpandingSubGraph(seedNodes ++ pathToNeighbours.keySet, seedPaths ++ pathToNeighbours, range -- seedNodes)
  }

}
