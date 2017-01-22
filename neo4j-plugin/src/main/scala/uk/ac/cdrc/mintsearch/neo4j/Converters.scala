package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.cypher.export.CypherResultSubGraph

/**
 * Created by ucfawli on 08-Jan-17.
 */
object Converters {

  implicit def asCypherResultSubGraph(subGraphStore: GraphSnippet): CypherResultSubGraph = {
    val cypherResultSubGraph = new CypherResultSubGraph()
    subGraphStore.nodes.foreach(cypherResultSubGraph.add)
    subGraphStore.relationships.foreach(cypherResultSubGraph.add)
    cypherResultSubGraph
  }

}
