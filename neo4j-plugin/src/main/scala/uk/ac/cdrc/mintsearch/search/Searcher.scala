/**
 * Implementation of graph ranking based on neighbours
 */
package uk.ac.cdrc.mintsearch.search

import uk.ac.cdrc.mintsearch.GraphDoc
import uk.ac.cdrc.mintsearch.graph.{NeighbourAwareContext, SubGraphEnumeratorContext, TraversalStrategy}
import uk.ac.cdrc.mintsearch.index.{BaseIndexReader, LabelMaker}
import uk.ac.cdrc.mintsearch.neo4j._
import uk.ac.cdrc.mintsearch.ranking.{GraphRanking, NodeRanking}

trait GraphSearcher {
  self: QueryAnalyzer with LabelMaker with  SubGraphEnumeratorContext =>
  def search(gsq: GraphQuery): GraphSearchResult
  case class GraphSearchResult(gsq: GraphQuery, graphSnippets: IndexedSeq[GraphSnippet], scores: IndexedSeq[Double])
}

trait NeighbourBasedSearcher extends GraphSearcher {
  self: BaseIndexReader
    with GraphDBContext
    with LabelMaker
    with TraversalStrategy
    with NeighbourAwareContext
    with NeighbourAggregatedAnalyzer
    with NodeRanking
    with GraphRanking
    with SubGraphEnumeratorContext =>

  override def search(gsq: GraphQuery): GraphSearchResult = {
    val analyzedQuery = analyze(gsq)
    val (graphSnippets, scores) = graphDocSearch(analyzedQuery).unzip
    GraphSearchResult(gsq, graphSnippets, scores)
  }

  def graphDocSearch(query: GraphDoc[L]): IndexedSeq[(GraphSnippet, Double)] = {
    val nodeMatchingSet = for {
      (n, wls) <- query.toIndexedSeq
    } yield searchNodes(n, wls)
    rankGraphs(query, nodeMatchingSet, composeGraphs(nodeMatchingSet.flatMap(_.ranked).map(_.getId).toSet).toIndexedSeq)
  }

}

