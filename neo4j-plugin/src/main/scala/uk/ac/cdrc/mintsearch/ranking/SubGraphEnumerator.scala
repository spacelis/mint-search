package uk.ac.cdrc.mintsearch.ranking

import scala.collection.JavaConverters._
import org.neo4j.graphdb.{GraphDatabaseService, Node, Path, Relationship}
import org.neo4j.graphdb.traversal.TraversalDescription
import uk.ac.cdrc.mintsearch.ranking.NeighbourBasedRanking.{NodeId, NodeMatching}
import uk.ac.cdrc.mintsearch.ranking.NeighbourAwareNode.wrapNode

/**
  * Created by ucfawli on 11/22/16.
  */

case class GraphStore(nodes: List[Node], relationships: List[Relationship]) {
  def addNode(n: Node) = GraphStore(n :: nodes, relationships)
  def addNodes(ns: Iterable[Node]) = GraphStore(ns.toList ++ nodes, relationships)
  def addRelationship(r: Relationship) = GraphStore(nodes, r :: relationships)
  def addRelationships(rs: Iterable[Relationship]) = GraphStore(nodes, rs.toList ++ relationships)
  def nodeIds: List[NodeId] = for (n <- nodes) yield n.getId
}

case class SubGraphEnumerator(td: TraversalDescription, db: GraphDatabaseService) {

  private implicit val wrapper = wrapNode

  def nestMap[T, U](xs: List[List[T]])(f: T => U): List[List[U]] =
    xs map {_ map f}

  def iterateEmbedding(nodeMatching: NodeMatching): Iterator[GraphStore] = {
    val nodeSet = (for {
      nl <- nodeMatching.values
      n <- nl
    } yield n).toSet
    assembleSubGraph(nodeSet).toIterator
  }

  def assembleSubGraph(dangled: Set[NodeId]): Stream[GraphStore] = {
    val seed = dangled.take(1)
    val subGraph = expandingSubGraph(seed, dangled)
    subGraph #:: assembleSubGraph(dangled -- subGraph.nodeIds)
  }

  def expandingSubGraph(seedNodes: Set[NodeId],  range: Set[NodeId]): GraphStore = {
    val (nodeIds, path) = stepExpandingSubGraph(seedNodes, Map.empty, range).reduce((a, b) => b)
    val nodes = for (n <- nodeIds) yield db.getNodeById(n)
    val relationships = (for (p <- path.values; r <- p.relationships().asScala) yield r.getId) map db.getRelationshipById
    GraphStore(nodes.toList, relationships.toList)
  }

  def stepExpandingSubGraph(seedNodes: Set[NodeId], seedPaths: Map[NodeId, Path], range: Set[NodeId]): Stream[(Set[NodeId], Map[NodeId, Path])] = {
    val pathToNeighbours = (for {
      nid <- seedNodes & range
      p <- db.getNodeById(nid).neighbours()
    } yield p.endNode().getId -> p).toMap

    val matched = pathToNeighbours.keySet & range
    val pathToMatched = (for (n <- matched) yield n -> pathToNeighbours(n)).toMap

    if (matched.isEmpty)
      (seedNodes, seedPaths) #:: Stream.empty
    else
      (seedNodes, seedPaths) #:: stepExpandingSubGraph(seedNodes ++ matched, seedPaths ++ pathToMatched, range -- pathToNeighbours.keySet)
  }
}
