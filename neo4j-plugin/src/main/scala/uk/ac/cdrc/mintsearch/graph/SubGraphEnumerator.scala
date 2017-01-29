/**
 * Enumerate graphs based on a given set of nodes and their connectivity
 */
package uk.ac.cdrc.mintsearch.graph

import org.neo4j.cypher.export.CypherResultSubGraph
import org.neo4j.graphdb.{ Node, Path, Relationship }
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.neighbourhood.{ NeighbourAwareContext, TraversalStrategy }
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext

import scala.collection.JavaConverters._

/**
 * This class is an intermediate result collecting bin as a counter part in scala for
 * Neo4J's class implementing SubGraph
 * @param nodes the nodes in a sub graph
 * @param relationships the relationships in a sub graph
 */
case class GraphSnippet(nodes: List[Node], relationships: List[Relationship]) {
  def addNode(n: Node) = GraphSnippet(n :: nodes, relationships)
  def addNodes(ns: Iterable[Node]) = GraphSnippet(ns.toList ++ nodes, relationships)
  def addRelationship(r: Relationship) = GraphSnippet(nodes, r :: relationships)
  def addRelationships(rs: Iterable[Relationship]) = GraphSnippet(nodes, rs.toList ++ relationships)
  lazy val nodeIds: List[NodeId] = for (n <- nodes) yield n.getId

  def toNeo4JSubGraph: CypherResultSubGraph = {
    val sg = new CypherResultSubGraph()
    for { n <- nodes } sg add n
    for { r <- relationships } sg add r
    sg
  }
}

object GraphSnippet {
  implicit def asCypherResultSubGraph(subGraphStore: GraphSnippet): CypherResultSubGraph = subGraphStore.toNeo4JSubGraph
}

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
   * This method is the main interface for iterating though the sub graphs from the ranking lists
   * of nodes.
   *
   * @param nodeMatching is a mapping between the queried graph nodes to similar nodes in the graph store
   * @return an iterator though the embeddings assembled from the pooled nodes
   */
  def iterateEmbedding(nodeMatching: NodeMatching): Iterator[GraphSnippet] = {
    val nodeSet = (for {
      nl <- nodeMatching.values
      n <- nl
    } yield n).toSet
    assembleSubGraph(nodeSet).toIterator
  }

  /**
   * Assemble graphs from dangled nodes
   * @param dangled is a set of nodeIds
   * @return a stream of sub graph stores from the dangled nodes
   */
  def assembleSubGraph(dangled: Set[NodeId]): Stream[GraphSnippet] = {
    dangled.toList match {
      case x :: xs =>
        val seed = dangled.take(1)
        val subGraph = expandingSubGraph(seed, dangled)
        subGraph #:: assembleSubGraph(dangled -- subGraph.nodeIds)
      case Nil =>
        Stream.empty
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
