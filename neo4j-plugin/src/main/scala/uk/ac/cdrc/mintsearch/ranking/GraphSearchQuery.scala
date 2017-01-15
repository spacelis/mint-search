package uk.ac.cdrc.mintsearch.ranking

import java.io.{ File, IOException, PrintWriter, StringWriter }

import org.neo4j.cypher.export.CypherResultSubGraph

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.cypher.export.SubGraphExporter
import uk.ac.cdrc.mintsearch._

/**
 * Created by ucfawli on 20-Nov-16.
 */
case class GraphSearchQuery(qdb: GraphDatabaseService, qdbStore: File) extends AutoCloseable{
  override def close(): Unit = {
    qdb.shutdown()
    qdbStore.delete()
  }
}

object GraphSearchQuery {
  def fromCypherCreate(cypher: String): GraphSearchQuery = {
    val dbStore = mkTempDir()
    val db = new GraphDatabaseFactory().newEmbeddedDatabase(dbStore)
    db.execute(cypher)
    new GraphSearchQuery(db, dbStore)
  }

  def fromNeighbourHood(nodeId: NodeId, range: Int)(implicit db: GraphDatabaseService): GraphSearchQuery = {
    assert(range > 0, "The range must be larger than 0")
    val subMatchingPatten = (for (i <- 1 to range) yield s"(_$i)") mkString "--"
    val subReturningStmt = (for (i <- 1 to range) yield s"_$i") mkString ", "
    val query = s"MATCH (n)--$subMatchingPatten WHERE ID(n) = $nodeId RETURN n, $subReturningStmt"
    fromCypherQuery(query)
  }

  def fromCypherQuery(query: String)(implicit db: GraphDatabaseService): GraphSearchQuery = {
    val sWriter = new StringWriter
    new SubGraphExporter(CypherResultSubGraph.from(db.execute(query), db, true)).export(new PrintWriter(sWriter))
    fromCypherCreate(sWriter.getBuffer.toString)
  }

  val defaultTempDir = new File(System.getProperty("java.io.tmpdir"))
  def mkTempDir(prefix: String = "mintsearch-", suffix: String = ".tmp.d", dir: File = defaultTempDir) = {
    val temp = File.createTempFile(prefix, suffix, dir)
    temp.delete()
    if (temp.mkdirs())
      temp
    else
      throw new IOException("Cannot create temp dir")
  }
}
