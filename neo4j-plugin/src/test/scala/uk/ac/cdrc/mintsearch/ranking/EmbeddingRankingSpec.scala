package uk.ac.cdrc.mintsearch.ranking

import org.scalatest.{Matchers, WordSpec}
import uk.ac.cdrc.mintsearch.index.KeyValueNode

/**
  * Test graph ranking
  */
class EmbeddingRankingSpec extends WordSpec with Matchers {
  "SimpleGraphRanking" should {
    "generally work" in new SimpleEmbeddingRanking with SimpleNodeSimilarity with NodeSearchResultContext with KeyValueNode {
      val graphScores: IndexedSeq[(Int, Double)] = graphScoring(
        IndexedSeq(Map(1l -> (1l, 0.5), 2l -> (2l, 0.5), 3l -> (3l, 0.1)), Map(4l -> (1l, 0.2), 5l -> (2l, 1.0)))
      )
      graphScores should contain theSameElementsAs Seq(0l -> 1.1d, 1l -> 1.2d)
    }
  }
}
