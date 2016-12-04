package uk.ac.cdrc.mintsearch.index

import org.neo4j.graphdb.{GraphDatabaseService, Node}

/**
  * Created by ucfawli on 04-Dec-16.
  */
trait NeighbourBasedIndex extends Index {
  def db: GraphDatabaseService
  def index() = for (n <- db.getAllNodes) index(n)
  def index(n: Node)
}
