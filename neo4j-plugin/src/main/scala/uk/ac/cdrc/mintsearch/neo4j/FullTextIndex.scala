package uk.ac.cdrc.mintsearch.neo4j

import java.util.List
import java.util.stream.Stream

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.index.IndexManager
import org.neo4j.procedure.{Name, PerformsWrites, Procedure}

import scala.collection.JavaConverters._
import scala.compat.java8.StreamConverters._

/**
 * This is an example showing how you could expose Neo4j's full text indexes as
 * two procedures - one for updating indexes, and one for querying by label and
 * the lucene query language.
 */
class FullTextIndex extends Neo4JProcedure {

  /**
   * This declares the first of two procedures in this class - a
   * procedure that performs queries in a legacy index.
   *
   * It returns a Stream of Records, where records are
   * specified per procedure. This particular procedure returns
   * a stream of {@link SearchHit} records.
   *
   * The arguments to this procedure are annotated with the
   * {@link Name} annotation and define the position, name
   * and type of arguments required to invoke this procedure.
   * There is a limited set of types you can use for arguments,
   * these are as follows:
   *
   * <ul>
   *     <li>{@link String}</li>
   *     <li>{@link Long} or {@code long}</li>
   *     <li>{@link Double} or {@code double}</li>
   *     <li>{@link Number}</li>
   *     <li>{@link Boolean} or {@code boolean}</li>
   *     <li>{@link java.util.Map} with key {@link String} and value {@link Object}</li>
   *     <li>{@link java.util.List} of elements of any valid argument type, including {@link java.util.List}</li>
   *     <li>{@link Object}, meaning any of the valid argument types</li>
   * </ul>
   *
   * @param label the label name to query by
   * @param query the lucene query, for instance `name:Brook*` to
   *              search by property `name` and find any value starting
   *              with `Brook`. Please refer to the Lucene Query Parser
   *              documentation for full available syntax.
   * @return the nodes found by the query
   */
  @Procedure("mint.fulltext_search")
  @PerformsWrites // TODO: This is here as a workaround, because index().forNodes() is not read-only
  def search(
    @Name("label") label: String,
    @Name("query") query: String
  ): Stream[SearchHit] = {
    val index = indexName(label)

    // Avoid creating the index, if it's not there we won't be
    // finding anything anyway!
    if (!db.index().existsForNodes(index)) {
      // Just to show how you'd do logging
      log.debug("Skipping index query since index does not exist: `%s`", index)
      return Stream.empty()
    }

    log.debug("Index uses: `%s`", index)
    // If there is an index, do a lookup and convert the result
    // to our output record.
    db.index().forNodes(index).query(query).iterator().asScala.map((n: Node) => { new SearchHit(n) }).seqStream
  }

  /**
   * This is the second procedure defined in this class, it is used to update the
   * index with nodes that should be queryable. You can send the same node multiple
   * times, if it already exists in the index the index will be updated to match
   * the current state of the node.
   *
   * This procedure works largely the same as {@link #search(String, String)},
   * with two notable differences. One, it is annotated with {@link PerformsWrites},
   * which is <i>required</i> if you want to perform updates to the graph in your
   * procedure.
   *
   * Two, it returns {@code void} rather than a stream. This is simply a short-hand
   * for saying our procedure always returns an empty stream of empty records.
   *
   * @param nodeId the id of the node to index
   * @param propKeys a list of property keys to index, only the ones the node
   *                 actually contains will be added
   */
  @Procedure("mint.fulltext_index")
  @PerformsWrites
  def index(
    @Name("nodeId") nodeId: Long,
    @Name("properties") propKeys: List[String]
  ): Unit = {
    val node = db.getNodeById(nodeId)

    // Load all properties for the node once and in bulk,
    // the resulting set will only contain those properties in `propKeys`
    // that the node actually contains.
    val properties =
      node.getProperties(propKeys.asScala: _*).entrySet()

    // Index every label (this is just as an example, we could filter which labels to index)
    for (
      label <- node.getLabels().asScala;
      index = db.index().forNodes(indexName(label.name()), FullTextIndex.FULL_TEXT.asJava)
    ) {

      // In case the node is indexed before, remove all occurrences of it so
      // we don't get old or duplicated data
      index.remove(node)

      // And then index all the properties
      for (property <- properties.asScala) {
        index.add(node, property.getKey(), property.getValue())
      }
    }
  }

  private def indexName(label: String) = "label-" + label
}

object FullTextIndex {
  // Only static fields and @Context-annotated fields are allowed in
  // Procedure classes. This static field is the configuration we use
  // to create full-text indexes.
  val FULL_TEXT = Map(IndexManager.PROVIDER -> "lucene", "type" -> "fulltext")
}
