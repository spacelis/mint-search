/**
 * Defines which nodes are considered as neighbours
 */
package uk.ac.cdrc.mintsearch.neighbourhood

import org.neo4j.graphdb.RelationshipType
import org.neo4j.graphdb.traversal.{ Evaluators, TraversalDescription, Uniqueness }
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription

trait TraversalStrategy {
  val traversalDescription: TraversalDescription
}

trait NeighbourhoodByRadiusAndRelationships extends TraversalStrategy {
  val radius: Int
  val relTypes: Seq[String]
  override lazy val traversalDescription: TraversalDescription =
    relTypes.foldLeft(new MonoDirectionalTraversalDescription(): TraversalDescription)((td, rType) => td.relationships(RelationshipType.withName(rType)))
      .uniqueness(Uniqueness.NODE_GLOBAL)
      .evaluator(Evaluators.toDepth(radius))

  override def toString: String = s"td$radius"
}

trait NeighbourhoodByRadius extends TraversalStrategy {
  val radius: Int
  override lazy val traversalDescription: TraversalDescription = new MonoDirectionalTraversalDescription()
    .uniqueness(Uniqueness.NODE_GLOBAL)
    .evaluator(Evaluators.toDepth(radius))

  override def toString: String = s"td$radius"
}

object TraversalStrategy {

}
