/**
  * Implementation of graph ranking based on neighbours
  */
package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.cypher.export.SubGraph
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.graph.{GraphSnippet, SubGraphEnumeratorContext}
import uk.ac.cdrc.mintsearch.index.NeighbourNodeIndexReader
import uk.ac.cdrc.mintsearch.neighbourhood.{NeighbourAwareContext, TraversalStrategy}
import uk.ac.cdrc.mintsearch.neo4j._
import uk.ac.cdrc.mintsearch.search.{GraphSearchQuery, NeighbourAggregatedAnalyzer}

trait NeighbourhoodRanking extends GraphRanking {
  self: NeighbourNodeIndexReader
    with GraphDBContext
    with TraversalStrategy
    with NeighbourAwareContext
    with NeighbourNodeIndexReader
    with NeighbourAggregatedAnalyzer
    with NodeRanking
    with SubGraphEnumeratorContext =>

  override def search(gsq: GraphSearchQuery): Iterator[SubGraph] = {
    val nodeMatching: NodeMatching = for { (n, wls) <- analyze(gsq) } yield n -> (rankNode(wls).toList map { _.getId })
    matchedEmbeddings(nodeMatching) map { _.toNeo4JSubGraph }
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
