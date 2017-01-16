package uk.ac.cdrc

import scala.math.max

/**
  * Created by ucfawli on 08-Jan-17.
  */
package object mintsearch {

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
  case class WeightedLabelSetWrapper(inner: WeightedLabelSet) {
    def ~(other: WeightedLabelSetWrapper): WeightedLabelSet = {
      inner map { case (k, v) => (k, max(0.0, v - other.inner.getOrElse(k, 0.0))) }
    }

    def ~~(other: WeightedLabelSetWrapper): WeightedLabelSet = {
      this.~(other) filter {_._2 > 0.0}
    }

    def tokenized: String = inner.keySet mkString " "
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

}
