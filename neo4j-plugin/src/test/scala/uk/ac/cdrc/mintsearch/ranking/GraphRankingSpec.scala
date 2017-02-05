package uk.ac.cdrc.mintsearch.ranking

import org.scalatest.{Matchers, WordSpec}
import uk.ac.cdrc.mintsearch.index.KeyValueLabelType

/**
  * Test graph ranking
  */
class GraphRankingSpec extends WordSpec with Matchers {
  "SimpleGraphRanking" should {
    "generally work" in new SimpleGraphRanking with NodeSearchResultContext with KeyValueLabelType {
      val graphScores: IndexedSeq[(Int, Double)] = graphScoring(
        IndexedSeq(Set(1, 2, 3), Set(4, 5)),
        Map(1l -> 0.5, 2l -> 0.5, 3l -> 0.1, 4l -> 0.2, 5l -> 1.0)
      )
      graphScores.head should be(1l -> 1.2d)
      graphScores.tail.head should be(0l -> 1.1d)
    }
  }
}
