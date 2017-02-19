/**
  * Enumerate graphs based on a given set of nodes and their connectivity
  */
package uk.ac.cdrc.mintsearch.graph

import org.neo4j.graphdb.Path
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext

import scala.annotation.tailrec
import scala.collection.JavaConverters._

trait EmbeddingEnumeratorContext {

  def composeEmbeddings(nodeMatchingSet: NodeMatchingSet): Stream[GraphEmbedding]
}

/**
  * This class defines a procedure to assemble embeddings from the ranking lists of nodes.
  * The principal in this procedure is to find all connected components of the nodes in
  * the graph store. The connection is defined by the traversalDescription which may not
  * require all the node on the connecting path in the embeddings. So the assembled
  * subgraphs may have extra nodes other than the embeddings to indicate that the nodes in
  * an embeddings are connected.
  */
trait ConnComponentEnumContext extends EmbeddingEnumeratorContext {
  self: GraphDBContext with TraversalStrategy with NeighbourAwareContext =>

  override def composeEmbeddings(nodeMatchingSet: NodeMatchingSet): Stream[GraphEmbedding] = {
    val nodes = (for {
      ns <- nodeMatchingSet.map.values
      n <- ns
    } yield n).toSet
    composeGraphs(nodes).toStream
  }
  /**
    * Assemble graphs from dangled nodes
    * @param dangled is a set of nodeIds
    * @return a stream of sub graph stores from the dangled nodes
    */
  @tailrec
  final def composeGraphs(dangled: Set[NodeId], acc: Seq[GraphEmbedding] = Seq.empty): Seq[GraphEmbedding] = {
    dangled.toList match {
      case x :: _ =>
        val subGraph = expandingSubGraph(Set(x), dangled)
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
  def expandingSubGraph(seedNodes: Set[NodeId], range: Set[NodeId]): GraphEmbedding = {
    val (nodeIds, path) = stepExpandingSubGraph(seedNodes, Map.empty, range).reduce((_, b) => b)
    val nodes = for (n <- nodeIds) yield db.getNodeById(n)
    val relationships = (for (p <- path.values; r <- p.relationships().asScala) yield r.getId) map db.getRelationshipById
    GraphEmbedding(nodes.toList, relationships.toList, List.empty)
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
      (seedNodes, seedPaths) #:: stepExpandingSubGraph(seedNodes ++ pathToNeighbours.keySet,
        seedPaths ++ pathToNeighbours, range -- seedNodes)
  }

}

trait NessEmbeddingEnumContext extends EmbeddingEnumeratorContext {
  self: GraphDBContext with TraversalStrategy with NeighbourAwareContext =>

  /**
    * Compose embeddings from the matching node sets
    *
    * The algorithm start with an arbitrary v and its corresponding list of target nodes
    * We construct the first set of embeddings by finding all connected components starting
    * with each node in the ranking list. Then we remove all the nodes already in the first
    * set of embeddings from noteMatchingSet and start all over again. Note all the target
    * nodes in v's list must be in one of the first set of embeddings, thus v is not in the
    * nodeMatchingSet of the second search.
    *
    * Two interwoven streams are defined to produce the final stream of embeddings. The first
    * one defines the batch of embeddings expanded from each target node of v. The second
    * extract all the found nodes from the batch of embeddings and remove them from nodeMatchingSet
    * to produce a new one for the next search which is defined in tail part of the first
    * stream.
    *
    * @param nodeMatchingSet a set of node ranking lists
    * @return a stream of embeddings
    */
  final override def composeEmbeddings(nodeMatchingSet: NodeMatchingSet): Stream[GraphEmbedding] = {
    lazy val embeddings: Stream[Stream[GraphEmbedding]] =
      initialEmbeddings(nodeMatchingSet) #:: (nms map composeEmbeddings)
    lazy val nms: Stream[NodeMatchingSet] =
      embeddings.scanLeft(nodeMatchingSet){_ removeValues _.flatMap(_.nodeIds).toSet}
    embeddings.flatten
  }

  /**
    * This method will return a stream of embeddings that can be found by starting from each of
    * the target node in v's list.
    * @param nodeMatchingSet a NodeMatchingSet
    * @return a stream of embeddings
    */
  def initialEmbeddings(nodeMatchingSet: NodeMatchingSet): Stream[GraphEmbedding] = {
    nodeMatchingSet.map.headOption match {
      case Some((v, ns)) => for {
        n <- ns.toStream
        e <- startFromNode(n, nodeMatchingSet.removeKeys(Seq(v)))
      } yield e
      case None => Stream.empty
    }
  }

  /**
    * Return a connect component by start from n within the range of all target nodes in
    * nodeMatchingSet
    * @param n a node not in nodeMatchingSet
    * @param nodeMatchingSet a NodeMatchingSet defining the search range
    * @return a sequence of embeddings
    */
  def startFromNode(n: NodeId, nodeMatchingSet: NodeMatchingSet): Seq[GraphEmbedding] = {
    val range = (db.getNodeById(n).neighbours map (x => x.endNode().getId -> x)).toMap
    expandingStep(nodeMatchingSet, range, List(n))
  }

  /**
    * A step through exhausting possible ways of expanding an embedding from
    * @param nodeMatchingSet a NodeMatchingSet to search for rest of embeddings
    * @param textile partial embeddings
    * @return a sequence of embeddings
    */
  def expandingStep(nodeMatchingSet: NodeMatchingSet, textile: Map[NodeId, Path], keyNodes: List[NodeId]): Seq[GraphEmbedding] = {
    val matched = nodeMatchingSet.map.values.flatten.toSet
    val expandables = matched & textile.keySet
    if (expandables.isEmpty) Seq(makeGraphEmbedding(textile, keyNodes))
    else for {
        n <- expandables.toSeq
        v = nodeMatchingSet.rev(n)
        patch = db.getNodeById(n).NeighboursIn(matched).toSeq
        neighours = patch map ((x: Path) => x.endNode().getId -> x)
        m <- expandingStep(nodeMatchingSet.removeKeys(Seq(v)), textile ++ neighours, n :: keyNodes)
      } yield m
  }

  def makeGraphEmbedding(textile: Map[NodeId, Path], keyNodes: List[NodeId]): GraphEmbedding = {
    val nodes = for {
      p <- textile.values.toList
      n <- p.nodes().asScala
    } yield n
    val relationships = for {
      p <- textile.values.toList
      r <- p.relationships().asScala
    } yield r
    GraphEmbedding(nodes.distinct, relationships.distinct, keyNodes)
  }
}
