package uk.ac.cdrc.mintsearch.neighbourhood

import org.neo4j.graphdb.Path
import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.index.LabelMaker

/**
 * Created by ucfawli on 08-Jan-17.
 */
trait PropagationStrategy {
  self: LabelMaker =>
  def propagate(p: Path): WeightedLabelSet[L]
}

trait ExponentialPropagation extends PropagationStrategy {
  self: LabelMaker =>

  val propagationFactor: Double

  override def propagate(p: Path): WeightedLabelSet[L] = {
    val weight = Math.pow(propagationFactor, p.length())
    collectLabels(p.endNode()) map { _ -> weight } toMap
  }

  override def toString: String = s"expP$propagationFactor"
}
