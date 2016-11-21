package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.graphdb.GraphDatabaseService

/**
  * Created by ucfawli on 11/18/16.
  */
trait Ranking {
  val db: GraphDatabaseService
}

object Ranking {
}
