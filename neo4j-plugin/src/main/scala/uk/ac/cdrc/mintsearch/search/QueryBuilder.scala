/**
  * Graph query builders for graph retrieval
  */
package uk.ac.cdrc.mintsearch.search

import java.io.{File, PrintWriter, StringWriter}

import org.neo4j.cypher.export.{CypherResultSubGraph, SubGraphExporter}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext

trait QueryBuilder

trait GraphQuery extends AutoCloseable {
  private val qdbDir = TempDir()
  val qdbStore: File = qdbDir.value
  val qdb: GraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(qdbStore)
  override def close(): Unit = {
    qdb.shutdown()
    qdb.isAvailable(5000)
    qdbDir.close()
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

}
