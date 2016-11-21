package uk.ac.cdrc.mintsearch.ranking

import java.io.{File, IOException}

import org.neo4j.cypher.export.CypherResultSubGraph

import collection.JavaConverters._
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.traversal.TraversalDescription
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
  def fromCypher(cypher: String): GraphSearchQuery = {
    val dbStore = mkTempDir()
    val db = new GraphDatabaseFactory().newEmbeddedDatabase(dbStore)
    db.execute(cypher)
    new GraphSearchQuery(db, dbStore)
  }

  def fromNodeNeighbourHood(nodeId: NodeId)(implicit db: GraphDatabaseService, traversalDescription: TraversalDescription) = {
    //FIXME We should reconsider how can we construct a subgraph for search
    val subgraph = new CypherResultSubGraph()
    for {
      path <- traversalDescription.traverse(db.getNodeById(nodeId)).iterator().asScala
      node = path.endNode()
    } subgraph.add(node)
  }

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
