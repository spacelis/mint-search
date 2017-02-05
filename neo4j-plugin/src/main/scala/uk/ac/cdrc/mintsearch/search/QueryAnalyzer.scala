/**
  * Defines query analyzers
  */
package uk.ac.cdrc.mintsearch.search

import org.neo4j.graphdb.Node
import uk.ac.cdrc.mintsearch.GraphDoc
import uk.ac.cdrc.mintsearch.graph.NeighbourAwareContext
import uk.ac.cdrc.mintsearch.index.LabelMaker
import uk.ac.cdrc.mintsearch.neo4j.WithResource

import scala.collection.JavaConverters._

trait QueryAnalyzer {
  self: LabelMaker =>
  def analyze(q: GraphQuery): GraphDoc[L]
}

trait NeighbourAggregatedAnalyzer extends QueryAnalyzer {
  self: NeighbourAwareContext with LabelMaker =>
  override def analyze(q: GraphQuery): GraphDoc[L] = WithResource(q.qdb.beginTx()) { _ =>
    mkGraphDoc(q.qdb.getAllNodes.asScala.toSet)
  }
  def mkGraphDoc(nodeSet: Set[Node]): GraphDoc[L] =
    (for { n <- nodeSet } yield n.getId -> n.collectNeighbourhoodLabels).toMap
}
