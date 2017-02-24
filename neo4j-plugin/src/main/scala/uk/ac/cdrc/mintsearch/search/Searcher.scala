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

  override def search(gsq: GraphQuery, limit: Int = 3): GraphSearchResult = {
    val analyzedQuery = analyze(gsq)
    val (graphSnippets, scores) = graphDocSearch(analyzedQuery, limit).unzip
    GraphSearchResult(gsq, graphSnippets, scores)
  }

  private val logger = LoggerFactory.getLogger(classOf[NeighbourBasedSearcher])

  def graphDocSearch(query: GraphDoc[L], limit: Int): IndexedSeq[(GraphEmbedding, Double)] = {

    val resultSets = for {
      (n, wls) <- query.toIndexedSeq
    } yield searchNodes(n, wls)

    val nodeMatchingSet = NodeMatchingSet((for {
      rs <- resultSets
    } yield rs.queryNode -> (rs.ranked zip rs.scores map (m => m._1.getId -> m._2))).toMap)

    val res = rankGraphs(query, resultSets, composeEmbeddings(nodeMatchingSet).take(limit).toIndexedSeq)

    val render = NodeOnlyAsciiRender(Seq("value"))
    logger.debug(s"graphs=\n${res map {case(g, s) => s"$s <= ${render.toAscii(g)}" } mkString "\n"}")
    res
  }

}

