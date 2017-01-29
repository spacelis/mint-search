package uk.ac.cdrc.mintsearch.search

import org.neo4j.graphdb.Node
import uk.ac.cdrc.mintsearch.GraphDoc
import uk.ac.cdrc.mintsearch.neighbourhood.NeighbourAwareContext
import uk.ac.cdrc.mintsearch.neo4j.{LabelMaker, WithResource}

import scala.collection.JavaConverters._

/**
 * Created by ucfawli on 15-Jan-17.
 */
trait GraphQueryAnalyzer {
  self: LabelMaker =>
  def analyze(q: GraphSearchQuery): GraphDoc[L]
}

trait NeighbourAggregatedAnalyzer extends GraphQueryAnalyzer {
  self: NeighbourAwareContext with LabelMaker =>
  override def analyze(q: GraphSearchQuery): GraphDoc[L] = WithResource(q.qdb.beginTx()) { session =>
    mkGraphDoc(q.qdb.getAllNodes.asScala.toSet)
  }
  def mkGraphDoc(nodeSet: Set[Node]): GraphDoc[L] =
    (for { n <- nodeSet } yield n.getId -> n.collectNeighbourLabels).toMap
}
