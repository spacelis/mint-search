package uk.ac.cdrc.mintsearch.neighbourhood

import org.neo4j.graphdb.Path
import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.neo4j.LabelMaker

/**
  * Created by ucfawli on 08-Jan-17.
  */
trait Propagation extends LabelMaker{
    def propagate(p: Path): WeightedLabelSet
}

trait ExponentialPropagation extends Propagation {

  val propagationFactor: Double

  override def propagate(p: Path) = {
    val weight = Math.pow(propagationFactor, p.length())
    collectLabels(p.endNode()) map {_ -> weight} toMap
  }

  override def toString: String = s"expP$propagationFactor"
}

trait HasPropagation {
  val propagation: Propagation
}

