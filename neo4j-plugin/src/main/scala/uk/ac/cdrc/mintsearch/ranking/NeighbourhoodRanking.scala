package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.graphdb.{Node, Path}
import uk.ac.cdrc.mintsearch.neighbourhood.{NeighbourAware, NeighbourAwareNode}
import uk.ac.cdrc.mintsearch.neo4j.{GraphSnippet, Neo4JContainer, SubGraphEnumerator}
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.index.NeighbourAggregatedIndexManager

import scala.collection.JavaConverters._

/**
 * Created by ucfawli on 11/18/16.
 *
 */

trait NeighbourhoodRanking extends GraphRanking{
  self: NeighbourAggregatedIndexManager with Neo4JContainer with NeighbourAware =>

  def traverDescription: TraversalDescription

  def propagate: Path => WeightedLabelSet

  def measureSimilarity(weightedLabelSet: WeightedLabelSet)

  def rankByNode(node: WeightedLabelSet): Iterator[WeightedLabelSet]


  override def search(gsq: GraphSearchQuery) = ???

  def mkGraphDoc(nodeSet: Set[Node]): GraphDoc =
    (for { n <- nodeSet } yield n.getId -> n.collectNeighbourLabels).toMap

  /**
   * Return `CypherResultSubGraph`s from
   * @param nodeMatching the matching nodes (query nodes -> matched nodes)
   * @return an series sub graphs assembled from the node pool
   */
  def matchedEmbeddings(nodeMatching: NodeMatching): Iterator[GraphSnippet] = for {
    sgs <- SubGraphEnumerator(traverDescription, db).iterateEmbedding(nodeMatching)
  } yield sgs
}

object NeighbourhoodRanking {

}
