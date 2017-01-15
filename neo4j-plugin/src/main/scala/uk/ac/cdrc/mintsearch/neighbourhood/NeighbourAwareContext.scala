package uk.ac.cdrc.mintsearch.neighbourhood

import org.neo4j.graphdb.{Node, Path}
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.neo4j.GraphContext

import scala.collection.JavaConverters._

/**
 * Created by ucfawli on 19-Nov-16.
 */

trait NeighbourAwareContext {
  self: GraphContext with TraversalStrategy with PropagationStrategy =>

  class NeighbourVisitor(val node: Node) {

    /**
      * Find all neighbours of this node
      * @return an iterator of all the neighbours by the paths to them
      */
    def neighbours(): Iterator[Path] = traversalDescription.traverse(node).iterator().asScala

    /**
      * Identify all the neighbours in the subset
      * @param subset the target range of nodes to find in the neighbourhood
      * @return an iterator of all the neighbours by the paths to them
      * @see neighbours()
      */
    def neighboursIn(subset: Set[NodeId]): Iterator[Path] = for {
      path <- neighbours()
      if path.nodes().asScala forall { subset contains _.getId }
    } yield path


    def generalNeighboursIn(subset: Set[NodeId]): Iterator[Path] = for {
      path <- neighbours()
      if subset contains path.endNode().getId
    } yield path

  }

  case class NeighbourAwareNode(override val node: Node) extends NeighbourVisitor(node) {

    /**
      * Collect all the labels from the neighbours and use propagate function to assign weight to the labels and merge
      * them to a `WeightedLabelSet`
      * @return a `WeightedLabelSet` derived from the neighbourhood
      */
    def collectNeighbourLabels: WeightedLabelSet = {
      val label_weight_parts = for { path <- neighbours() } yield propagate(path)

      sum(label_weight_parts) // Aggregate the label weights.
    }
  }

  implicit def nodeWrapper(node: Node): NeighbourAwareNode = NeighbourAwareNode(node)
}

