/**
 * Implementation of graph ranking based on neighbours
 */
package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.cypher.export.SubGraph
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.graph.{GraphSnippet, SubGraphEnumeratorContext}
import uk.ac.cdrc.mintsearch.index.NodeIndexReader
import uk.ac.cdrc.mintsearch.neighbourhood.{NeighbourAwareContext, TraversalStrategy}
import uk.ac.cdrc.mintsearch.neo4j._
import uk.ac.cdrc.mintsearch.search.{GraphSearchQuery, NeighbourAggregatedAnalyzer}

trait NeighbourhoodRanking extends GraphRanking {
  self: NodeIndexReader with GraphDBContext with TraversalStrategy with NeighbourAwareContext with NeighbourAggregatedAnalyzer with NodeRanking with SubGraphEnumeratorContext =>

  override def search(gsq: GraphSearchQuery): Iterator[SubGraph] = {
    for { em <- rankEmbeddings(gsq) } yield em.toNeo4JSubGraph
  }

  def rankEmbeddings(gsq: GraphSearchQuery): Iterator[GraphSnippet] = {
    val nodeMatching = for {
      (n, wls) <- analyze(gsq)
    } yield n -> (rankNode(wls).toList map { _.getId })
    matchedEmbeddings(nodeMatching)
  }

  /**
   * Return `CypherResultSubGraph`s from
   * @param nodeMatching the matching nodes (query nodes -> matched nodes)
   * @return an series sub graphs assembled from the node pool
   */
  def matchedEmbeddings(nodeMatching: NodeMatching): Iterator[GraphSnippet] = for {
    sgs <- iterateEmbedding(nodeMatching)
  } yield sgs
}

object NeighbourhoodRanking {

}
