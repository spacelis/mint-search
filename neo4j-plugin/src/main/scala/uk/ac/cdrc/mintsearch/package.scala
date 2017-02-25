/**
  * Utility Definitions of types
  */
package uk.ac.cdrc

import java.io.{File, IOException}

import org.apache.commons.io.FileUtils

import scala.math.max

package object mintsearch {

  type NodeId = Long

  /**
    * A mapping from label to a weight value
    */
  type WeightedLabelSet[L] = Map[L, Double]

  /**
    * A mapping from a node (by its ID) to a list of nodes (IDs)
    */
  case class NodeMatchingSet(matching: Map[NodeId, Seq[(NodeId, Double)]]) {
    lazy val inverse: Map[NodeId, (NodeId, Double)] = (for {
      (v, ns) <- matching.toSeq
      (n, s) <- ns
    } yield n -> (v -> s)).toMap
    lazy val candidates: Set[NodeId] = matching.values.flatten.map(_._1).toSet
    def removeQueryNodes(vs: Seq[NodeId]): NodeMatchingSet = NodeMatchingSet(matching.filterKeys(k => ! (vs contains k)))
    def removeCandidates(ns: Set[NodeId]): NodeMatchingSet = NodeMatchingSet(for {
      (k, nodes) <- matching
      left = nodes filter (n => !(ns contains n._1))
      if left.nonEmpty
    } yield k -> left)
    def nonEmpty: Boolean = matching.nonEmpty

    def take(n: Int) = NodeMatchingSet(for {
      (v, ns) <- matching
    } yield v -> ns.take(n))
  }

  /**
    * A mapping from node to its weighted label set
    */
  type GraphDoc[L] = Map[NodeId, WeightedLabelSet[L]]

  /**
    * A class for adding operators to Map[String, Double] aliased to WeightedLabelSet
    * @param inner a value of type Map[String, Double]
    */
  class WeightedLabelSetWrapper[L](val inner: WeightedLabelSet[L]) {
    def ~(other: WeightedLabelSetWrapper[L]): WeightedLabelSet[L] = {
      inner map { case (k, v) => (k, max(0.0, v - other.inner.getOrElse(k, 0.0))) }
    }

    def ~~(other: WeightedLabelSetWrapper[L]): WeightedLabelSet[L] = {
      this.~(other) filter { _._2 > 0.0 }
    }

  }

  /**
    * Sum up a list of weight distributions
    * @param xs a list of WeightedLabelSets
    * @return a WeightedLabelSet in which the labels' weights are summed from xs
    */
  def sum[L](xs: TraversableOnce[WeightedLabelSet[L]]): WeightedLabelSet[L] =
    xs.flatMap(_.toSeq).toSeq.groupBy(_._1).mapValues(_.map(_._2).sum)

  /**
    * Implicit wrapping a value of type WeightedLabelSet (Map[String, Double]) to provide additional operator on them
    * @param wls a value of WeightedLabelSet
    * @return a wrapped Map[String, Double]
    */
  implicit def asWightedLabelSetWrapper[L](wls: WeightedLabelSet[L]): WeightedLabelSetWrapper[L] =
    new WeightedLabelSetWrapper(wls)

  val defaultTempDir = new File(System.getProperty("java.io.tmpdir"))
  class TempDir(prefix: String = "mintsearch-", suffix: String = ".tmp.d", dir: File = defaultTempDir) extends AutoCloseable {
    private val temp = File.createTempFile(prefix, suffix, dir)
    temp.delete()
    if (temp.mkdirs())
      temp
    else
      throw new IOException("Cannot create temp dir")

    def value: File = temp

    override def close(): Unit = {
      FileUtils.deleteDirectory(temp)
    }
  }
  object TempDir {
    def apply() = new TempDir()
  }
}
