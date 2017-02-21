/**
  * Defines how node labels should be propagate to neighbours
  */
package uk.ac.cdrc.mintsearch.graph

import org.neo4j.graphdb.Path
import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.index.LabelMaker

trait PropagationStrategy {
  self: LabelMaker =>
  def propagate(p: Path): WeightedLabelSet[L]
  def propagate(p: Path, w: Double): WeightedLabelSet[L] = {
    collectLabels(p.endNode()) map { _ -> w } toMap
  }
}

trait ExponentialPropagation extends PropagationStrategy {
  self: LabelMaker =>

  val propagationFactor: Double

  override def propagate(p: Path): WeightedLabelSet[L] = {
    val l = p.length()
    val weight = if (l == 0) selfLabelWeight else Math.pow(propagationFactor, l)
    propagate(p, weight)
  }

  override def toString: String = s"expP$propagationFactor"
}
