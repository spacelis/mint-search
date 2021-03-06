package uk.ac.cdrc.mintsearch.neo4j

import java.util.stream.{Stream => JStream}

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.procedure.{Mode, Name, Procedure}
import uk.ac.cdrc.mintsearch.ServiceStub
import uk.ac.cdrc.mintsearch.graph.{ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius, SubGraphEnumeratorContext}
import uk.ac.cdrc.mintsearch.index.terrier.{TerrierIndexReader, TerrierIndexWriter}
import uk.ac.cdrc.mintsearch.index.{BaseIndexWriter, PropertyLabelMaker}
import uk.ac.cdrc.mintsearch.ranking.{NESSSimilarity, SimpleGraphRanking, SimpleNodeRanking}
import uk.ac.cdrc.mintsearch.search.{NeighbourAggregatedAnalyzer, NeighbourBasedSearcher, SimpleQueryBuilder}

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
  override def getSearcher(gdb: GraphDatabaseService): NeighbourBasedSearcher with SimpleQueryBuilder = {
    if (graphSearcher == null)
      graphSearcher = new NeighbourBasedSearcher
        with TerrierIndexReader
        with GraphDBContext
        with ExponentialPropagation
        with PropertyLabelMaker
        with NeighbourhoodByRadius
        with NeighbourAwareContext
        with NeighbourAggregatedAnalyzer
        with NESSSimilarity
        with SimpleNodeRanking
        with SimpleGraphRanking
        with SubGraphEnumeratorContext
        with SimpleQueryBuilder {

        override val radius: Int = 2
        override val propagationFactor: Double = 0.5

        override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
        override val labelStorePropKey: String = s"__nagg_$radius"
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
        with ExponentialPropagation
        with PropertyLabelMaker
        with NeighbourhoodByRadius
        with NeighbourAwareContext {

        override val radius: Int = 2
        override val propagationFactor: Double = 0.5

        override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
        override val labelStorePropKey: String = s"__nagg_$radius"
        override val db: GraphDatabaseService = gdb
      }
    indexWriter
  }
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
      val searcher = ServiceStubUponTerrierIndex.getSearcher(db)
      WithResource(searcher.fromCypherCreate(query)){ q =>
        (for {
          g <- searcher.search(q).graphSnippets
        } yield new GraphResult(g.nodes.asJava, g.relationships.asJava)).seqStream
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
}
