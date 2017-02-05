package uk.ac.cdrc.mintsearch.graph

import org.neo4j.cypher.export.CypherResultSubGraph
import org.neo4j.graphdb.{Node, Relationship}
import uk.ac.cdrc.mintsearch.NodeId

/**
  * This class is an intermediate result collecting bin as a counter part in scala for
  * Neo4J's class implementing SubGraph
  *
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

