package uk.ac.cdrc.mintsearch.search

import org.neo4j.driver.v1.{Config, Driver, GraphDatabase}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.ServerControls
import uk.ac.cdrc.mintsearch.graph.{ConnComponentEnumContext, ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius}
import uk.ac.cdrc.mintsearch.index.terrier.{TerrierIndexReader, TerrierIndexWriter}
import uk.ac.cdrc.mintsearch.index.{BaseIndexWriter, LegacyNeighbourBaseIndexReader, LegacyNeighbourBaseIndexWriter, PropertyLabelMaker}
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext
import uk.ac.cdrc.mintsearch.ranking.{NESSSimilarity, SimpleGraphRanking, SimpleNodeRanking}

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
  val graphSearcher: NeighbourBasedSearcher with SimpleQueryBuilder
}


trait ServiceStubLegacyNeo4J extends ServiceStubForTest {

  val indexWriter = new LegacyNeighbourBaseIndexWriter
    with ExponentialPropagation
    with PropertyLabelMaker
    with NeighbourhoodByRadius
    with NeighbourAwareContext {

    override val radius: Int = 2
    override val propagationFactor: Double = 0.5

    override val labelStorePropKey: String = s"__nagg_$radius"
    override val db: GraphDatabaseService = neo4jServer.graph()
    override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
  }

  val graphSearcher = new NeighbourBasedSearcher
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
    with PropertyLabelMaker
    with NeighbourhoodByRadius
    with NeighbourAwareContext {

    override val radius: Int = 2
    override val propagationFactor: Double = 0.5

    override val labelStorePropKey: String = s"__nagg_$radius"
    override val db: GraphDatabaseService = neo4jServer.graph()
    override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
  }

  val graphSearcher = new NeighbourBasedSearcher
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
    with ConnComponentEnumContext
    with SimpleQueryBuilder {

    override val radius: Int = 2
    override val propagationFactor: Double = 0.5

    override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
    override val labelStorePropKey: String = s"__nagg_$radius"
    override val db: GraphDatabaseService = neo4jServer.graph()
  }
}
