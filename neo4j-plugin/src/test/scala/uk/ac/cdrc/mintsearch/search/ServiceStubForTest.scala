package uk.ac.cdrc.mintsearch.search

import org.neo4j.driver.v1.{Config, Driver, GraphDatabase}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.ServerControls
import uk.ac.cdrc.mintsearch.graph._
import uk.ac.cdrc.mintsearch.index.terrier.{TerrierIndexReader, TerrierIndexWriter}
import uk.ac.cdrc.mintsearch.index.{BaseIndexWriter, LegacyNeighbourBaseIndexReader, LegacyNeighbourBaseIndexWriter, PropertyNodeMarker}
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext
import uk.ac.cdrc.mintsearch.ranking.{NessNodeSimilarity, SimpleEmbeddingRanking, SimpleNodeRanking}

/**
  * Provide a service stub for tests
  */
trait ServiceStubForTest  extends AutoCloseable{
  val neo4jServer: ServerControls
  val driver: Driver = GraphDatabase.driver(
    neo4jServer.boltURI(),
    Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig
  )

  override def close(): Unit = {
    neo4jServer.close()
  }

  val indexWriter: BaseIndexWriter
  val graphSearcher: TruncatedSearcher with SimpleQueryBuilder
}


trait ServiceStubLegacyNeo4J extends ServiceStubForTest {

  val indexWriter = new LegacyNeighbourBaseIndexWriter
    with ExponentialPropagation
    with PropertyNodeMarker
    with NeighbourhoodByRadius
    with NeighbourAwareContext {

    override val radius: Int = 2
    override val propagationFactor: Double = 0.5

    override val labelStorePropKey: String = s"__nagg_$radius"
    override val db: GraphDatabaseService = neo4jServer.graph()
    override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
  }

  val graphSearcher = new TruncatedSearcher
    with LegacyNeighbourBaseIndexReader
    with GraphDBContext
    with ExponentialPropagation
    with PropertyNodeMarker
    with NeighbourhoodByRadius
    with NeighbourAwareContext
    with NeighbourAggregatedAnalyzer
    with NessNodeSimilarity
    with SimpleNodeRanking
    with SimpleEmbeddingRanking
    with ConnComponentEnumContext
    with SimpleQueryBuilder {

    override val radius: Int = 2
    override val propagationFactor: Double = 0.5

    override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
    override val labelStorePropKey: String = s"__nagg_$radius"
    override val db: GraphDatabaseService = neo4jServer.graph()
  }
}

trait ServiceStubTerrier extends ServiceStubForTest {

  val indexWriter = new TerrierIndexWriter
    with ExponentialPropagation
    with PropertyNodeMarker
    with NeighbourhoodByRadius
    with NeighbourAwareContext {

    override val radius: Int = 2
    override val propagationFactor: Double = 0.5

    override val labelStorePropKey: String = s"__nagg_$radius"
    override val db: GraphDatabaseService = neo4jServer.graph()
    override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
  }

  val graphSearcher = new TruncatedSearcher
    with TerrierIndexReader
    with GraphDBContext
    with ExponentialPropagation
    with PropertyNodeMarker
    with NeighbourhoodByRadius
    with NeighbourAwareContext
    with NeighbourAggregatedAnalyzer
    with NessNodeSimilarity
    with SimpleNodeRanking
    with SimpleEmbeddingRanking
    with ConnComponentEnumContext
    with SimpleQueryBuilder {

    override val radius: Int = 2
    override val propagationFactor: Double = 0.5

    override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
    override val labelStorePropKey: String = s"__nagg_$radius"
    override val db: GraphDatabaseService = neo4jServer.graph()
  }
}
