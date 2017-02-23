package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.graph.GraphEmbedding
import uk.ac.cdrc.mintsearch.index.LabelTypeContext
import uk.ac.cdrc.mintsearch.{GraphDoc, NodeId}

/**
  * A component of ranking
  */
trait GraphRanking {
  self: NodeSearchResultContext with LabelTypeContext =>
  def rankGraphs(
    query: GraphDoc[L],
    nodeMatchingSet: IndexedSeq[NodeSearchResult],
    graphSnippetList: IndexedSeq[GraphEmbedding]
  ): IndexedSeq[(GraphEmbedding, Double)]
}

trait SimpleGraphRanking extends GraphRanking {
  self: NodeSearchResultContext with NodeSimilarity with LabelTypeContext =>

  /**
    * Ranking the the retrieved graph by summing matching scores of nodes inside each of them
    *
    * @param nodeMatchingResult Lists of matching nodes composing the graphSnippetList
    * @param graphSnippetList A list of graph to rank
    * @return a list of graph snippets with the scores.
    */
  override def rankGraphs(
    graphDoc: GraphDoc[L],
    nodeMatchingResult: IndexedSeq[NodeSearchResult],
    graphSnippetList: IndexedSeq[GraphEmbedding]
  ): IndexedSeq[(GraphEmbedding, Score)] = {
    val graphs = graphSnippetList.map(_.nodeIds.toSet)
    for {
      (gid, score) <- graphScoring(graphs, scoreMapping(nodeMatchingResult))
    } yield graphSnippetList(gid) -> score
  }

  def scoreMapping(nodeMatchingResult: IndexedSeq[NodeSearchResult]): Map[NodeId, Score] =
    (for {
      nms <- nodeMatchingResult
      (r, s) <- nms.ranked zip nms.scores
    } yield r.getId -> s).toMap.withDefaultValue(0.0d)

  def graphScoring(graphs: IndexedSeq[Set[NodeId]], nodeScores: Map[NodeId, Score]): IndexedSeq[(Int, Score)] = {

    val scoreParts = for {
      (g, gid) <- graphs.zipWithIndex
      n <- g.toSeq
    } yield gid -> nodeScores(n)

    scoreParts.groupBy(_._1)
      .mapValues(s => (s map { _._2 }).sum)
      .toIndexedSeq
      .sortBy(_._2)
  }

}
