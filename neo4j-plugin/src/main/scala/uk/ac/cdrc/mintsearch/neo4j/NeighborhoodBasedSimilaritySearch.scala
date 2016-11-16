package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.graphdb.index.{ Index, IndexManager }

import scala.language.implicitConversions
import scala.math._
import collection.JavaConverters._
import java.util.stream.{ Stream => JStream }
import java.util.function.{ Function => JFunction }

import org.neo4j.procedure.Context
import org.neo4j.graphdb.{ GraphDatabaseService, Node, RelationshipType }
import org.neo4j.graphdb.traversal.{ Evaluators, TraversalDescription, Uniqueness }
import org.neo4j.graphdb.index.IndexManager
import org.neo4j.procedure.Name
import org.neo4j.procedure.PerformsWrites
import org.neo4j.procedure.Procedure

case class SearchHit(nodeId: Long)
/**
 * This class implements the NEighborhood based Similarity Search
 */
class NeighborhoodBasedSimilaritySearch extends Neo4JProcedure {
  implicit private def toJavaFunction[U, V](f: Function1[U, V]): JFunction[U, V] = new JFunction[U, V] {
    override def apply(t: U): V = f(t)
  }

  /**
   *
   * @param label the label name to query by
   * @param query the lucene query, for instance `name:Brook*` to
   *              search by property `name` and find any value starting
   *              with `Brook`. Please refer to the Lucene Query Parser
   *              documentation for full available syntax.
   * @return the nodes found by the query
   */
  @Procedure("mint.ness_search")
  @PerformsWrites // TODO: This is here as a workaround, because index().forNodes() is not read-only
  def search(
    @Name("label") label: String,
    @Name("query") query: String
  ): JStream[SearchHit] = {
    JStream.empty()
  }

  /**
   *
   * @param nodeId the id of the node to index
   * @param relType the relation type used as neighboring
   * @param propName the property name for the labels, the labels should be stored as a space-separated words
   * @param depth the label propagation range, i.e., number of hops a label can propagate though neighbor nodes
   * @param alpha the alpha (propagating damper) and 0 < alpha < 1, i.e., labels are propagated to the neighbor with
   *              a less weight
   */
  @Procedure("mint.ness_index")
  @PerformsWrites
  def index(
    @Name("nodeId") nodeId: Long,
    @Name("relType") relType: String,
    @Name("propName") propName: String,
    @Name("depth") depth: Long,
    @Name("alpha") alpha: Double
  ): Unit = {
    // TODO Need to add alpha for the propagation of the labels
    val node = db.getNodeById(nodeId)
    val td = db.traversalDescription()
      .relationships(RelationshipType.withName(relType))
      .uniqueness(Uniqueness.NODE_GLOBAL)
      .evaluator(Evaluators.atDepth(depth.toInt))
    val index = db.index().forNodes(NeighborhoodBasedSimilaritySearch.indexName(propName), NeighborhoodBasedSimilaritySearch.FULL_TEXT.asJava)
    val labelWeights = propagatedLabels(node, propName, td, alpha)
    index.remove(node) // Make sure the node will be replaced in the index
    index.add(node, propName, labelWeights.keys mkString (" "))
    node.setProperty(NeighborhoodBasedSimilaritySearch.LABEL_PROPERTY_NAME, labelWeights)
  }

  def propagatedLabels(node: Node, propName: String, td: TraversalDescription, alpha: Double): Map[String, Double] = {
    val label_weight_parts = for (
      path <- td.traverse(node).iterator().asScala;
      weight = pow(alpha, path.length());
      label <- path.endNode().getProperty(propName).toString.split(" ")
    ) yield (label, weight)
    label_weight_parts.toList.groupBy(_._1).mapValues(x => x map (_._2) sum)
  }

}

object NeighborhoodBasedSimilaritySearch {
  val FULL_TEXT = Map(IndexManager.PROVIDER -> "lucene", "type" -> "fulltext")
  def indexName(propName: String) = s"index-prop-$propName"
  val LABEL_PROPERTY_NAME = "_labels"
}
