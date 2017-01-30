package uk.ac.cdrc.mintsearch.neo4j

import java.util.stream.{Stream => JStream}

import org.neo4j.graphdb.index.IndexManager
import org.neo4j.graphdb.traversal.{Evaluators, TraversalDescription, Uniqueness}
import org.neo4j.graphdb.{Node, RelationshipType}
import org.neo4j.procedure.{Name, PerformsWrites, Procedure}

import scala.collection.JavaConverters._
import scala.compat.java8.StreamConverters._
import scala.math._
import scala.pickling._
import scala.pickling.json._

/**
 * This class implements the NEighborhood based Similarity Search
 */
class ExampleNessSearch extends Neo4JProcedure {
  import ExampleNessSearch._
  /**
   *
   * @param propName the property name for the labels used in the index
   * @param relType the name of the relationtype used in the index the index
   * @param query the lucene query.
   * @return the nodes found by the query
   */
  @Procedure("mint.ness_search")
  @PerformsWrites // TODO: This is here as a workaround, because index().forNodes() is not read-only
  def search(
    @Name("relType") relType: String,
    @Name("propName") propName: String,
    @Name("query") query: String
  ): JStream[SearchHit] = {
    Option(db.index().forNodes(mkIndexName(relType, propName))) match {
      case Some(index) => index.query(query).iterator().asScala.map((n: Node) => new SearchHit(n)).seqStream
      case None => JStream.empty()
    }
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
    // TODO need to figure out how to scoring a graph
    val indexName = mkIndexName(relType, propName)
    val nstorePropName = mkNStoreLabelName(relType, propName)
    val index = db.index().forNodes(indexName, FULL_TEXT.asJava)

    // Prepare the graph traverser for collecting neighbors' labels
    val node = db.getNodeById(nodeId)
    val td = db.traversalDescription()
      .relationships(RelationshipType.withName(relType))
      .uniqueness(Uniqueness.NODE_GLOBAL)
      .evaluator(Evaluators.atDepth(depth.toInt))
    val labelWeights = propagatedLabels(node, propName, td, alpha)

    // Indexing the node and store the neighbors' labels in the node's property
    index.remove(node) // Make sure the node will be replaced in the index
    index.add(node, propName, labelWeights.keys mkString " ")
    node.setProperty(nstorePropName, labelWeights.pickle.value)
  }

  def propagatedLabels(node: Node, propName: String, td: TraversalDescription, alpha: Double): Map[String, Double] = {
    // Visit all neighbors for the labels from the property given by the name
    val label_weight_parts = for (
      path <- td.traverse(node).iterator().asScala;
      weight = pow(alpha, path.length());
      label <- path.endNode().getProperty(propName).toString.split(" ")
    ) yield (label, weight)
    // Aggregate the label weights.
    label_weight_parts.toList.groupBy(_._1).mapValues(x => x.map(_._2).sum)
  }

}

object ExampleNessSearch {
  val FULL_TEXT = Map(IndexManager.PROVIDER -> "lucene", "type" -> "fulltext")
  def mkIndexName(relName: String, propName: String) = s"NESSIndex-$relName-$propName"
  def mkNStoreLabelName(relName: String, propName: String) = s"_nstore-$relName-$propName"
}
