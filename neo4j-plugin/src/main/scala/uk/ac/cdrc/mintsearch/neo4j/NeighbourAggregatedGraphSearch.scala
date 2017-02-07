package uk.ac.cdrc.mintsearch.neo4j

import java.util.stream.{Stream => JStream}

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.procedure.{Mode, Name, Procedure}
import uk.ac.cdrc.mintsearch.graph.{ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius, SubGraphEnumeratorContext}
import uk.ac.cdrc.mintsearch.index.{LegacyNeighbourBaseIndexReader, LegacyNeighbourBaseIndexWriter, PropertyLabelMaker}
import uk.ac.cdrc.mintsearch.ranking.{NESSSimilarity, SimpleGraphRanking, SimpleNodeRanking}
import uk.ac.cdrc.mintsearch.search.{NeighbourAggregatedAnalyzer, NeighbourBasedSearcher, SimpleQueryBuilder}

import scala.collection.JavaConverters._
import scala.compat.java8.StreamConverters._
import scala.util.control.NonFatal

/**
  * This stub is to avoid the non-static/final fields in Neo4J procedure definition.
  */
object ServiceStub {

  private var graphSearcher: NeighbourBasedSearcher with SimpleQueryBuilder = _

  private var indexWriter: LegacyNeighbourBaseIndexWriter = _

  def getSearcher(gdb: GraphDatabaseService): NeighbourBasedSearcher with SimpleQueryBuilder = {
    if (graphSearcher == null)
      graphSearcher = new NeighbourBasedSearcher
          with LegacyNeighbourBaseIndexReader
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

  def getIndexWriter(gdb: GraphDatabaseService): LegacyNeighbourBaseIndexWriter = {
    if (indexWriter == null)
      indexWriter = new LegacyNeighbourBaseIndexWriter
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
class NeighbourAggregatedGraphSearch extends Neo4JProcedure {

  /**
    * Search via neighbour based method
    * @param query A cypher statement for creating a graph to search with
    * @return the nodes found by the query
    */
  @Procedure(name="mint.nagg_search", mode=Mode.WRITE)
  def search(@Name("query") query: String): JStream[GraphResult] = {
    try {
      val searcher = ServiceStub.getSearcher(db)
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
  @Procedure(name="mint.nagg_index", mode=Mode.SCHEMA)
  def index(): Unit = {
    try {
      ServiceStub.getIndexWriter(db).index()
    } catch {
      case NonFatal(ex) =>
        log.error("Index Failed!", ex)
        throw ex
    }
  }
}
