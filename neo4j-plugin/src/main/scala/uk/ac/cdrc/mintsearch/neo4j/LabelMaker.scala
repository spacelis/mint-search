package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.graphdb.Node

import scala.collection.JavaConverters._

/**
  * Created by ucfawli on 08-Jan-17.
  */
trait LabelMaker {
  val labelPropKey: String
  def collectLabels(n: Node): Seq[String]
}

trait PropertyLabelMaker extends LabelMaker{
  override def collectLabels(n: Node) = n.getPropertyKeys.asScala.toSeq
    .filter (_ != labelPropKey)
    .map { pName => s"$pName:${n.getProperties(pName).toString}"}
}
