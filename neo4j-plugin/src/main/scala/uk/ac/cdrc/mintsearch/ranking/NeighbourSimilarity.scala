package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.index.LabelMaker

/**
 * Created by ucfawli on 15-Jan-17.
 */
trait NeighbourSimilarity {
  self: LabelMaker =>
  def measureSimilarity(wls: WeightedLabelSet[L], other: WeightedLabelSet[L]): Double
}

trait SimpleNeighbourSimilarity extends NeighbourSimilarity {
  self: LabelMaker =>
  import uk.ac.cdrc.mintsearch.asWightedLabelSetWrapper

  override def measureSimilarity(wls: WeightedLabelSet[L], other: WeightedLabelSet[L]): Double = {
    (wls ~~ other).values.sum
  }

}
