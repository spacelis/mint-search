package uk.ac.cdrc.mintsearch.ranking

import java.io.{File, IOException}

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import uk.ac.cdrc.mintsearch.ranking.NeighbourBasedRanking.NodeId

/**
  * Created by ucfawli on 20-Nov-16.
  */
case class GraphSearchQuery(db: GraphDatabaseService, dbStore: File) {
  def close(): Unit = {
    db.shutdown()
    dbStore.delete()
  }
}

object GraphSearchQuery {
  def fromCypher(query: String): GraphSearchQuery = {
    val dbStore = mkTempDir()
    val db = new GraphDatabaseFactory().newEmbeddedDatabase(dbStore)
    db.execute(query)
    new GraphSearchQuery(db, dbStore)
  }

  def fromNodeNeighbourHood(nodeId: NodeId) = _

  val defaultTempDir = new File("/tmp")
  def mkTempDir(prefix: String = "mintsearch-", suffix: String = ".tmp.d", dir: File = defaultTempDir) = {
    val temp = File.createTempFile(prefix, suffix, dir)
    temp.delete()
    if (temp.mkdirs())
      temp
    else
      throw new IOException("Cannot create temp dir")
  }
}
