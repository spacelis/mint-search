package uk.ac.cdrc.mintsearch.ranking

import uk.ac.cdrc.mintsearch.graph.GraphEmbedding
import uk.ac.cdrc.mintsearch.index.LabelTypeContext
import uk.ac.cdrc.mintsearch.{GraphDoc, NodeId}

/**
  * A component of ranking
  */
trait EmbeddingRanking {
  self: NodeSearchResultContext with LabelTypeContext =>
  def rankGraphs(
    query: GraphDoc[L],
    embeddings: IndexedSeq[GraphEmbedding]
  ): IndexedSeq[(GraphEmbedding, Double)]
}

trait SimpleEmbeddingRanking extends EmbeddingRanking {
  self: NodeSearchResultContext with NodeSimilarity with LabelTypeContext =>

  /**
    * Ranking the the retrieved graph by summing matching scores of nodes inside each of them
    *
    * @param query the query graph document
    * @param embeddings A list of graph to rank
    * @return a list of graph snippets with the scores.
    */
  override def rankGraphs(query: GraphDoc[L], embeddings: IndexedSeq[GraphEmbedding]): IndexedSeq[(GraphEmbedding, Score)] = {
    val graphs = embeddings.map(_.projection)
    (for {
      (gid, score) <- graphScoring(graphs)
    } yield embeddings(gid) -> score).sortBy(_._2)(scoreOrd)
  }

  def graphScoring(graphs: IndexedSeq[Map[NodeId, (NodeId, Double)]]): IndexedSeq[(Int, Score)] = {
    for {
      (g, gid) <- graphs.zipWithIndex
    } yield gid -> g.values.map(_._2).sum
  }

}
