package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.cypher.export.{ CypherResultSubGraph, SubGraph }

import scala.math.max
import org.neo4j.graphdb.traversal.{ Evaluators, TraversalDescription, Uniqueness }
import org.neo4j.graphdb.{ Node, Path, RelationshipType }
import uk.ac.cdrc.mintsearch.ranking.NeighbourBasedRanking._
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription

/**
 * Created by ucfawli on 11/18/16.
 *
 */

trait NeighbourBasedRanking extends GraphRanking {

  def traverDescription: TraversalDescription

  def propagate: Path => WeightedLabelSet

  def measureSimilarity(weightedLabelSet: WeightedLabelSet)

  implicit val nodeWrapper: (Node) => NeighbourAwareNode = NeighbourAwareNode.wrapNode(traverDescription)

  def rank(result: Iterator[WeightedLabelSet], query: WeightedLabelSet)

  def mkGraphDoc(nodeSet: Set[Node]): GraphDoc =
    (for { n <- nodeSet } yield n.getId -> n.collectNeighbourLabels(propagate)).toMap

  /**
   * Return `CypherResultSubGraph`s from
   * @param nodeMatching the matching nodes (query nodes -> matched nodes)
   * @return an series sub graphs assembled from the node pool
   */
  def matchedEmbeddings(nodeMatching: NodeMatching): Iterator[SubGraph] = for {
    sgs <- SubGraphEnumerator(traverDescription, db).iterateEmbedding(nodeMatching)
  } yield sgs
}

object NeighbourBasedRanking {

  type NodeId = Long

  /**
   * A mapping from label to a weight value
   */
  type WeightedLabelSet = Map[String, Double]

  /**
   * A mapping from a node (by its ID) to a list of nodes (IDs)
   */
  type NodeMatching = Map[NodeId, Seq[NodeId]]

  /**
   * A mapping from node to its weighted label set
   */
  type GraphDoc = Map[NodeId, WeightedLabelSet]

  /**
   * A class for adding operators to Map[String, Double] aliased to WeightedLabelSet
   * @param inner a value of type Map[String, Double]
   */
  class WeightedLabelSetWrapper(val inner: WeightedLabelSet) {
    def ~(other: WeightedLabelSetWrapper): WeightedLabelSet = {
      inner map { case (k, v) => (k, max(0.0, v - other.inner.getOrElse(k, 0.0))) }
    }

    def ~~(other: WeightedLabelSetWrapper): WeightedLabelSet = {
      this.~(other) filter {_._2 > 0.0}
    }

  }

  /**
   * Sum up a list of weight distributions
   * @param xs a list of WeightedLabelSets
   * @return a WeightedLabelSet in which the labels' weights are summed from xs
   */
  def sum(xs: TraversableOnce[WeightedLabelSet]): WeightedLabelSet =
    xs.flatMap(_.toSeq).toSeq.groupBy(_._1).mapValues(_.map(_._2).sum)

  /**
   * Implicit wrapping a value of type WeightedLabelSet (Map[String, Double]) to provide additional operator on them
   * @param wls a value of WeightedLabelSet
   * @return a wrapped Map[String, Double]
   */
  implicit def asWightedLabelSetWrapper(wls: WeightedLabelSet): WeightedLabelSetWrapper =
    new WeightedLabelSetWrapper(wls)

  implicit def asCypherResultSubGraph(subGraphStore: SubGraphStore): CypherResultSubGraph = {
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
