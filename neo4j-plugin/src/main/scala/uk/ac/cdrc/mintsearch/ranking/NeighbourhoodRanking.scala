package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.graphdb.Node
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.index.NeighbourAggregatedIndexManager
import uk.ac.cdrc.mintsearch.neighbourhood.{NeighbourAwareContext, TraversalStrategy}
import uk.ac.cdrc.mintsearch.neo4j._
import uk.ac.cdrc.mintsearch.search.GraphSearchQuery

/**
 * Created by ucfawli on 11/18/16.
 *
 */

trait NeighbourhoodRanking extends GraphRanking{
  self: NeighbourAggregatedIndexManager with GraphContext with TraversalStrategy with NeighbourAwareContext with SubGraphEnumeratorContext =>

  def measureSimilarity(weightedLabelSet: WeightedLabelSet)

  def rankByNode(node: WeightedLabelSet): Iterator[WeightedLabelSet]

  override def search(gsq: GraphSearchQuery) = ???


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
