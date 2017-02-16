package uk.ac.cdrc.mintsearch.index

import uk.ac.cdrc.mintsearch.WeightedLabelSet


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
trait KeyValueLabelTypeStub extends LabelTypeContext {
  override type L = (String, String)
  override def labelEncode(label: L): String = s"${label._1}:${label._2}"
  override def labelEncodeQuery(label: L): String = s"${label._1}\\:${label._2}"
}

trait KeyValueLabelType extends KeyValueLabelTypeStub with ScalaJackJsonfier
