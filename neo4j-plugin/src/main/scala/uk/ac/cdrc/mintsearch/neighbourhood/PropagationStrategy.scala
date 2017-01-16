package uk.ac.cdrc.mintsearch.neighbourhood

import org.neo4j.graphdb.Path
import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.neo4j.LabelMaker

/**
  * Created by ucfawli on 08-Jan-17.
  */
trait PropagationStrategy {
    def propagate(p: Path): WeightedLabelSet
}

trait ExponentialPropagation extends PropagationStrategy {
  self: LabelMaker =>

  val propagationFactor: Double

  override def propagate(p: Path): WeightedLabelSet = {
    val weight = Math.pow(propagationFactor, p.length())
    collectLabels(p.endNode()) map {_ -> weight} toMap
  }

  override def toString: String = s"expP$propagationFactor"
}