/**
 * Defines node ranking methods
 */
package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.graphdb.Node
import uk.ac.cdrc.mintsearch.{NodeId, WeightedLabelSet}
import uk.ac.cdrc.mintsearch.index.{BaseIndexReader, LabelMaker}

trait NodeRanking {
  self: LabelMaker =>
  def searchNodes(queryNode: NodeId, query: WeightedLabelSet[L]): NodeSearchResult
  case class NodeSearchResult(queryNode: NodeId,
                              wls: WeightedLabelSet[L],
                              ranked: IndexedSeq[Node],
                              scores: IndexedSeq[Double]
                             )
}

trait SimpleNodeRanking extends NodeRanking {
  self: BaseIndexReader with NeighbourSimilarity with LabelMaker =>
  override def searchNodes(queryNode: NodeId, query: WeightedLabelSet[L]): NodeSearchResult = {
    val nodes = getNodesByLabels(query.keySet)
    val nodesWithScore = for {
      n <- nodes
      s = measureSimilarity(retrieveWeightedLabels(n), query)
    } yield n -> s
    val (rankedNodes, rankScore) = nodesWithScore.sortBy(_._2).unzip
    NodeSearchResult(queryNode, query, rankedNodes, rankScore)
  }

}

