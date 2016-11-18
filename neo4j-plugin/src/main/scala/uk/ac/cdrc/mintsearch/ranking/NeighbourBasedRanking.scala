package uk.ac.cdrc.mintsearch.ranking

import collection.JavaConverters._

import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.graphdb.{Node, Path}
import uk.ac.cdrc.mintsearch.ranking.NeighbourBasedRanking.WeightedLabelSet

/**
  * Created by ucfawli on 11/18/16.
  */
trait NeighbourBasedRanking extends Ranking{

  def propagate(path: Path, propName: String): WeightedLabelSet

  def measureSimilarity(weightedLabelSet: WeightedLabelSet)

  def rank(result: Iterator[WeightedLabelSet], query: WeightedLabelSet) = {

  }

  def collectNeighbourLabels(node: Node, propName: String, td: TraversalDescription) = {

    val label_weight_parts = for (
      path <- td.traverse(node).iterator().asScala;
    ) yield propagate(path, propName)

    // Aggregate the label weights.
    label_weight_parts.toList.flatMap(_.toSeq).groupBy(_._1).mapValues(x => x.map(_._2).sum)
  }
}

object NeighbourBasedRanking {
  type WeightedLabelSet = Map[String, Double]

}
