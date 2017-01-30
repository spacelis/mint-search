/**
 * Neighbourhood is the core concept in MintSearch.
 * A node is indexed by its neighbours but itself.
 */

package uk.ac.cdrc.mintsearch.neighbourhood

import org.neo4j.graphdb.{Node, Path}
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.index.LabelMaker
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext

import scala.collection.JavaConverters._

/**
 * This trait provides methods and objects to find neighbours in the graph
 */
trait NeighbourAwareContext {
  self: GraphDBContext with TraversalStrategy with PropagationStrategy with LabelMaker =>

  class NeighbourVisitor(val node: Node) {

    /**
     * Find all neighbours of this node including this node
     * @return an iterator of all the neighbours by the paths to them
     */
    def neighbourhood: Iterator[Path] = traversalDescription.traverse(node).iterator().asScala

    /**
     * Find all neighbours of this node but this node
     * @return an iterator of all neighbours by the paths to them
     */
    def neighbours: Iterator[Path] = for {
      p <- neighbourhood
      if p.endNode().getId != node.getId
    } yield p

    /**
     * Identify all the neighbours in the subset
     * @param subset the target range of nodes to find in the neighbourhood
     * @return an iterator of all the neighbours by the paths to them
     * @see neighbours()
     */
    def closeNeighboursIn(subset: Set[NodeId]): Iterator[Path] = for {
      path <- neighbours
      if path.nodes().asScala forall { subset contains _.getId }
    } yield path

    /**
     * Find paths to neighbours within a given subset
     * @param subset a target range of nodes
     * @return An iterator though the paths leading to the neighbour nodes
     */
    def NeighboursIn(subset: Set[NodeId]): Iterator[Path] = for {
      path <- neighbours
      if subset contains path.endNode().getId
    } yield path

  }

  /**
   * A wrapping class for nodes to add neighbourhood related function to them
   * @param node A node to wrap
   */
  case class NeighbourAwareNode(override val node: Node) extends NeighbourVisitor(node) {

    /**
     * Collect all the labels from the neighbours and use propagate function to assign weight to the labels and merge
     * them to a `WeightedLabelSet`
     * @return a `WeightedLabelSet` derived from the neighbourhood
     */
    def collectNeighbourhoodLabels: WeightedLabelSet[L] = {
      val label_weight_parts = for { path <- neighbourhood } yield propagate(path)

      sum(label_weight_parts) // Aggregate the label weights.
    }
  }

  /**
   * An implicit converter for node to wrapped node.
   * @param node A node to wrap
   * @return A wrapped node with neighbourhood related functions
   */
  implicit def nodeWrapper(node: Node): NeighbourAwareNode = NeighbourAwareNode(node)
}

