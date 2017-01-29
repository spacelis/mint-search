/**
 * Defines node ranking methods
 */
package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.index.LabelMaker

trait NodeRanking {
  self: LabelMaker =>
  def rankByNode(ref: WeightedLabelSet[L], seq: Seq[WeightedLabelSet[L]]): Iterator[WeightedLabelSet[L]]
}

trait SimpleNodeRanking extends NodeRanking {
  self: NeighbourSimilarity with LabelMaker =>
  override def rankByNode(ref: WeightedLabelSet[L], seq: Seq[WeightedLabelSet[L]]): Iterator[WeightedLabelSet[L]] = {
    ((for (w <- seq) yield (w, measureSimilarity(w, ref))).sortBy(_._2) map { _._1 }).toIterator
  }
}
