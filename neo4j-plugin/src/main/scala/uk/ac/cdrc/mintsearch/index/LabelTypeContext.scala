package uk.ac.cdrc.mintsearch.index

import uk.ac.cdrc.mintsearch.WeightedLabelSet


/**
  * Describe the label structure
  */
trait LabelTypeContext {
  type L
  val selfLabelWeight: Double = 100d // for distinguish labels from neighbours,
                                     // as those labels will have far less weights.
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
  private val WS = "\\s".r
  override def labelEncode(label: L): String = s"${label._1}:${WS replaceAllIn(label._2, "_")}"
  override def labelEncodeQuery(label: L): String = s"${label._1}\\:${WS replaceAllIn(label._2, "_")}"
}

trait KeyValueLabelType extends KeyValueLabelTypeStub with ScalaJackJsonfier
