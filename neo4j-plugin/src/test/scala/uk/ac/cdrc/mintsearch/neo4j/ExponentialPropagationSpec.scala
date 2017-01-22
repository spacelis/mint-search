package uk.ac.cdrc.mintsearch.neo4j

import org.neo4j.driver.v1.{ Config, Driver, GraphDatabase }
import org.neo4j.graphalgo.impl.util.PathImpl
import org.neo4j.graphdb.{ GraphDatabaseService, RelationshipType }
import org.neo4j.harness.{ ServerControls, TestServerBuilder, TestServerBuilders }
import org.scalatest.{ Matchers, WordSpec }
import uk.ac.cdrc.mintsearch.neighbourhood.ExponentialPropagation

import scala.collection.JavaConverters._

/**
 * Created by ucfawli on 21-Jan-17.
 */
class ExponentialPropagationSpec extends WordSpec with Matchers {

  trait Neo4JFixture {
    private val _builder = TestServerBuilders.newInProcessBuilder()
    def builder: TestServerBuilder = _builder
    lazy val neo4jServer: ServerControls = builder.newServer()
    lazy val driver: Driver = GraphDatabase.driver(neo4jServer.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)

    val context = new GraphContext with PropertyLabelMaker with ExponentialPropagation {
      override val labelStorePropKey: String = s"__nagg_0"
      override val db: GraphDatabaseService = neo4jServer.graph()
      override val propagationFactor: Double = 0.5
    }
  }

  class Friendship extends RelationshipType {
    override def name(): String = "Friend"
  }

  "PropertyLabelMaker" should {
    "collect labels into a WLS" in new Neo4JFixture {
      WithResource(driver.session()) { session =>
        val nodeIdA: Long = session.run(
          """CREATE
            | (a: Person {name:'Alice', gender:'Female'}),
            | (b: Person {name:'Bob', gender:'Male'}),
            | (c: Person {name:'Carl', gender:'Male'}),
            | (a) -[:Friend]-> (b),
            | (b) -[:Friend]-> (c)
            | RETURN id(a)""".stripMargin
        )
          .single()
          .get(0).asLong()
        WithResource(context.db.beginTx()) { _ =>
          val nodeA = context.db.getNodeById(nodeIdA)
          val friendAB = nodeA.getRelationships.asScala.toList(0)
          val builder: PathImpl.Builder = new PathImpl.Builder(nodeA)
          val path = builder.push(friendAB).build()
          context.propagate(path) should be(Map("name:Bob" -> 0.5, "gender:Male" -> 0.5))
          val path2 = builder.push(friendAB).push(friendAB.getEndNode.getRelationships.asScala.toList(0)).build
          context.propagate(path2) should be(Map("name:Carl" -> 0.25, "gender:Male" -> 0.25))
        }
      }
    }
  }

}

