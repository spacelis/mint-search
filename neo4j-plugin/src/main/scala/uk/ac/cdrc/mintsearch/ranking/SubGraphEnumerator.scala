package uk.ac.cdrc.mintsearch.ranking

import scala.collection.JavaConverters._
import org.neo4j.graphdb.{GraphDatabaseService, Node, Path, Relationship}
import org.neo4j.graphdb.traversal.TraversalDescription
import uk.ac.cdrc.mintsearch.ranking.NeighbourBasedRanking.NodeId
import uk.ac.cdrc.mintsearch.ranking.NeighbourAwareNode.wrapNode

/**
  * Created by ucfawli on 11/22/16.
  */

case class GraphStore(nodes: List[Node], relationships: List[Relationship]) {
  def addNode(n: Node) = GraphStore(n :: nodes, relationships)
  def addNodes(ns: Iterable[Node]) = GraphStore(ns.toList ++ nodes, relationships)
  def addRelationship(r: Relationship) = GraphStore(nodes, r :: relationships)
  def addRelationships(rs: Iterable[Relationship]) = GraphStore(nodes, rs.toList ++ relationships)
}

class SubGraphEnumerator(td: TraversalDescription, db: GraphDatabaseService) {

  private implicit val wrapper = wrapNode

  def nestMap[T, U](xs: List[List[T]])(f: T => U): List[List[U]] =
    xs map {_ map f}

  //TODO Use searchNeighours for each of the nodes in the first list
  def iterateInRankingList(nodeList: Map[NodeId, List[NodeId]]): Iterator[GraphStore] = ???

  private def expandCandidates(candidateNodes: Map[NodeId, Path], newMatchedNode: Node): Map[NodeId, Path] = {
    val neighbours = (for {
      p <- newMatchedNode.neighbours()
      n = p.endNode()
    } yield (n.getId() -> p)).toMap

    (candidateNodes ++ neighbours) - newMatchedNode.getId
  }

  def searchNeighbours(assembled: Set[NodeId], parts: Set[NodeId], candidateNodes: Map[NodeId, Path], gs: GraphStore): Iterator[GraphStore] = {
    candidateNodes.keySet & parts match {
      case Set.empty => Iterator(gs)
      case unmatchedNeighbours => {
        for {
          nid <- unmatchedNeighbours
          expanded = expandCandidates(candidateNodes, db.getNodeById(nid))
          p = candidateNodes(nid)
          newGS = gs.addNodes(p.nodes.asScala).addRelationships(p.relationships.asScala)
          gs <- searchNeighbours(assembled + nid, parts - nid, expanded, newGS)
        } yield gs
      }.toIterator
    }
  }

  def assembleSubGraph(dangled: Set[NodeId], assembled: Set[NodeId], candidates: Set[NodeId]): Stream[(GraphStore, Set[NodeId], Set[NodeId], Set[NodeId])] = ???

  def expandingSubGraph(seedNodes: Set[NodeId], seedPaths: Map[NodeId, Path], range: Set[NodeId]): Stream[(Set[NodeId], Map[NodeId, Path])] = {
    val neighbours = (for {
      nid <- seedNodes & range
      p <- db.getNodeById(nid).neighbours()
    } yield p.endNode().getId -> p).toMap

    (seedNodes, seedPaths) #:: expandingSubGraph(seedNodes ++ neighbours.keySet, seedPaths ++ neighbours, range)
  }
}
