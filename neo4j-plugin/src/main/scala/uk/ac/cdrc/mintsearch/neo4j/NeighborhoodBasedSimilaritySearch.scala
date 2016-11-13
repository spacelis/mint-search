package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.graphdb.index.IndexManager
import collection.JavaConverters._
import scala.language.implicitConversions

import java.util.{List => JList}
import java.util.stream.{Stream => JStream}
import java.util.function.{Function => JFunction}

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.RelationshipType
import org.neo4j.graphdb.traversal.Uniqueness
import org.neo4j.graphdb.traversal.Evaluators
import org.neo4j.graphdb.index.IndexManager
import org.neo4j.procedure.Name
import org.neo4j.procedure.PerformsWrites
import org.neo4j.procedure.Procedure

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
   * @param propKeys a list of property keys to index, only the ones the node
   *                 actually contains will be added
   */
  @Procedure("mint.ness_index")
  @PerformsWrites
  def index(
    @Name("nodeId") nodeId: Long,
    @Name("relType") relType: String,
    @Name("depth") depth: Int
  ): Unit = {
    // TODO Need to add alpha for the propagation of the labels
    val node = db.getNodeById(nodeId)
    val td = db.traversalDescription()
      .relationships(RelationshipType.withName(relType))
      .uniqueness(Uniqueness.NODE_GLOBAL)
      .evaluator(Evaluators.atDepth(depth))

    for ( node <- td.traverse().nodes().asScala;
          labels = node.getLabels().asScala mkString " ";
          index = db.index().forNodes(NeighborhoodBasedSimilaritySearch.LABEL_INDEX_NAME, NeighborhoodBasedSimilaritySearch.FULL_TEXT.asJava)
         ) {

      // In case the node is indexed before, remove all occurrences of it so
      // we don't get old or duplicated data
      index.remove(node)

      index.add(node, NeighborhoodBasedSimilaritySearch.LABEL_INDEX_NAME, labels)
    }
  }

}

object NeighborhoodBasedSimilaritySearch {
  val FULL_TEXT = Map(IndexManager.PROVIDER -> "lucene", "type" -> "fulltext")
  val LABEL_INDEX_NAME = "labelstore"
}
