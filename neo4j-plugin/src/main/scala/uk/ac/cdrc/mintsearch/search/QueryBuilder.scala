/**
  * Graph query builders for graph retrieval
  */
package uk.ac.cdrc.mintsearch.search

import java.io.{File, IOException, PrintWriter, StringWriter}

import org.apache.commons.io.FileUtils
import org.neo4j.cypher.export.{CypherResultSubGraph, SubGraphExporter}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext

trait QueryBuilder

trait GraphQuery extends AutoCloseable {
  import QueryBuilder._
  val qdbStore: File = mkTempDir()
  val qdb: GraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(qdbStore)
  override def close(): Unit = {
    qdb.shutdown()
    qdb.isAvailable(5000)
    FileUtils.deleteDirectory(qdbStore)
  }
}

trait SimpleQueryBuilder extends QueryBuilder {

  case class SimpleQuery() extends GraphQuery

  def fromCypherCreate(cypher: String): GraphQuery = {
    val gsq = SimpleQuery()
    gsq.qdb.execute(cypher)
    gsq
  }

}

trait DependentQueryBuilder extends SimpleQueryBuilder {
  self: GraphDBContext =>

  def fromNeighbourHood(nodeId: NodeId, range: Int): GraphQuery = {
    assert(range > 0, "The range must be larger than 0")
    val subMatchingPatten = (for (i <- 1 to range) yield s"(_$i)") mkString "--"
    val subReturningStmt = (for (i <- 1 to range) yield s"_$i") mkString ", "
    val query = s"MATCH (n)--$subMatchingPatten WHERE ID(n) = $nodeId RETURN n, $subReturningStmt"
    fromCypherQuery(query)
  }

  def fromCypherQuery(query: String): GraphQuery = {
    val sWriter = new StringWriter
    new SubGraphExporter(CypherResultSubGraph.from(db.execute(query), db, true)).export(new PrintWriter(sWriter))
    fromCypherCreate(sWriter.getBuffer.toString)
  }

}

object QueryBuilder {

  val defaultTempDir = new File(System.getProperty("java.io.tmpdir"))
  def mkTempDir(prefix: String = "mintsearch-", suffix: String = ".tmp.d", dir: File = defaultTempDir): File = {
    val temp = File.createTempFile(prefix, suffix, dir)
    temp.delete()
    if (temp.mkdirs())
      temp
    else
      throw new IOException("Cannot create temp dir")
  }
}
