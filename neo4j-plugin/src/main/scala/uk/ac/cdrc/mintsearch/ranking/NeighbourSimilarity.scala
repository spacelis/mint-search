package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.WeightedLabelSet

/**
  * Created by ucfawli on 15-Jan-17.
  */
trait NeighbourSimilarity {
  def measureSimilarity(weightedLabelSet: WeightedLabelSet)
}
