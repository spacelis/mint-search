package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.{GraphDoc, NodeId}
import uk.ac.cdrc.mintsearch.graph.GraphSnippet
import uk.ac.cdrc.mintsearch.index.{LabelMaker, LabelTypeContext}

/**
  * A component of ranking
  */
trait GraphRanking {
  self: NodeSearchResultContext with LabelTypeContext =>
  def rankGraphs(
    query: GraphDoc[L],
    nodeMatchingSet: IndexedSeq[NodeSearchResult],
    graphSnippetList: IndexedSeq[GraphSnippet]
  ): IndexedSeq[(GraphSnippet, Double)]
}

trait SimpleGraphRanking extends GraphRanking {
  self: NodeSearchResultContext with LabelTypeContext =>

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
    graphSnippetList: IndexedSeq[GraphSnippet]
  ): IndexedSeq[(GraphSnippet, Double)] = {
    val graphs = graphSnippetList.map(_.nodeIds.toSet)
    for {
      (gid, score) <- graphScoring(graphs, scoreMapping(nodeMatchingResult))
    } yield graphSnippetList(gid) -> score
  }

  def scoreMapping(nodeMatchingResult: IndexedSeq[NodeSearchResult]): Map[NodeId, Double] =
    (for {
      nms <- nodeMatchingResult
      (r, s) <- nms.ranked zip nms.scores
    } yield r.getId -> s).toMap

  def graphScoring(graphs: IndexedSeq[Set[NodeId]], nodeScores: Map[NodeId, Double]): IndexedSeq[(Int, Double)] = {

    val scoreParts = for {
      (g, gid) <- graphs.zipWithIndex
      n <- g.toSeq
    } yield gid -> nodeScores(n)

    scoreParts.groupBy(_._1)
      .mapValues(s => (s map { _._2 }).sum)
      .toIndexedSeq
      .sortBy(_._2)
      .reverse
  }

}
