package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.graphdb.Node
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.index.NeighbourAggregatedIndexReader
import uk.ac.cdrc.mintsearch.neighbourhood.{NeighbourAwareContext, TraversalStrategy}
import uk.ac.cdrc.mintsearch.neo4j._
import uk.ac.cdrc.mintsearch.search.{GraphSearchQuery, NeighbourAggregatedAnalyzer}

/**
 * Created by ucfawli on 11/18/16.
 *
 */

trait NeighbourhoodRanking extends GraphRanking{
  self: NeighbourAggregatedIndexReader
    with GraphContext
    with TraversalStrategy
    with NeighbourAwareContext
    with NeighbourAggregatedIndexReader
    with NeighbourAggregatedAnalyzer
    with SubGraphEnumeratorContext =>


  override def search(gsq: GraphSearchQuery): Iterator[GraphSnippet] = ???


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
