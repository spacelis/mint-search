package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.driver.v1.{Config, Driver, GraphDatabase}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ServerControls, TestServerBuilder, TestServerBuilders}
import org.scalatest.{Matchers, Outcome, fixture}
import uk.ac.cdrc.mintsearch.index.KeyValueNode
import uk.ac.cdrc.mintsearch.neo4j.{GraphDBContext, WithResource}

/**
  * Created by ucfawli on 27-Feb-17.
  */
class NeighbourhoodEnhancedScoringSpec extends fixture.WordSpec with Matchers {

  case class FixtureParam(neo4jServer: ServerControls) extends AutoCloseable {

    lazy val driver: Driver = GraphDatabase.driver(
      neo4jServer.boltURI(),
      Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig
    )

    val context = new GraphDBContext with NeighbourhoodEnhancedScoring with KeyValueNode {

      override val db: GraphDatabaseService = neo4jServer.graph()
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

  "nonZeroMin" should {
    "find a min other than zero" in { f =>
      import f.context._
      nonZeroMin(Array(0d, 1d, 2d)) should be (1d)
    }
  }
}
