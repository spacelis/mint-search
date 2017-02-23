/**
  * Defines the similarity measurements between nodes
  */
package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.index.LabelMaker

trait NodeSimilarity {
  self: LabelMaker =>
  type Score = Double
  implicit val scoreOrd: Ordering[Score]
  def similarity(wls: WeightedLabelSet[L], other: WeightedLabelSet[L]): Score
}

trait SimpleNodeSimilarity extends NodeSimilarity {
  self: LabelMaker =>
  import uk.ac.cdrc.mintsearch.asWightedLabelSetWrapper

  implicit override val scoreOrd: Ordering[Score] = Ordering.Double.reverse

  override def similarity(wls: WeightedLabelSet[L], other: WeightedLabelSet[L]): Score = {
    other.values.sum - (other ~~ wls).values.sum
  }

}


trait NessNodeSimilarity extends NodeSimilarity {
  self: LabelMaker =>
  import uk.ac.cdrc.mintsearch.asWightedLabelSetWrapper

  implicit override val scoreOrd: Ordering[Score] = Ordering.Double

  override def similarity(wls: WeightedLabelSet[L], other: WeightedLabelSet[L]): Score = {
    (other ~~ wls).values.sum
  }
}
