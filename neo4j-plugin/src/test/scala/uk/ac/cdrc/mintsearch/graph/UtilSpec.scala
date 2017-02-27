package uk.ac.cdrc.mintsearch.graph

import breeze.linalg.DenseMatrix
import org.scalatest.{Matchers, WordSpec}
import uk.ac.cdrc.mintsearch.graph.Util._

/**
  * Created by ucfawli on 26-Feb-17.
  */
class UtilSpec extends WordSpec with Matchers {

  "toBinary" should {
    "convert single bit" in {
      toBinary(0) should be (List(0))
      toBinary(1) should be (List(1))
      toBinary(2) should be (List(1, 0))
      toBinary(3) should be (List(1, 1))
      toBinary(10) should be (List(1, 0, 1, 0))
    }
  }

  "pow" should {
    "calculate power of 1" in {
      val mat = DenseMatrix((0, 1), (1, 0)) // [ 0 1 ; 1 0 ]
      pow(mat, 1) should be (mat)
    }
    "calculate power of n" in {
      val mat = DenseMatrix((0, 1), (1, 0)) // [ 0 1 ; 1 0 ]
      pow(mat, 2) should be (DenseMatrix(1 -> 0, 0 -> 1))
      pow(mat, 10) should be (DenseMatrix(1 -> 0, 0 -> 1))
    }
  }

  "GraphMatrix" should {
    "find a connected graph contected" in {
      val gm = GraphMatrix(Seq(1l,2l,3l,4l), Seq(1l->2l, 2l->3l, 3l->4l, 4l->1l))
      gm.connected should be (true)
    }

    "find a un-contected graph not connected" in {
      val gm = GraphMatrix(Seq(1l,2l,3l,4l), Seq(1l->2l, 3l->4l))
      gm.connected should be (false)
    }
  }
}
