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

class NeighbourAggregatedIndexSpec extends fixture.WordSpec with Matchers {

  case class FixtureParam(neo4jServer: ServerControls) extends AutoCloseable {
    val driver: Driver = GraphDatabase.driver(neo4jServer.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)
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

    override def close(): Unit = {
      neo4jServer.close()
    }
  }

  override def withFixture(test: OneArgTest): Outcome = {
    val builder: TestServerBuilder = TestServerBuilders.newInProcessBuilder()
    WithResource(FixtureParam(builder.newServer())) { f =>
      withFixture(test.toNoArgTest(f))
    }
  }

  "A index writer" should {
    "write wls to index" in { f =>
      WithResource(f.driver.session()) { session =>
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
        WithResource(f.indexWriter.db.beginTx()) { tx =>
          // query the neighbours
          f.indexWriter.index(f.indexWriter.db.getNodeById(nodeA))
          f.indexWriter.index(f.indexWriter.db.getNodeById(nodeB))
          f.indexWriter.index(f.indexWriter.db.getNodeById(nodeC))
          f.indexReader.db.schema().awaitIndexesOnline(5, SECONDS)
          tx.success()
        }
      }
      WithResource(f.driver.session()) { session =>
        WithResource(f.indexReader.db.beginTx()) { tx =>
          // query the neighbours
          f.indexReader.indexDB.query("__nagg_2:name\\:carl").stream().iterator().asScala.toList should have length 2
          f.indexReader.indexDB.query("__nagg_2:name\\:Bob").stream().iterator().asScala.toList should have length 2
          tx.success()
        }
      }
    }
  }

}
