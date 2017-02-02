/**
 * Defines node ranking methods
 */
package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.index.{BaseIndexReader, LabelMaker}

trait NodeRanking {
  self: LabelMaker =>
  def rankNode(ref: WeightedLabelSet[L], seq: Seq[WeightedLabelSet[L]]): Iterator[WeightedLabelSet[L]]
}

trait SimpleNodeRanking extends NodeRanking {
  self: BaseIndexReader with NeighbourSimilarity with LabelMaker =>
  override def rankNode(query: WeightedLabelSet[L]): Iterator[(WeightedLabelSet[L], Double)] = {
    getNodes(query.keySet) map (w => w -> measureSimilarity(w, query))
  }
}
