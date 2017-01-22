package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.graphdb.GraphDatabaseService

/**
 * Created by ucfawli on 08-Jan-17.
 */
trait GraphContext {

  val db: GraphDatabaseService

}
