package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.graphdb.{ Node, Path }
import uk.ac.cdrc.mintsearch.ranking.NeighbourBasedRanking.{ NodeId, WeightedLabelSet, sum }

import scala.collection.JavaConverters._

/**
 * Created by ucfawli on 19-Nov-16.
 */
trait NeighbourVisitor {

  val node: Node

  val traversalDescription: TraversalDescription

  /**
   * Collect all the labels from the neighbours and use propagate function to assign weight to the labels and merge
   * them to a `WeightedLabelSet`
   * @param propagate a function to derive the labels and their weights of a neighbour given the path to that neighbour
   * @return a `WeightedLabelSet` derived from the neighbourhood
   */
  def collectNeighbourLabels(propagate: Path => WeightedLabelSet): WeightedLabelSet = {

    val label_weight_parts = for { path <- neighbours() } yield propagate(path)

    // Aggregate the label weights.
    sum(label_weight_parts)
  }

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

case class NeighbourAwareNode(node: Node, traversalDescription: TraversalDescription) extends NeighbourVisitor

object NeighbourAwareNode {

  /**
   * This is a help function for creating a wrapped node
   * When an the returned function is assigned to a implicit val,
   * the wrapping can be done automatically when necessary.
   * For example,
   * <pre>
   * implicit val nodeWrapper = wrapNode(someTraversalDescription)
   * for (n <- node.neighbours()) println(n)
   * </pre>
   * @param traversalDescription defines how neighbours are to be find
   * @return
   */
  def wrapNode(implicit traversalDescription: TraversalDescription): (Node) => NeighbourAwareNode =
    (node: Node) => NeighbourAwareNode(node, traversalDescription)
}
