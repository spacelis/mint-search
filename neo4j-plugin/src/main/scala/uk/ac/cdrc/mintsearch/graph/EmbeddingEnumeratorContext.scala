/**
  * Enumerate graphs based on a given set of nodes and their connectivity
  */
package uk.ac.cdrc.mintsearch.graph

import org.neo4j.graphdb.{Node, Path}
import org.slf4j.LoggerFactory
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext
import uk.ac.cdrc.mintsearch.ranking.NodeSimilarity

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
      ns <- nodeMatchingSet.matching.values
      n <- ns
    } yield n._1).toSet
    composeEmbeddings(nodes)(nodeMatchingSet)
  }

  private val logger = LoggerFactory.getLogger(classOf[ConnComponentEnumContext])

  def composeEmbeddings(nodeIds: Set[NodeId])(implicit nms: NodeMatchingSet): Stream[GraphEmbedding] = {
    val nodes: Set[Node] = nodeIds map (x => db.getNodeById(x))
    if (nodes.nonEmpty) {
      logger.debug("composeEmbeddings")
      lazy val leftNodes: Stream[Set[Node]] = nodes #:: (leftNodes zip component.takeWhile(_._1.nonEmpty) map {case(ns, (seeds, neighbours)) => ns -- seeds -- neighbours.keySet})
      lazy val component: Stream[(Set[Node], Map[Node, Path])] = leftNodes.takeWhile(_.size > 1) map { nodes =>
        logger.debug("$nodes")
        val seed = nodes.take(1)
        val rest = nodes -- seed
        (seed, findComponent(seed, rest))
      }
      component map { case (seed, neighbourPaths) =>
        val nodes = seed ++ neighbourPaths.values.flatMap(_.nodes().asScala)
        val relationships = neighbourPaths.values.flatMap(_.relationships().asScala).toSet
        val projection = nms.inverse.filterKeys(nodes map {_.getId})
        GraphEmbedding(nodes.toList, relationships.toList, projection)
      }
    } else {
      Stream.empty
    }
  }

  @tailrec
  final def findComponent(toExpand: Set[Node], range: Set[Node], acc: Map[Node, Path]=Map.empty): Map[Node, Path] = {
    val expanded: Map[Node, Path] = (for {
      n <- toExpand
      p <- n.neighbours
    } yield p.endNode() -> p).toMap
    val margin = (expanded.keySet -- acc.keySet) & range
    if (margin.nonEmpty)
      findComponent(margin, range -- toExpand, acc ++ expanded.filter((margin -- toExpand) contains _._1))
    else
      acc

  }

}

trait NessEmbeddingEnumContext extends EmbeddingEnumeratorContext {
  self: GraphDBContext with NodeSimilarity with TraversalStrategy with NeighbourAwareContext =>

  private val logger = LoggerFactory.getLogger(classOf[NessEmbeddingEnumContext])
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
  override def composeEmbeddings(nodeMatchingSet: NodeMatchingSet): Stream[GraphEmbedding] = {
    lazy val nms: Stream[NodeMatchingSet] = nodeMatchingSet #::
      (nms.takeWhile(_.nonEmpty) zip embeddings
        map {x => x._1 removeCandidates x._2.flatMap(_.projection.keySet).toSet})
    lazy val embeddings: Stream[Stream[GraphEmbedding]] = nms map initialEmbeddings
    embeddings.flatten
  }

  /**
    * This method will return a stream of embeddings that can be found by starting from each of
    * the target node in v's list.
    * @param nodeMatchingSet a NodeMatchingSet
    * @return a stream of embeddings
    */
  def initialEmbeddings(nodeMatchingSet: NodeMatchingSet): Stream[GraphEmbedding] = {
    if (nodeMatchingSet.matching.nonEmpty) {
      val (v, n, s) = nodeMatchingSet.matching.map(m => (m._1, m._2.head._1, m._2.head._2)).toVector.sortBy(_._3).head
      startFromNode(n, v, s, nodeMatchingSet.removeQueryNodes(Seq(v)))
    } else
      Stream.empty
  }

  /**
    * Return a connect component by start from n within the range of all target nodes in
    * nodeMatchingSet
    * @param n a node not in nodeMatchingSet
    * @param v the corresponding node in query
    * @param s the corresponding node score for query node
    * @param nodeMatchingSet a NodeMatchingSet defining the search range
    * @return a sequence of embeddings
    */
  def startFromNode(n: NodeId, v: NodeId, s: Double, nodeMatchingSet: NodeMatchingSet): Stream[GraphEmbedding] = {
    logger.info(s"start from $n")
    val nbs = (db.getNodeById(n).neighbours map (x => x.endNode().getId -> x)).toMap
    expandingStep(nodeMatchingSet.removeCandidates(Set(n)), nbs, Map(n -> (v, s)))
  }

  /**
    * A step through exhausting possible ways of expanding an embedding from
    * @param nodeMatchingSet a NodeMatchingSet to search for rest of embeddings
    * @param textile partial embeddings
    * @return a sequence of embeddings
    */
  def expandingStep(nodeMatchingSet: NodeMatchingSet, textile: Map[NodeId, Path], projection: Map[NodeId, (NodeId, Double)]): Stream[GraphEmbedding] = {
    val matched = nodeMatchingSet.matching.values.flatten.toSet
    val expansible = (matched filter {textile.keySet contains _._1}).toSeq.sortBy(_._2).map(_._1)
    if (expansible.isEmpty){
      Stream(makeGraphEmbedding(textile, projection))
    }
    else for {
        n <- expansible.toStream
        (v, s) = nodeMatchingSet.inverse(n)
        patch = db.getNodeById(n).NeighboursIn(matched.map(_._1)).toSeq
        neighours = patch map ((x: Path) => x.endNode().getId -> x)
        m <- expandingStep(nodeMatchingSet.removeQueryNodes(Seq(v)).removeCandidates(Set(n)), textile ++ neighours, projection + (n -> (v, s)))
      } yield {
        m
      }
  }

  def makeGraphEmbedding(textile: Map[NodeId, Path], projection: Map[NodeId, (NodeId, Double)]): GraphEmbedding = {
    val nodes = for {
      p <- textile.values.toList
      n <- p.nodes().asScala
    } yield n
    val relationships = for {
      p <- textile.values.toList
      r <- p.relationships().asScala
    } yield r
    GraphEmbedding((nodes ++ (projection.keySet map db.getNodeById)).distinct, relationships.distinct, projection)
  }
}


trait TopFirstEmbeddingEnumContext extends NessEmbeddingEnumContext {
  self: GraphDBContext with NodeSimilarity with TraversalStrategy with NeighbourAwareContext =>

  private val logger = LoggerFactory.getLogger(classOf[NessEmbeddingEnumContext])

  /**
    * Iterate through the growing size of node matching sets.
    * @param nodeMatchingSet a node matching set
    * @return a stream of graph embeddings
    */
  final override def composeEmbeddings(nodeMatchingSet: NodeMatchingSet): Stream[GraphEmbedding] = {
    lazy val nms: Stream[(NodeMatchingSet, Int)] = (nodeMatchingSet.take(1) -> 1) #:: (nms map {case (_, l) => nodeMatchingSet.take(l + 1) -> l})
    lazy val embeddings: Stream[Stream[GraphEmbedding]] = nms map {case (matching, _) => findEmbeddingsIn(matching)}
    embeddings.flatten
  }

  def findEmbeddingsIn(nodeMatchingSet: NodeMatchingSet): Stream[GraphEmbedding] = {
    initialEmbeddings(nodeMatchingSet).filter(_.projection.size == nodeMatchingSet.matching.size)
  }

}
