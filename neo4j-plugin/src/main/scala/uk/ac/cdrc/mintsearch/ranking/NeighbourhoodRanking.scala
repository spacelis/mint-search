package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.graphdb.{Node, Path}
import uk.ac.cdrc.mintsearch.neighbourhood.NeighbourAwareNode
import uk.ac.cdrc.mintsearch.neo4j.{GraphSnippet, SubGraphEnumerator}
import uk.ac.cdrc.mintsearch._

import scala.collection.JavaConverters._

/**
 * Created by ucfawli on 11/18/16.
 *
 */

trait NeighbourhoodRanking extends GraphRanking {

  def traverDescription: TraversalDescription

  def propagate: Path => WeightedLabelSet

  def measureSimilarity(weightedLabelSet: WeightedLabelSet)

  implicit val nodeWrapper: (Node) => NeighbourAwareNode = NeighbourAwareNode.mkNodeWrapper(traverDescription)


  def rankByNode(node: WeightedLabelSet): Iterator[WeightedLabelSet]


  override def search(gsq: GraphSearchQuery) = {
    val gsqWLS = mkGraphDoc(gsq.qdb.getAllNodes.asScala.toSet)

  }

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
