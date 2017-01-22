package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.driver.v1._
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ ServerControls, TestServerBuilder, TestServerBuilders }
import org.scalatest._
import uk.ac.cdrc.mintsearch.neighbourhood.{ ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius }
import uk.ac.cdrc.mintsearch.neo4j.{ GraphContext, PropertyLabelMaker, WithResource }

import scala.collection.JavaConverters._

/**
 * Testing NeighbourAwareNode
 */

class NeighbourAwareNodeSpec extends WordSpec with Matchers {

  trait Neo4JFixture {
    private val _builder = TestServerBuilders.newInProcessBuilder()
    def builder: TestServerBuilder = _builder
    lazy val neo4jServer: ServerControls = builder.newServer()
    lazy val driver: Driver = GraphDatabase.driver(neo4jServer.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)

    val context = new GraphContext with ExponentialPropagation with PropertyLabelMaker with NeighbourhoodByRadius with NeighbourAwareContext {

      override val radius: Int = 2
      override val propagationFactor: Double = 0.5

      override val labelStorePropKey: String = s"__nagg_$radius"
      override val db: GraphDatabaseService = neo4jServer.graph()
    }
  }

  "A neighbour aware node" should {

    "return paths to neighbours" in new Neo4JFixture {
      WithResource(driver.session()) { session =>
        import context.nodeWrapper
        // create a simple graph with two order of relationship friend
        val nodeId: Long = session.run(
          """CREATE
            | (a: Person {name:'Alice'}),
            | (b: Person {name: 'Bob'}),
            | (c: Person {name: 'Carl'}),
            | (a)-[:Friend]->(b)-[:Friend]->(c)
            | RETURN id(a)""".stripMargin
        )
          .single()
          .get(0).asLong()

        // create a wrapper function
        WithResource(context.db.beginTx()) { _ =>
          // query the neighbours
          val result = (for {
            p <- context.db.getNodeById(nodeId).neighbours()
            n <- p.nodes().asScala
          } yield n.getProperty("name").toString).toSet
          result should contain("Alice")
          result should contain("Bob")
          result should contain("Carl")
        }
      }
    }

    "return many neighbours" in new Neo4JFixture {
      WithResource(driver.session()) { session =>
        import context.nodeWrapper
        // create a simple graph with two order of relationship friend
        val nodeId: Long = session.run(
          """CREATE
            | (a: Person {name: 'Alice'}),
            | (b: Person {name: 'Bob'}),
            | (c: Person {name: 'Carl'}),
            | (d: Person {name: 'David'}),
            | (e: Person {name: 'Elizabeth'}),
            | (a)-[:Friend]->(b)-[:Friend]->(c),
            | (a)-[:Friend]->(d)-[:Friend]->(e)
            | RETURN id(a)""".stripMargin
        )
          .single()
          .get(0).asLong()

        // create a wrapper function
        WithResource(context.db.beginTx()) { _ =>
          // query the neighbours
          val result = (for {
            p <- context.db.getNodeById(nodeId).neighbours()
            n <- p.nodes().asScala
          } yield n.getProperty("name").toString).toSet
          result should contain("Alice")
          result should contain("Bob")
          result should contain("Carl")
          result should contain("David")
        }
      }
    }

    "return all neighbours within 2 hops" in new Neo4JFixture {
      WithResource(driver.session()) { session =>
        import context.nodeWrapper
        // create a simple graph with two order of relationship friend
        val nodeId: Long = session.run(
          """CREATE
            | (a: Person {name: 'Alice'}),
            | (b: Person {name: 'Bob'}),
            | (c: Person {name: 'Carl'}),
            | (d: Person {name: 'David'}),
            | (a)-[:Friend]->(b)-[:Friend]->(c),
            | (a)-[:Friend]->(d)-[:Friend]->(c)
            | RETURN id(a)""".stripMargin
        )
          .single()
          .get(0).asLong()

        // create a wrapper function
        WithResource(context.db.beginTx()) { _ =>
          // query the neighbours
          val result = (for {
            p <- context.db.getNodeById(nodeId).neighbours()
            n <- p.nodes().asScala
          } yield n.getProperty("name").toString).toSet
          result should contain("Alice")
          result should contain("Bob")
          result should contain("Carl")
          result should contain("David")
        }
      }
    }
  }

}
