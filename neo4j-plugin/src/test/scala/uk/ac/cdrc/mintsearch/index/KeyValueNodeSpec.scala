package uk.ac.cdrc.mintsearch.index

import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.scalacheck.Arbitrary._
import org.scalatest.{Matchers, WordSpec}
import uk.ac.cdrc.mintsearch.WeightedLabelSet

/**
  * Test KeyValueLabelType
  */
class KeyValueNodeSpec extends WordSpec with Matchers {

//  implicit val arbEntry = for {
//    a <- Arbitrary.arbString.arbitrary
//    b <- Arbitrary.arbString.arbitrary
//    c <- Arbitrary.arbDouble.arbitrary
//  } yield ((a, b), c)
//
//  implicit val arbWLS = for {
//    es <- Arbitrary.arbitrary[List[((String, String), Double)]]
//  } yield es.toMap

  "KeyValueLabelType" should {
    "encode and decode WLS" in new KeyValueNode {
      forAll {
        (wls: Map[(String, String), Double]) =>
          wls == deJSONfy(JSONfy(wls))
      }
    }
  }
}
