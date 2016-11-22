package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.cypher.export.SubGraph
import org.neo4j.graphdb.GraphDatabaseService

/**
  * Created by ucfawli on 11/18/16.
  */
trait GraphRanking {
  def db: GraphDatabaseService
  def search(gsq: GraphSearchQuery): Iterator[SubGraph]
}

object GraphRanking {
}
