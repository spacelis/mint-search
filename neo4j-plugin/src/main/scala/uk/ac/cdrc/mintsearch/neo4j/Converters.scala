package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.cypher.export.CypherResultSubGraph
import org.neo4j.graphdb.{Path, RelationshipType}
import org.neo4j.graphdb.traversal.{Evaluators, TraversalDescription, Uniqueness}
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription

/**
  * Created by ucfawli on 08-Jan-17.
  */
object Converters {

  implicit def asCypherResultSubGraph(subGraphStore: SimpleGraphSnippet): CypherResultSubGraph = {
    val cypherResultSubGraph = new CypherResultSubGraph()
    subGraphStore.nodes.foreach(cypherResultSubGraph.add)
    subGraphStore.relationships.foreach(cypherResultSubGraph.add)
    cypherResultSubGraph
  }

}
