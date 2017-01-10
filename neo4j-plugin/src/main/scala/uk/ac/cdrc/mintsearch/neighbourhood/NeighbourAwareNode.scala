package uk.ac.cdrc.mintsearch.neighbourhood

import org.neo4j.graphdb.{Node, Path}
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.neo4j.Neo4JContainer

import scala.collection.JavaConverters._

/**
 * Created by ucfawli on 19-Nov-16.
 */
class NeighbourVisitor(val node: Node)(implicit container: Neo4JContainer with TraversalStrategy){

  /**
   * Find all neighbours of this node
   * @return an iterator of all the neighbours by the paths to them
   */
  def neighbours(): Iterator[Path] = container.traversalDescription.traverse(node).iterator().asScala

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

case class NeighbourAwareNode(override val node: Node)(implicit container: Neo4JContainer with TraversalStrategy with Propagation) extends NeighbourVisitor(node)(container){

  /**
    * Collect all the labels from the neighbours and use propagate function to assign weight to the labels and merge
    * them to a `WeightedLabelSet`
    * @return a `WeightedLabelSet` derived from the neighbourhood
    */
  def collectNeighbourLabels: WeightedLabelSet = {
    val label_weight_parts = for { path <- neighbours() } yield container.propagate(path)

    sum(label_weight_parts) // Aggregate the label weights.
  }
}

trait NeighbourAware {
  self: Neo4JContainer with TraversalStrategy with Propagation =>
  implicit def nodeWrapper(node: Node): NeighbourAwareNode = NeighbourAwareNode(node)(this)
}