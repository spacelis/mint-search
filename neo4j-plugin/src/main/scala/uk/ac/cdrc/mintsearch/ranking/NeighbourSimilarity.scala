package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.WeightedLabelSet

/**
  * Created by ucfawli on 15-Jan-17.
  */
trait NeighbourSimilarity {
  def measureSimilarity(wls: WeightedLabelSet, other: WeightedLabelSet): Double
}

trait SimpleNeighbourSimilarity extends NeighbourSimilarity{
  import uk.ac.cdrc.mintsearch.asWightedLabelSetWrapper

  override def measureSimilarity(wls: WeightedLabelSet, other: WeightedLabelSet): Double = {
    (wls ~~ other).values.sum
  }

}
