/**
 * Implementation of graph ranking based on neighbours
 */
package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.cypher.export.SubGraph
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.graph.{GraphSnippet, SubGraphEnumeratorContext}
import uk.ac.cdrc.mintsearch.index.BaseIndexReader
import uk.ac.cdrc.mintsearch.neighbourhood.{NeighbourAwareContext, TraversalStrategy}
import uk.ac.cdrc.mintsearch.neo4j._
import uk.ac.cdrc.mintsearch.search.{GraphSearchQuery, NeighbourAggregatedAnalyzer}

trait NeighbourhoodSearcher extends GraphSearcher {
  self: BaseIndexReader
    with GraphDBContext
    with TraversalStrategy
    with NeighbourAwareContext
    with NeighbourAggregatedAnalyzer
    with NodeRanking
    with GraphRanking
    with SubGraphEnumeratorContext =>

  override def search(gsq: GraphSearchQuery): Iterator[SubGraph] = {
    (for { em <- rankEmbeddings(gsq).graphSnippets } yield em.toNeo4JSubGraph).toIterator
  }

  def rankEmbeddings(gsq: GraphSearchQuery): GraphSearchResult = {
    val nodeMatching = for {
      (n, wls) <- analyze(gsq)
    } yield n -> (rankNode(wls).toList map { _.getId })
    val (graphSnippets, scores) = rankGraphs(nodeMatching, iterateEmbedding(nodeMatching).toIndexedSeq).unzip
    GraphSearchResult(gsq, graphSnippets, scores)
  }

}

case class GraphSearchResult(gsq: GraphSearchQuery, graphSnippets: IndexedSeq[GraphSnippet], scores: IndexedSeq[Double])

object NeighbourhoodSearcher {

}
