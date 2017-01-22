package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.driver.v1.{ Config, Driver, GraphDatabase }
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ ServerControls, TestServerBuilder, TestServerBuilders }
import org.scalatest.{ Matchers, WordSpec }

/**
 * Created by ucfawli on 21-Jan-17.
 */
class PropertyLabelMakerSpec extends WordSpec with Matchers {

  trait Neo4JFixture {
    private val _builder = TestServerBuilders.newInProcessBuilder()
    def builder: TestServerBuilder = _builder
    lazy val neo4jServer: ServerControls = builder.newServer()
    lazy val driver: Driver = GraphDatabase.driver(neo4jServer.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)

    val context = new GraphContext with PropertyLabelMaker {
      override val labelStorePropKey: String = s"__nagg_0"
      override val db: GraphDatabaseService = neo4jServer.graph()
    }
  }

  "PropertyLabelMaker" should {
    "collect labels into a WLS" in new Neo4JFixture {
      WithResource(driver.session()) { session =>
        val nodeId: Long = session.run(
          """CREATE
            | (a: Person {name:'Alice', gender:'Female'})
            | RETURN id(a)""".stripMargin
        )
          .single()
          .get(0).asLong()
        WithResource(context.db.beginTx()) { _ =>
          context.collectLabels(context.db.getNodeById(nodeId)) shouldBe Stream("name:Alice", "gender:Female")
        }
      }
    }
  }

}
