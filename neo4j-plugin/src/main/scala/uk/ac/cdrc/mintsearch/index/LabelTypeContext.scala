package uk.ac.cdrc.mintsearch.index

import uk.ac.cdrc.mintsearch.WeightedLabelSet

import scala.pickling._
import json._
import scala.pickling.Defaults.{arrayPickler, doubleArrayPickler, intPickler, pickleOps, refPickler, refUnpickler, stringPickler}
import scala.pickling.static._

// Do not use IntelliJ's Optimizing Imports. In case you did, recover from the following
//import scala.pickling.Defaults.{arrayPickler, doubleArrayPickler, intPickler, pickleOps, refPickler, refUnpickler, stringPickler}
//import scala.pickling.static._

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

  case class KVLabelStore(ks: Array[String], vs: Array[String], ws: Array[Double])
  implicit val KVLabelStorePickler: Pickler[KVLabelStore] = Pickler.generate[KVLabelStore]
  implicit val KVLabelStoreUnpickler: Unpickler[KVLabelStore] = Unpickler.generate[KVLabelStore]

  override def JSONfy(wls: WeightedLabelSet[(String, String)]): String = {
    val (kvs, ws) = wls.toArray.unzip[(String, String), Double]
    val (ks, vs) = kvs.unzip[String, String]
    val toStore: KVLabelStore = KVLabelStore(ks, vs, ws)
    toStore.pickle.value
  }
  override def deJSONfy(jsonObj: String): WeightedLabelSet[(String, String)] ={
    val kvls = jsonObj.unpickle[KVLabelStore]
    (kvls.ks zip kvls.vs zip kvls.ws).toMap
  }
}
