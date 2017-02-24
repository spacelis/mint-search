/**
  * Implementation of graph ranking based on neighbours
  */
package uk.ac.cdrc.mintsearch.search

import org.slf4j.LoggerFactory
import uk.ac.cdrc.mintsearch.graph._
import uk.ac.cdrc.mintsearch.index.{BaseIndexReader, LabelMaker}
import uk.ac.cdrc.mintsearch.neo4j._
import uk.ac.cdrc.mintsearch.ranking.{GraphRanking, NodeRanking}
import uk.ac.cdrc.mintsearch.{GraphDoc, NodeMatchingSet}

case class GraphSearchResult(gsq: GraphQuery, graphSnippets: IndexedSeq[GraphEmbedding], scores: IndexedSeq[Double])

trait GraphSearcher {
  self: QueryAnalyzer with LabelMaker with EmbeddingEnumeratorContext =>
  def search(gsq: GraphQuery, limit: Int = 3): GraphSearchResult
}

trait NeighbourBasedSearcher extends GraphSearcher {
  self: BaseIndexReader with GraphDBContext with LabelMaker with TraversalStrategy with NeighbourAwareContext with NeighbourAggregatedAnalyzer with NodeRanking with GraphRanking with EmbeddingEnumeratorContext =>

  private val logger = LoggerFactory.getLogger(classOf[NeighbourBasedSearcher])

  override def search(gsq: GraphQuery, limit: Int = 3): GraphSearchResult = {
    val analyzedQuery = analyze(gsq)
    logger.info(
      s"""query=
         |${analyzedQuery map {case (k, v) => s"$k => $v"} mkString "\n"}
         |===========""".stripMargin)
    val (graphSnippets, scores) = graphDocSearch(analyzedQuery, limit).unzip
    GraphSearchResult(gsq, graphSnippets, scores)
  }


  def graphDocSearch(query: GraphDoc[L], limit: Int): IndexedSeq[(GraphEmbedding, Double)] = {

    val resultSets = for {
      (n, wls) <- query.toIndexedSeq
    } yield searchNodes(n, wls)

    val nodeMatchingSet = NodeMatchingSet((for {
      rs <- resultSets
    } yield rs.queryNode -> (rs.ranked zip rs.scores map (m => m._1.getId -> m._2))).toMap)
    logger.info(
      s"""nodeRankLists=
         |${nodeMatchingSet.map map {case(k, v) => s"$k => $v"} mkString "\n"}
         |=============""".stripMargin)
    rankGraphs(query, resultSets, composeEmbeddings(nodeMatchingSet).take(limit).toIndexedSeq)
  }

}

