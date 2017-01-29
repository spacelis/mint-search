/**
 * Label handling
 */
package uk.ac.cdrc.mintsearch.index

import org.neo4j.graphdb.Node

import scala.collection.JavaConverters._

trait LabelMaker {
  type L
  val labelStorePropKey: String
  def collectLabels(n: Node): Seq[L]
  def labelEncode(label: L): String
  def labelEncodeQuery(label: L): String
}

trait PropertyLabelMaker extends LabelMaker {
  override type L = (String, String)
  override def collectLabels(n: Node): Seq[L] = for {
    pName <- n.getPropertyKeys.asScala.toSeq
    if pName != labelStorePropKey
  } yield (pName, n.getProperty(pName).toString)

  override def labelEncode(label: L): String = s"${label._1}:${label._2}"
  override def labelEncodeQuery(label: L): String = s"${label._1}\\:${label._2}"
}

