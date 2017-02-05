/**
  * The mixin of graphdb from neo4j
  */
package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.graphdb.GraphDatabaseService

trait GraphDBContext {

  val db: GraphDatabaseService

}
