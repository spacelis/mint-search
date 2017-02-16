package uk.ac.cdrc.mintsearch.index

import co.blocke.scalajack.ScalaJack
import uk.ac.cdrc.mintsearch.WeightedLabelSet

import scala.pickling._
import json._
import scala.pickling.Defaults.{arrayPickler, doubleArrayPickler, intPickler, pickleOps, refPickler, refUnpickler, stringPickler}
import scala.pickling.static._

// Do not use IntelliJ's Optimizing Imports. In case you did, recover from the following
//import scala.pickling.Defaults.{arrayPickler, doubleArrayPickler, intPickler, pickleOps, refPickler, refUnpickler, stringPickler}
//import scala.pickling.static._
/**
  * Created by ucfawli on 2/16/17.
  */
trait ScalaPicklingJsonfier {
  self: KeyValueLabelTypeStub =>

  case class KVLabelStore(ks: Array[String], vs: Array[String], ws: Array[Double])

  def toKVLabelStore(wls: WeightedLabelSet[L]): KVLabelStore = {
    val (kvs, ws) = wls.toArray.unzip[(String, String), Double]
    val (ks, vs) = kvs.unzip[String, String]
    KVLabelStore(ks, vs, ws)
  }

  def fromKVLabelStore(kvls: KVLabelStore): WeightedLabelSet[L] = {
    (kvls.ks zip kvls.vs zip kvls.ws).toMap
  }

  implicit val KVLabelStorePickler: Pickler[KVLabelStore] = Pickler.generate[KVLabelStore]
  implicit val KVLabelStoreUnpickler: Unpickler[KVLabelStore] = Unpickler.generate[KVLabelStore]

  override def JSONfy(wls: WeightedLabelSet[L]): String = {
    toKVLabelStore(wls).pickle.value
  }
  override def deJSONfy(jsonObj: String): WeightedLabelSet[L] ={
    fromKVLabelStore(jsonObj.unpickle[KVLabelStore])
  }
}

case class KVLabelStoreSJ(ks: Seq[String], vs: Seq[String], ws: Seq[Double])

trait ScalaJackJsonfier {
  self: KeyValueLabelTypeStub =>

  def toKVLabelStore(wls: WeightedLabelSet[L]): KVLabelStoreSJ = {
    val (kvs, ws) = wls.toSeq.unzip[(String, String), Double]
    val (ks, vs) = kvs.unzip[String, String]
    KVLabelStoreSJ(ks, vs, ws)
  }

  def fromKVLabelStore(kvls: KVLabelStoreSJ): WeightedLabelSet[L] = {
    (kvls.ks zip kvls.vs zip kvls.ws).toMap
  }

  val sj = ScalaJack()

  override def JSONfy(wls: WeightedLabelSet[L]): String = {
    sj.render(toKVLabelStore(wls))
  }
  override def deJSONfy(jsonObj: String): WeightedLabelSet[L] ={
    fromKVLabelStore(sj.read[KVLabelStoreSJ](jsonObj))
  }
}


