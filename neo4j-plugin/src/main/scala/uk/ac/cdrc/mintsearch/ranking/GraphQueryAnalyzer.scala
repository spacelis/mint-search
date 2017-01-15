package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.graphdb.Node
import uk.ac.cdrc.mintsearch.GraphDoc
import uk.ac.cdrc.mintsearch.neighbourhood.NeighbourAwareContext
import uk.ac.cdrc.mintsearch.neo4j.WithResource

import scala.collection.JavaConverters._

/**
  * Created by ucfawli on 15-Jan-17.
  */
trait GraphQueryAnalyzer {
  self: NeighbourAwareContext =>
  def analyze(q: GraphSearchQuery): GraphDoc = WithResource (q.qdb.beginTx()) {session =>
    mkGraphDoc(q.qdb.getAllNodes.asScala.toSet)
  }
  def mkGraphDoc(nodeSet: Set[Node]): GraphDoc =
    (for { n <- nodeSet } yield n.getId -> n.collectNeighbourLabels).toMap
}
