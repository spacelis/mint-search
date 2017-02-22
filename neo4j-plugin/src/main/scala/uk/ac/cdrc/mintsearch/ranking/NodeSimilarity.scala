/**
  * Defines the similarity measurements between nodes
  */
package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.index.LabelMaker

trait NodeSimilarity {
  self: LabelMaker =>
  def similarity(wls: WeightedLabelSet[L], other: WeightedLabelSet[L]): Double
  def distance(wls: WeightedLabelSet[L], other: WeightedLabelSet[L]): Double
}

trait NessNodeSimilarity extends NodeSimilarity {
  self: LabelMaker =>
  import uk.ac.cdrc.mintsearch.asWightedLabelSetWrapper

  override def similarity(wls: WeightedLabelSet[L], other: WeightedLabelSet[L]): Double = {
    other.values.sum - (other ~~ wls).values.sum
  }

  override def distance(wls: WeightedLabelSet[L], other: WeightedLabelSet[L]): Double = {
    (other ~~ wls).values.sum
  }
}
