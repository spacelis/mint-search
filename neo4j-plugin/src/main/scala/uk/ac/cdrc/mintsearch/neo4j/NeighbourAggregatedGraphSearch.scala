package uk.ac.cdrc.mintsearch.neo4j

import java.util.stream.{Stream => JStream}

import org.neo4j.cypher.export.SubGraph
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.procedure.{Name, PerformsWrites, Procedure}
import uk.ac.cdrc.mintsearch.graph.SubGraphEnumeratorContext
import uk.ac.cdrc.mintsearch.index.{LegacyNeighbourNodeIndexReader, LegacyNeighbourNodeIndexWriter, PropertyLabelMaker}
import uk.ac.cdrc.mintsearch.neighbourhood.{ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius}
import uk.ac.cdrc.mintsearch.ranking.{NeighbourhoodRanking, SimpleNeighbourSimilarity, SimpleNodeRanking}
import uk.ac.cdrc.mintsearch.search.{NeighbourAggregatedAnalyzer, SimpleGraphQueryBuilder}

import scala.compat.java8.StreamConverters._

/**
 * This class implements the NEighborhood based Similarity Search
 */
class NeighbourAggregatedGraphSearch extends Neo4JProcedure {

  val graphSearcher = new NeighbourhoodRanking
    with LegacyNeighbourNodeIndexReader
    with GraphDBContext
    with ExponentialPropagation
    with PropertyLabelMaker
    with NeighbourhoodByRadius
    with NeighbourAwareContext
    with NeighbourAggregatedAnalyzer
    with SimpleNeighbourSimilarity
    with SimpleNodeRanking
    with SubGraphEnumeratorContext
    with SimpleGraphQueryBuilder {

    override val radius: Int = 2
    override val propagationFactor: Double = 0.5

    override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
    override val labelStorePropKey: String = s"__nagg_$radius"
    override val db: GraphDatabaseService = NeighbourAggregatedGraphSearch.this.db
  }

  val indexWriter = new LegacyNeighbourNodeIndexWriter
    with GraphDBContext
    with ExponentialPropagation
    with PropertyLabelMaker
    with NeighbourhoodByRadius
    with NeighbourAwareContext {

    override val radius: Int = 2
    override val propagationFactor: Double = 0.5

    override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
    override val labelStorePropKey: String = s"__nagg_$radius"
    override val db: GraphDatabaseService = NeighbourAggregatedGraphSearch.this.db
  }
  /**
   *
   * @param query    A cypher statement for creating a graph to search with
   * @return the nodes found by the query
   */
  @Procedure("mint.nagg_search")
  @PerformsWrites // TODO: This is here as a workaround, because index().forNodes() is not read-only
  def search(@Name("query") query: String): JStream[SubGraph] = {
    graphSearcher.search(graphSearcher.fromCypherCreate(query)).seqStream
  }

  /**
   * Create a Neighbour based index for all nodes
   */
  @Procedure("mint.nagg_index")
  @PerformsWrites
  def index(): Unit = {

    indexWriter.index()
  }
}

