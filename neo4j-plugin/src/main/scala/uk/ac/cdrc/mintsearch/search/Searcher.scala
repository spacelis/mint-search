/**
  * Implementation of graph ranking based on neighbours
  */
package uk.ac.cdrc.mintsearch.search

import org.slf4j.LoggerFactory
import uk.ac.cdrc.mintsearch.graph._
import uk.ac.cdrc.mintsearch.index.{BaseIndexReader, LabelMaker}
import uk.ac.cdrc.mintsearch.neo4j._
import uk.ac.cdrc.mintsearch.ranking.{EmbeddingRanking, NodeRanking}
import uk.ac.cdrc.mintsearch.{GraphDoc, NodeMatchingSet}

case class GraphSearchResult(gsq: GraphQuery, graphSnippets: IndexedSeq[GraphEmbedding], scores: IndexedSeq[Double])

trait GraphSearcher {
  self: QueryAnalyzer with LabelMaker with EmbeddingEnumeratorContext =>
  def search(gsq: GraphQuery, limit: Int = 3): GraphSearchResult
}

trait TruncatedSearcher extends GraphSearcher {
  self: BaseIndexReader with GraphDBContext with LabelMaker with TraversalStrategy with NeighbourAwareContext with NeighbourAggregatedAnalyzer with NodeRanking with EmbeddingRanking with EmbeddingEnumeratorContext =>

  private val logger = LoggerFactory.getLogger(classOf[GraphSearcher])

  override def search(gsq: GraphQuery, limit: Int = 3): GraphSearchResult = {
    val analyzedQuery = analyze(gsq)
    logger.info(
      s"""query=
         |${analyzedQuery map {case (k, v) => s"V$k => $v"} mkString "\n"}
         |===========""".stripMargin)
    val (graphSnippets, scores) = graphDocSearch(analyzedQuery, limit).unzip
    GraphSearchResult(gsq, graphSnippets, scores)
  }

  def matchNodes(query: GraphDoc[L], limit: Int = Int.MaxValue): NodeMatchingSet = {
    val resultSets = for {
      (n, wls) <- query.toIndexedSeq
    } yield searchNodes(n, wls)

    val nodeMatchingSet = NodeMatchingSet((for {
      rs <- resultSets
    } yield rs.queryNode -> (rs.ranked zip rs.scores take limit map (m => m._1.getId -> m._2))).toMap)
    logger.info(
      s"""nodeRankLists=
         |${nodeMatchingSet.map map {case(k, v) => s"V$k => $v"} mkString "\n"}
         |=============""".stripMargin)
    nodeMatchingSet
  }

  def graphDocSearch(query: GraphDoc[L], limit: Int): IndexedSeq[(GraphEmbedding, Double)] = {
    val nodeMatchingSet = matchNodes(query)
    rankGraphs(query, composeEmbeddings(nodeMatchingSet).take(limit).toIndexedSeq)
  }

}

trait LargePoolTruncatedSearcher extends TruncatedSearcher {
  self: BaseIndexReader with GraphDBContext with LabelMaker with TraversalStrategy with NeighbourAwareContext with NeighbourAggregatedAnalyzer with NodeRanking with EmbeddingRanking with EmbeddingEnumeratorContext =>

  override def graphDocSearch(query: GraphDoc[L], limit: Int): IndexedSeq[(GraphEmbedding, Double)] = {
    val bufferSize = query.size * limit * 2
    val nodeMatchingSet = matchNodes(query, bufferSize)
    rankGraphs(query, composeEmbeddings(nodeMatchingSet).take(bufferSize).toIndexedSeq).take(limit)
  }

}
