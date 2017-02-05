/**
  * Label handling
  */
package uk.ac.cdrc.mintsearch.index

import org.neo4j.graphdb.Node

import scala.collection.JavaConverters._

trait LabelMaker extends LabelTypeContext {
  val labelStorePropKey: String
  def collectLabels(n: Node): Seq[L]
}

trait PropertyLabelMaker extends LabelMaker with KeyValueLabelType {
  override def collectLabels(n: Node): Seq[L] = for {
    pName <- n.getPropertyKeys.asScala.toSeq
    if pName != labelStorePropKey
  } yield (pName, n.getProperty(pName).toString)

}

