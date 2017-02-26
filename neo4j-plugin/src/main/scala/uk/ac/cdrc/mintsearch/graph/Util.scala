package uk.ac.cdrc.mintsearch.graph

import breeze.linalg._
import uk.ac.cdrc.mintsearch.NodeId

import scala.annotation.tailrec

/**
  * General graph algorithms
  */
object Util {

  /**
    * A matrix form of a graph
    * @param mat a matrix describing the adjcency
    * @param proj a mapping of node id to row/col index
    */
  case class GraphMatrix(mat: DenseMatrix[Int], proj: Map[NodeId, Int]){
    lazy val invProj: Map[Int, NodeId] = for ((k, v) <- proj) yield v -> k
    /**
      * Whether the graph is connected as a whole
      */
    lazy val connected: Boolean = {
      lazy val reachableInHops: Stream[DenseMatrix[Int]] = mat #:: reachableInHops map { _ * mat}
      val reachable = reachableInHops.take(mat.cols)
        .foldLeft(DenseMatrix.zeros[Int](mat.rows, mat.cols))(_ + _)
      (for {
        i <- 1 until mat.cols
      } yield reachable(0, i)).forall(_ > 0)
    }
  }

  /**
    * Compute a matrix to the power of n
    * @param mat a matrix
    * @param n the number of multiplications
    * @return the matrix having been raised in power of n
    */
  def pow(mat: DenseMatrix[Int], n: Int): DenseMatrix[Int] =
    if (n < 1 || (mat.cols != mat.rows))
      throw new IllegalArgumentException(s"parameter n ($n) should not be less than 1.")
    else if (n==1) mat
    else _pow(mat, n)

  /**
    * Generate a binary value list starting from most significant bit
    * @param n an integer
    * @param acc a suffix of higher significant bits
    * @return a binary value list
    */
  @tailrec
  def toBinary(n: Int, acc: List[Int] = List.empty): List[Int] = n match {
    case 0|1 => n :: acc
    case _ => toBinary(n/2, (n % 2) :: acc)
  }

  def _pow(mat: DenseMatrix[Int], n: Int): DenseMatrix[Int] = {
    lazy val terms: Stream[DenseMatrix[Int]] = mat #:: (terms zip terms map {case(a, b) => a * b})
    val stepMul = terms.zip(toBinary(n).reverse)
      .filter(_._2 == 1).map(_._1)
      .scanLeft(DenseMatrix.eye[Int](mat.rows))(_ * _)
    stepMul.takeRight(1).head
  }

  /**
    * Converting an embedding to matrix form
    * @param em an embedding
    * @return a GraphMatrix
    */
  def toGraphMatrix(em: GraphEmbedding): GraphMatrix = {
    toGraphMatrix(
      em.nodeIds,
      em.relationships map {r => r.getStartNode.getId -> r.getEndNode.getId}
    )
  }

  /**
    * Make a GraphMatrix from nodes and relationships
    * @param nodeIds a set of node in terms of ids
    * @param relationships a set of relationships in terms of mapping
    * @return a GraphMatrix
    */
  def toGraphMatrix(nodeIds: Seq[NodeId], relationships: Seq[(NodeId, NodeId)]): GraphMatrix = {
    val proj = nodeIds.zipWithIndex.toMap
    val mat = DenseMatrix.zeros[Int](proj.size, proj.size)
    for {
      (s, e) <- relationships
      if (proj contains s) && (proj contains e)
    } {
      mat(proj(s), proj(e)) = 1
      mat(proj(e), proj(s)) = 1
    }
    GraphMatrix(mat, proj)
  }
}
