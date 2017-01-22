package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.graphdb.index.IndexManager

/**
 * Created by ucfawli on 08-Jan-17.
 */
object Neo4JIndexTypes {
  val FULL_TEXT = Map(IndexManager.PROVIDER -> "lucene", "type" -> "fulltext")
  val EXACT_TEXT = Map(IndexManager.PROVIDER -> "lucene", "type" -> "exact")
}
