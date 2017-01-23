/**
 * Test for indexing
 */

package uk.ac.cdrc.mintsearch.index

import org.neo4j.driver.v1._
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ ServerControls, TestServerBuilder, TestServerBuilders }
import org.scalatest._
import uk.ac.cdrc.mintsearch.neighbourhood.{ ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius }
import uk.ac.cdrc.mintsearch.neo4j.{ PropertyLabelMaker, WithResource }
import uk.ac.cdrc.mintsearch.ranking.{ SimpleNeighbourSimilarity, SimpleNodeRanking }
import java.util.concurrent.TimeUnit.SECONDS

import scala.collection.JavaConverters._

class NeighbourAggregatedIndexSpec extends WordSpec with Matchers {

  trait Neo4JFixture {
    private val _builder = TestServerBuilders.newInProcessBuilder()
    def builder: TestServerBuilder = _builder
    lazy val neo4jServer: ServerControls = builder.newServer()
    lazy val driver: Driver = GraphDatabase.driver(neo4jServer.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)

    val indexWriter = new NeighbourAggregatedIndexWriter with ExponentialPropagation with PropertyLabelMaker with NeighbourhoodByRadius with NeighbourAwareContext {

      override val radius: Int = 2
      override val propagationFactor: Double = 0.5

      override val labelStorePropKey: String = s"__nagg_$radius"
      override val db: GraphDatabaseService = neo4jServer.graph()
      override val indexName: String = "ness_index"
    }
    val indexReader = new NeighbourAggregatedIndexReader with ExponentialPropagation with PropertyLabelMaker with NeighbourhoodByRadius with NeighbourAwareContext with SimpleNeighbourSimilarity with SimpleNodeRanking {

      override val radius: Int = 2
      override val propagationFactor: Double = 0.5

      override val labelStorePropKey: String = s"__nagg_$radius"
      override val db: GraphDatabaseService = neo4jServer.graph()
      override val indexName: String = "ness_index"
    }
  }

  "A index writer" should {
    "write wls to index" in new Neo4JFixture {
      WithResource(driver.session()) { session =>
        val res = session.run(
          """CREATE
            | (a: Person {name:'Alice'}),
            | (b: Person {name: 'Bob'}),
            | (c: Person {name: 'Carl'}),
            | (a)-[:Friend]->(b)-[:Friend]->(c)
            | RETURN id(a), id(b), id(c)""".stripMargin
        )
          .single()
        val Seq(nodeA, nodeB, nodeC) = for (i <- 0 to 2) yield res.get(i).asLong
        WithResource(indexWriter.db.beginTx()) { tx =>
          // query the neighbours
          indexWriter.index(indexWriter.db.getNodeById(nodeA))
          tx.success()
        }
      }
      WithResource(indexReader.db.beginTx()) { tx =>
        indexReader.db.schema().awaitIndexesOnline(5, SECONDS)
      }
      Thread.sleep(60 * 1000)
      WithResource(driver.session()) { session =>
        WithResource(indexReader.db.beginTx()) { tx =>
          // query the neighbours
          indexReader.indexDB.query("*:*").stream().iterator().asScala.toList should be('empty)
          tx.success()
        }
      }
    }
  }

}
