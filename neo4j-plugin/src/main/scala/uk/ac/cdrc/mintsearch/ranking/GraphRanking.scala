package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.NodeMatchingSet
import uk.ac.cdrc.mintsearch.graph.GraphSnippet

/**
  * Created by ucfawli on 2/2/17.
  */
trait GraphRanking {
  def rankGraphs(nodeMatchingSet: NodeMatchingSet, graphSnippetList: IndexedSeq[GraphSnippet]): IndexedSeq[(GraphSnippet, Double)]
}

trait SimpleGraphRanking extends GraphRanking {
  override def rankGraphs(nodeMatchingSet: NodeMatchingSet, graphSnippetList: IndexedSeq[GraphSnippet]) = (for {
    g <- graphSnippetList
  } yield g -> 0d).sortBy(_._2)
}

