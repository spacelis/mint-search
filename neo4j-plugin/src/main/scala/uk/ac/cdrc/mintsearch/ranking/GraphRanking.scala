package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.GraphDoc
import uk.ac.cdrc.mintsearch.graph.{GraphSnippet, SubGraphEnumeratorContext}
import uk.ac.cdrc.mintsearch.index.LabelMaker

/**
  * A component of ranking
  */
trait GraphRanking {
  self: NodeRanking with LabelMaker =>
  def rankGraphs(query: GraphDoc[L],
                 nodeMatchingSet: IndexedSeq[NodeSearchResult],
                 graphSnippetList: IndexedSeq[GraphSnippet]): IndexedSeq[(GraphSnippet, Double)]
}

trait SimpleGraphRanking extends GraphRanking {
  self: NodeRanking with LabelMaker =>

  /**
    * Ranking the the retrieved graph by summing matching scores of nodes inside each of them
    *
    * @param nodeMatchingSet Lists of matching nodes composing the graphSnippetList
    * @param graphSnippetList A list of graph to rank
    * @return a list of graph snippets with the scores.
    */
  override def rankGraphs(graphDoc: GraphDoc[L],
                          nodeMatchingSet: IndexedSeq[NodeSearchResult],
                          graphSnippetList: IndexedSeq[GraphSnippet]): IndexedSeq[(GraphSnippet, Double)] = {
    val nodeScores = (for {
      nms <- nodeMatchingSet
      (r, s) <- nms.ranked zip nms.scores
    } yield r.getId -> s).toMap

    val graphScores = (for {
      (g, gid) <- graphSnippetList.zipWithIndex
      n <- g.nodeIds
    } yield gid -> nodeScores(n)).groupBy(_._1).mapValues(s => (s map {_._2}).sum)

    for {
      (g, s) <- graphScores.toIndexedSeq.sortBy(_._2).reverse
    } yield (graphSnippetList(g), s)
  }
}
