package uk.ac.cdrc.mintsearch.index

import uk.ac.cdrc.mintsearch.WeightedLabelSet
import scala.pickling._
import Defaults._
import json._

/**
  * Describe the label structure
  */
trait LabelTypeContext {
  type L
  def labelEncode(label: L): String
  def labelEncodeQuery(label: L): String
  def JSONfy(wls: WeightedLabelSet[L]): String
  def deJSONfy(json: String): WeightedLabelSet[L]
}

/**
  * Labels are pairs of key-value
  */
trait KeyValueLabelType extends LabelTypeContext {
  override type L = (String, String)
  override def labelEncode(label: L): String = s"${label._1}:${label._2}"
  override def labelEncodeQuery(label: L): String = s"${label._1}\\:${label._2}"

  override def JSONfy(wls: WeightedLabelSet[(String, String)]): String =
    wls.pickle.value
  override def deJSONfy(json: String): WeightedLabelSet[(String, String)] =
    JSONPickle(json).unpickle[WeightedLabelSet[(String, String)]]
}
