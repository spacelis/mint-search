package uk.ac.cdrc.mintsearch

import org.scalatest.{ Matchers, WordSpec }

/**
 *  Tests for NeighbourAwareNodeWrapper
 */
class WeightedLabelSetWrapperSpec extends WordSpec with Matchers {

  "NodeWraper" should {
    "tokenize WLS" in {
      WeightedLabelSetWrapper(Map("a" -> 0.5, "b" -> 0.5)).tokenized should be("a b")
    }
  }

}
