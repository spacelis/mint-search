package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.graphdb.Node

import scala.collection.JavaConverters._

/**
 * Created by ucfawli on 08-Jan-17.
 */
trait LabelMaker {
  val labelStorePropKey: String
  def collectLabels(n: Node): Seq[String]
}

trait PropertyLabelMaker extends LabelMaker {
  override def collectLabels(n: Node): Seq[String] = n.getPropertyKeys.asScala.toSeq
    .filter(_ != labelStorePropKey)
    .map { pName => s"$pName:${n.getProperty(pName).toString}" }
}
