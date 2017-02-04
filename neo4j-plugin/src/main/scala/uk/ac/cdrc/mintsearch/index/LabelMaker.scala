/**
 * Label handling
 */
package uk.ac.cdrc.mintsearch.index

import org.neo4j.graphdb.Node
import uk.ac.cdrc.mintsearch.WeightedLabelSet

import scala.collection.JavaConverters._
import scala.pickling._
import Defaults._
import json._

trait LabelMaker {
  type L
  val labelStorePropKey: String
  def collectLabels(n: Node): Seq[L]
  def labelEncode(label: L): String
  def labelEncodeQuery(label: L): String
  def JSONfy(wls: WeightedLabelSet[L]): String
  def deJSONfy(json: String): WeightedLabelSet[L]
}

trait PropertyLabelMaker extends LabelMaker {
  override type L = (String, String)
  override def collectLabels(n: Node): Seq[L] = for {
    pName <- n.getPropertyKeys.asScala.toSeq
    if pName != labelStorePropKey
  } yield (pName, n.getProperty(pName).toString)

  override def labelEncode(label: L): String = s"${label._1}:${label._2}"
  override def labelEncodeQuery(label: L): String = s"${label._1}\\:${label._2}"

  def JSONfy(wls: WeightedLabelSet[(String, String)]): String =
    wls.pickle.value
  def deJSONfy(json: String): WeightedLabelSet[(String, String)] =
    JSONPickle(json).unpickle[WeightedLabelSet[(String, String)]]
}

