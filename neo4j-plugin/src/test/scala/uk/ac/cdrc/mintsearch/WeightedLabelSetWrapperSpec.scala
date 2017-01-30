/**
 *  Tests for NeighbourAwareNodeWrapper
 */
package uk.ac.cdrc.mintsearch

import org.scalatest.{Matchers, WordSpec}

class WeightedLabelSetWrapperSpec extends WordSpec with Matchers {

  "NodeWrapper" should {

    "have cover-diff operation (non-negative subtraction)" in {
      val a = Map("a" -> 1.0, "b" -> 2.0)
      val b = Map("a" -> 3.0)
      a ~ b should be(Map("a" -> 0.0, "b" -> 2.0))
      a ~~ b should be(Map("b" -> 2.0))
    }

    "have sum" in {
      val a = Map("a" -> 1.0, "b" -> 2.0)
      val b = Map("a" -> 3.0)
      val c = Map("c" -> 1.0)
      sum(Seq(a, b, c)) should be(Map("a" -> 4.0, "b" -> 2.0, "c" -> 1.0))
    }
  }

}
