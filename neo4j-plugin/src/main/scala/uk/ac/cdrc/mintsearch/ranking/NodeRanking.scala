package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.WeightedLabelSet

/**
  * Created by ucfawli on 15-Jan-17.
  */
trait NodeRanking {
  def rankByNode(ref: WeightedLabelSet, seq: Seq[WeightedLabelSet]): Iterator[WeightedLabelSet]
}

trait SimpleNodeRanking extends NodeRanking{
  self: NeighbourSimilarity =>
  override def rankByNode(ref: WeightedLabelSet, seq: Seq[WeightedLabelSet]): Iterator[WeightedLabelSet] = {
    ((for (w <- seq) yield (w, measureSimilarity(w, ref))).sortBy(_._2) map {_._1}).toIterator
  }
}
