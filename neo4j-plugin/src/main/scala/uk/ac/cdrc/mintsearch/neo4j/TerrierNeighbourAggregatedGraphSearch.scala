package uk.ac.cdrc.mintsearch.neo4j

import java.util.stream.{Stream => JStream}

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.procedure.{Mode, Name, Procedure}
import uk.ac.cdrc.mintsearch.ServiceStub
import uk.ac.cdrc.mintsearch.graph.{ConnComponentEnumContext, NodeOnlyAsciiRenderer}
import uk.ac.cdrc.mintsearch.index.BaseIndexWriter
import uk.ac.cdrc.mintsearch.index.terrier.{TerrierIndexReader, TerrierIndexWriter}
import uk.ac.cdrc.mintsearch.ranking.{NessNodeSimilarity, SimpleEmbeddingRanking, SimpleNodeRanking}
import uk.ac.cdrc.mintsearch.search._

import scala.collection.JavaConverters._
import scala.compat.java8.StreamConverters._
import scala.util.control.NonFatal

/**
  * @inheritdoc
  * This stub defines the Terrier indexing/searching infrastructure for the procedures.
  */
object ServiceStubUponTerrierIndex extends ServiceStub {

  /**
    * @inheritdoc
    * @param gdb a graph database service
    * @return a searcher instance
    */
  override def getSearcher(gdb: GraphDatabaseService): GraphSearcher with SimpleQueryBuilder = {
    if (graphSearcher == null)
      graphSearcher = new LargePoolSimpleSearcher
        with TerrierIndexReader
        with GraphDBContext
        with ConfR2expPropLIdx
        with NeighbourAggregatedAnalyzer
        with NessNodeSimilarity
        with SimpleNodeRanking
        with SimpleEmbeddingRanking
        with ConnComponentEnumContext
        with SimpleQueryBuilder {

        override val db: GraphDatabaseService = gdb
      }
    graphSearcher
  }

  /**
    * @inheritdoc
    * @param gdb a graph database service
    * @return a index writer instance
    */
  override def getIndexWriter(gdb: GraphDatabaseService): BaseIndexWriter = {
    if (indexWriter == null)
      indexWriter = new TerrierIndexWriter
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
class TerrierNeighbourAggregatedGraphSearch extends Neo4JProcedure {

  /**
    * Search via neighbour based method
    * @param query A cypher statement for creating a graph to search with
    * @return the nodes found by the query
    */
  @Procedure(name="mint.naggt_search", mode=Mode.WRITE)
  def search(@Name("query") query: String): JStream[GraphResult] = {
    try {
      import ServiceStubUponTerrierIndex._
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
  @Procedure(name="mint.naggt_index", mode=Mode.SCHEMA)
  def index(): Unit = {
    try {
      ServiceStubUponTerrierIndex.getIndexWriter(db).index()
    } catch {
      case NonFatal(ex) =>
        log.error("Index Failed!", ex)
        throw ex
    }
  }

  /**
    * Delete the index
    */
  @Procedure(name="mint.naggt_reset_index", mode=Mode.SCHEMA)
  def reset_index(): Unit = {
    try {
      ServiceStubUponTerrierIndex.getIndexWriter(db).reset_index()
    } catch {
      case NonFatal(ex) =>
        log.error("Index Failed!", ex)
        throw ex
    }
  }
}
