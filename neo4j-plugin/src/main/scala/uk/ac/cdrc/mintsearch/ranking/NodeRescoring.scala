package uk.ac.cdrc.mintsearch.ranking

import breeze.linalg.DenseMatrix
import breeze.numerics.abs
import uk.ac.cdrc.mintsearch.graph.Util.GraphMatrix
import uk.ac.cdrc.mintsearch.index.NodeDefContext
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext
import uk.ac.cdrc.mintsearch.{GraphDoc, NodeMatchingSet}

import scala.annotation.tailrec
import scala.collection.JavaConverters._

/**
  * Created by ucfawli on 26-Feb-17.
  */
trait NodeRescoring {
  self: NodeDefContext =>
  def rescore(query: GraphDoc[L], nodeMatchingSet: NodeMatchingSet): NodeMatchingSet
}

trait NeighbourhoodEnhancedScoring extends NodeRescoring {
  self: NodeDefContext with GraphDBContext=>

  def makeGraph(nodeMatchingSet: NodeMatchingSet): GraphMatrix = {
    val candidates = nodeMatchingSet.candidates
    val relationships = for {
      nid <- candidates
      n = db.getNodeById(nid)
      r <- n.getRelationships.asScala
      s = r.getStartNode.getId
      e = r.getEndNode.getId
      if (candidates contains s) && (candidates contains e)
    } yield s -> e
    GraphMatrix(candidates.toSeq, relationships.toSeq)
  }

  def rescore(query: GraphDoc[L], nodeMatchingSet: NodeMatchingSet): NodeMatchingSet = {
    implicit val ord = nodeMatchingSet.ord

    // Prepare adjacency matrix for query graph
    val vNodes = query.weights.keySet.toSeq
    val relationships = (for {
      (s, es) <- query.ajacency
      e <- es
    } yield s -> e).toSeq
    val vGraph = GraphMatrix(vNodes, relationships)

    // Prepare adjacency matrix for candidates in the target graph
    val uGraph = makeGraph(nodeMatchingSet)

    // Prepare score matrix
    val scores = DenseMatrix.zeros[Double](uGraph.proj.size, nodeMatchingSet.matching.size)
    for {
      (v, ns) <- nodeMatchingSet.matching
      (n, s) <- ns
    } scores(uGraph.proj(n), vGraph.proj(v)) = s

    // Adjusting scores based on neighbour matchings and return adjusted NodeMatching Set
    val adjustedScores = neighbourScoring(scores, vGraph, uGraph)
    val adjustedMatching = for {
      (v, ns) <- nodeMatchingSet.matching
    } yield v -> (ns map {case (n, s) => n -> adjustedScores(vGraph.proj(v), uGraph.proj(n))}).sortBy(_._2)

    NodeMatchingSet(adjustedMatching)
  }

  val neighbourInfluence: Double = 0.3

  /**
    * Adjust scores of matching by neighbour scores
    * S_u = U *> SV / |V|
    * where |.| is the order of the square matrix, U is matching nodes' adjacency and V is query nodes' adjacency.
    * S is the score matrix of u by v.
    * @param scores the score matrix
    * @param vGraph the query graph in matrix of adjacency
    * @param uGraph the target graph in matrix of adjacency
    * @param maxIter the maximum iteration of inference
    * @return updated score
    */
  @tailrec
  final def neighbourScoring(scores: DenseMatrix[Double],
                             vGraph: GraphMatrix,
                             uGraph: GraphMatrix,
                             maxIter: Int = 10)(implicit ord: Ordering[Double]): DenseMatrix[Double] = {
    val nbs = scores * vGraph.mat.map(_.toDouble) / vGraph.proj.size.toDouble
    val updatedScore = findBest(uGraph.mat.map(_.toDouble), nbs * neighbourInfluence) + scores * (1 - neighbourInfluence)
    if (abs(updatedScore - scores).forall(_ < 1e-1) || (maxIter <= 0))
      updatedScore
    else
      neighbourScoring(updatedScore, vGraph, uGraph, maxIter - 1)
  }

  def findBest(U: DenseMatrix[Double], S: DenseMatrix[Double])(implicit ord: Ordering[Double]): DenseMatrix[Double] = {
    val r = DenseMatrix.zeros[Double](U.rows, S.cols)
    for {
      i <- 1 until U.rows
      j <- 1 until S.cols
    } r(i, j) = nonZeroMin((U(i, ::).t :* S(::, j)).data)
    r
  }

  def nonZeroMin(x: Array[Double])(implicit ord: Ordering[Double]): Double = x.filter(_ > 0.0d).sorted.head
}
