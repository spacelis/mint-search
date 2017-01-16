package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.index.NeighbourAggregatedIndexReader

/**
  * Created by ucfawli on 15-Jan-17.
  */
trait NodeRanking {
  self: NeighbourAggregatedIndexReader =>
  def rankByNode(node: WeightedLabelSet): Iterator[WeightedLabelSet]
}
