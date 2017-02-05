/**
  * Define the common type of index from Neo4J
  */

package uk.ac.cdrc.mintsearch.index

object Neo4JIndexTypes {
  import org.neo4j.graphdb.index.IndexManager
  val FULL_TEXT = Map(IndexManager.PROVIDER -> "lucene", "type" -> "fulltext")
  val EXACT_TEXT = Map(IndexManager.PROVIDER -> "lucene", "type" -> "exact")
}
