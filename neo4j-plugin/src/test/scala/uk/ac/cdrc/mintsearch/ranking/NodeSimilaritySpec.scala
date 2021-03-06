package uk.ac.cdrc.mintsearch.ranking

import org.scalatest.{Matchers, WordSpec}
import uk.ac.cdrc.mintsearch.index.PropertyLabelMaker

/**
  * Tests for node similarities
  */
class NodeSimilaritySpec extends WordSpec with Matchers {
  "NESSSimilarity" should {
    "work generally" in new NESSSimilarity with PropertyLabelMaker {
      override val labelStorePropKey: String = ""
      val a = Map(("name", "bob") -> 3d, ("name", "alice") -> 4d)
      val b = Map(("name", "bob") -> 4d, ("name", "alice") -> 4d)
      similarity(a, b) should be(7d)
      similarity(b, a) should be(7d)
      similarity(a, a) should be(7d)
      similarity(b, b) should be(8d)
    }

    "handle empty" in new NESSSimilarity with PropertyLabelMaker {
      override val labelStorePropKey: String = ""
      val a: Map[(String, String), Double] = Map()
      val b: Map[(String, String), Double] = Map(("name", "bob") -> 4d, ("name", "alice") -> 4d)
      similarity(a, b) should be(0d)
      similarity(b, a) should be(0d)
      similarity(a, a) should be(0d)
      similarity(b, b) should be(8d)
    }

    "handle no intersection" in new NESSSimilarity with PropertyLabelMaker {
      override val labelStorePropKey: String = ""
      val a: Map[(String, String), Double] = Map(("name", "carl") -> 3d, ("name", "david") -> 2d)
      val b: Map[(String, String), Double] = Map(("name", "bob") -> 4d, ("name", "alice") -> 4d)
      similarity(a, b) should be(0d)
      similarity(b, a) should be(0d)
      similarity(a, a) should be(5d)
      similarity(b, b) should be(8d)
    }
  }
}
