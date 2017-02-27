/**
  * Defines query analyzers
  */
package uk.ac.cdrc.mintsearch.search

import org.neo4j.graphdb.{Node, Relationship}
import uk.ac.cdrc.mintsearch.GraphDoc
import uk.ac.cdrc.mintsearch.graph.NeighbourAwareContext
import uk.ac.cdrc.mintsearch.index.NodeMarker
import uk.ac.cdrc.mintsearch.neo4j.WithResource

import scala.collection.JavaConverters._

trait QueryAnalyzer {
  self: NodeMarker =>
  def analyze(q: GraphQuery): GraphDoc[L]
}

trait NeighbourAggregatedAnalyzer extends QueryAnalyzer {
  self: NeighbourAwareContext with NodeMarker =>
  override def analyze(q: GraphQuery): GraphDoc[L] = WithResource(q.qdb.beginTx()) { _ =>
    mkGraphDoc(q.qdb.getAllNodes.asScala, q.qdb.getAllRelationships.asScala)
  }
  def mkGraphDoc(nodeSet: Iterable[Node], relationships: Iterable[Relationship]): GraphDoc[L] = {
    val weights = (for { n <- nodeSet } yield n.getId -> n.collectNeighbourhoodLabels).toMap
    val adjacency = (for {
      rel <- relationships
    } yield rel.getStartNode.getId -> rel.getEndNode.getId).groupBy(_._1).mapValues(_.toSeq.map(_._2))
    GraphDoc(weights, adjacency)
  }
}
