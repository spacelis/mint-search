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
case class GraphEmbedding(nodes: List[Node], relationships: List[Relationship], keyNodes: List[NodeId]) {
  lazy val nodeIds: List[NodeId] = for (n <- nodes) yield n.getId

  def toNeo4JSubGraph: CypherResultSubGraph = {
    val sg = new CypherResultSubGraph()
    for { n <- nodes } sg add n
    for { r <- relationships } sg add r
    sg
  }
}

object GraphEmbedding {
  implicit def toCypherResultSubGraph(subGraphStore: GraphEmbedding): CypherResultSubGraph = subGraphStore.toNeo4JSubGraph
}

