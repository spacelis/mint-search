package uk.ac.cdrc.mintsearch.neo4j

import java.util.stream.{Stream => JStream}

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.procedure.{Mode, Name, Procedure}
import uk.ac.cdrc.mintsearch.ServiceStub
import uk.ac.cdrc.mintsearch.graph.{NodeOnlyAsciiRenderer, TopFirstEmbeddingEnumContext}
import uk.ac.cdrc.mintsearch.index.{BaseIndexWriter, LegacyNeighbourBaseIndexReader, LegacyNeighbourBaseIndexWriter}
import uk.ac.cdrc.mintsearch.ranking.{NessNodeSimilarity, SimpleEmbeddingRanking, SimpleNodeRanking}
import uk.ac.cdrc.mintsearch.search._

import scala.collection.JavaConverters._
import scala.compat.java8.StreamConverters._
import scala.util.control.NonFatal

/**
  * This stub defines the Neo4J's Lucene indexing/searching infrastructure for the procedures.
  */
object ServiceStubUponNeo4JIndex extends ServiceStub {

  /**
    * @inheritdoc
    * @param gdb a graph database service
    * @return a searcher instance
    */
  def getSearcher(gdb: GraphDatabaseService): GraphSearcher with SimpleQueryBuilder = {
    if (graphSearcher == null)
      graphSearcher = new LargePoolSimpleSearcher
          with LegacyNeighbourBaseIndexReader
          with ConfR2expPropLIdx
          with GraphDBContext
          with NeighbourAggregatedAnalyzer
          with NessNodeSimilarity
          with SimpleNodeRanking
          with SimpleEmbeddingRanking
          with TopFirstEmbeddingEnumContext
          with SimpleQueryBuilder {
        override val mini: Boolean = true
        override val db: GraphDatabaseService = gdb
      }
    graphSearcher
  }

  /**
    * @inheritdoc
    * @param gdb a graph database service
    * @return a index writer instance
    */
  def getIndexWriter(gdb: GraphDatabaseService): BaseIndexWriter = {
    if (indexWriter == null)
      indexWriter = new LegacyNeighbourBaseIndexWriter
          with GraphDBContext
          with ConfR2expPropLIdx {

        override val db: GraphDatabaseService = gdb
      }
    indexWriter
  }

  val renderer = NodeOnlyAsciiRenderer(Seq("value"))
}

/**
  * This class implements the NEighborhood based Similarity Search
  */
class NeighbourAggregatedGraphSearchL extends Neo4JProcedure {

  /**
    * Search via neighbour based method
    * @param query A cypher statement for creating a graph to search with
    * @return the nodes found by the query
    */
  @Procedure(name="mint.nagg_search", mode=Mode.WRITE)
  def search(@Name("query") query: String): JStream[GraphResult] = {
    try {
      import ServiceStubUponNeo4JIndex._
      import renderer._
      val searcher = getSearcher(db)
      WithResource(searcher.fromCypherCreate(query)){ q =>
        val res = searcher.search(q)
        (for {
          ((g, s), i) <- (res.graphSnippets zip res.scores).zipWithIndex
        } yield {
          log.info(s"graphs[$i] = $s \n${g.render}\n=======")
          new GraphResult(g.nodes.asJava, g.relationships.asJava)
        }).seqStream
      }
    } catch {
      case NonFatal(ex) =>
        log.error("Search Failed", ex)
        throw ex
    }
  }

  /**
    * Create a Neighbour based index for all nodes
    */
  @Procedure(name="mint.nagg_index", mode=Mode.SCHEMA)
  def index(): Unit = {
    try {
      ServiceStubUponNeo4JIndex.getIndexWriter(db).index()
    } catch {
      case NonFatal(ex) =>
        log.error("Index Failed!", ex)
        throw ex
    }
  }

  /**
    * Delete index
    */
  @Procedure(name="mint.nagg_reset_index", mode=Mode.SCHEMA)
  def reset_index(): Unit = {
    try {
      ServiceStubUponNeo4JIndex.getIndexWriter(db).reset_index()
    } catch {
      case NonFatal(ex) =>
        log.error("Deleting index Failed!", ex)
        throw ex
    }
  }
}
