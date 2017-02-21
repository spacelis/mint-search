/**
  * Defines node ranking methods
  */
package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.graphdb.Node
import uk.ac.cdrc.mintsearch.index.{BaseIndexReader, LabelTypeContext}
import uk.ac.cdrc.mintsearch.{NodeId, WeightedLabelSet}

trait NodeSearchResultContext {
  self: LabelTypeContext =>
  case class NodeSearchResult(
    queryNode: NodeId,
    wls: WeightedLabelSet[L],
    ranked: IndexedSeq[Node],
    scores: IndexedSeq[Double]
  )
}

trait NodeRanking extends NodeSearchResultContext {
  self: LabelTypeContext =>
  def searchNodes(queryNode: NodeId, query: WeightedLabelSet[L]): NodeSearchResult
}

trait SimpleNodeRanking extends NodeRanking {
  self: BaseIndexReader with NodeSimilarity with LabelTypeContext =>
  override def searchNodes(queryNode: NodeId, query: WeightedLabelSet[L]): NodeSearchResult = {
    val nodes = getNodesByLabels(query.keySet)
    val nodesWithScore = for {
      n <- nodes
      s = similarity(retrieveWeightedLabels(n), query)
    } yield n -> s
    val (rankedNodes, rankScore) = nodesWithScore.filter(_._2 > selfLabelWeight).sortBy(_._2).unzip
    NodeSearchResult(queryNode, query, rankedNodes, rankScore)
  }

}

