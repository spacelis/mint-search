/**
  * Implementation of graph ranking based on neighbours
  */
package uk.ac.cdrc.mintsearch.search

import uk.ac.cdrc.mintsearch.graph._
import uk.ac.cdrc.mintsearch.index.{BaseIndexReader, LabelMaker}
import uk.ac.cdrc.mintsearch.neo4j._
import uk.ac.cdrc.mintsearch.ranking.{GraphRanking, NodeRanking}
import uk.ac.cdrc.mintsearch.{GraphDoc, NodeMatchingSet}

case class GraphSearchResult(gsq: GraphQuery, graphSnippets: IndexedSeq[GraphEmbedding], scores: IndexedSeq[Double])

trait GraphSearcher {
  self: QueryAnalyzer with LabelMaker with EmbeddingEnumeratorContext =>
  def search(gsq: GraphQuery): GraphSearchResult
}

trait NeighbourBasedSearcher extends GraphSearcher {
  self: BaseIndexReader with GraphDBContext with LabelMaker with TraversalStrategy with NeighbourAwareContext with NeighbourAggregatedAnalyzer with NodeRanking with GraphRanking with EmbeddingEnumeratorContext =>

  override def search(gsq: GraphQuery): GraphSearchResult = {
    val analyzedQuery = analyze(gsq)
    val (graphSnippets, scores) = graphDocSearch(analyzedQuery).unzip
    GraphSearchResult(gsq, graphSnippets, scores)
  }

  def graphDocSearch(query: GraphDoc[L]): IndexedSeq[(GraphEmbedding, Double)] = {

    val resultSets = for {
      (n, wls) <- query.toIndexedSeq
    } yield searchNodes(n, wls)

    val nodeMatchingSet = NodeMatchingSet((for {
      rs <- resultSets
    } yield rs.queryNode -> (rs.ranked map (_.getId))).toMap)

    rankGraphs(query, resultSets, composeEmbeddings(nodeMatchingSet).toIndexedSeq)
  }

}

