/**
  * Label handling
  */
package uk.ac.cdrc.mintsearch.index

import org.neo4j.graphdb.Node

import scala.collection.JavaConverters._

trait NodeMarker extends NodeDefContext {
  val labelStorePropKey: String
  def collectLabels(n: Node): Seq[L]
}

trait PropertyNodeMarker extends NodeMarker with KeyValueNode {
  override def collectLabels(n: Node): Seq[L] = for {
    pName <- n.getPropertyKeys.asScala.toSeq
    if pName != labelStorePropKey
  } yield pName -> n.getProperty(pName).toString

}

trait PropertyLabelNodeMarker extends NodeMarker with KeyValueNode {
  override def collectLabels(n: Node): Seq[L] = (for {
    pName <- n.getPropertyKeys.asScala.toSeq
    if pName != labelStorePropKey
  } yield pName -> n.getProperty(pName).toString) ++
    (n.getLabels.asScala map {"LABEL" -> _.name()})

}
