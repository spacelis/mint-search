/**
 * Test LabelMaker
 */
package uk.ac.cdrc.mintsearch.index

import org.neo4j.driver.v1.{Config, Driver, GraphDatabase}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ServerControls, TestServerBuilder, TestServerBuilders}
import org.scalatest._
import uk.ac.cdrc.mintsearch.graph.ExponentialPropagation
import uk.ac.cdrc.mintsearch.neo4j.{GraphDBContext, WithResource}

class PropertyLabelMakerSpec extends fixture.WordSpec with Matchers {

  case class FixtureParam(neo4jServer: ServerControls) extends AutoCloseable {

    lazy val driver: Driver = GraphDatabase.driver(
      neo4jServer.boltURI(),
      Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)

    val context = new GraphDBContext with PropertyLabelMaker with ExponentialPropagation {
      override val labelStorePropKey: String = s"__nagg_0"
      override val db: GraphDatabaseService = neo4jServer.graph()
      override val propagationFactor: Double = 0.5
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

  "PropertyLabelMaker" should {
    "collect labels into a WLS" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
        WithResource(driver.session()) { session =>
          val nodeId: Long = session.run(
            """CREATE
              | (a: Person {name:'Alice', gender:'Female'})
              | RETURN id(a)""".stripMargin
          )
            .single()
            .get(0).asLong()
          WithResource(context.db.beginTx()) { _ =>
            context.collectLabels(context.db.getNodeById(nodeId)) shouldBe Stream(("name", "Alice"), ("gender", "Female"))
          }
        }
      }
    }
  }

}
