package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.cypher.export.CypherResultSubGraph
import org.neo4j.graphdb.RelationshipType
import org.neo4j.graphdb.traversal.{Evaluators, TraversalDescription, Uniqueness}
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription
import uk.ac.cdrc.mintsearch.ranking.SimpleGraphSnippet

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

  def neighbourhoodTraversalDescription(order: Int, relTypes: Seq[String]): TraversalDescription = {
    val td: TraversalDescription = new MonoDirectionalTraversalDescription()
    relTypes.foldLeft(td)((td, rType) => td.relationships(RelationshipType.withName(rType)))
      .uniqueness(Uniqueness.NODE_GLOBAL)
      .evaluator(Evaluators.toDepth(order))
  }
}
