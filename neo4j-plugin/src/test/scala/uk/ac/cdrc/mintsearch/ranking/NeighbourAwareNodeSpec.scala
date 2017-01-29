/**
 * Testing NeighbourAwareNode
 */

package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.driver.v1._
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ ServerControls, TestServerBuilder, TestServerBuilders }
import org.scalatest._
import uk.ac.cdrc.mintsearch.index.PropertyLabelMaker
import uk.ac.cdrc.mintsearch.neighbourhood.{ ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius }
import uk.ac.cdrc.mintsearch.neo4j.{ GraphDBContext, WithResource }

class NeighbourAwareNodeSpec extends fixture.WordSpec with Matchers {

  case class FixtureParam(neo4jServer: ServerControls) extends AutoCloseable {

    lazy val driver: Driver = GraphDatabase.driver(neo4jServer.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)

    val context = new GraphDBContext with ExponentialPropagation with PropertyLabelMaker with NeighbourhoodByRadius with NeighbourAwareContext {

      override val radius: Int = 2
      override val propagationFactor: Double = 0.5

      override val labelStorePropKey: String = s"__nagg_$radius"
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

  "A neighbour aware node" should {

    "return paths to neighbours" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
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
              p <- context.db.getNodeById(nodeId).neighbours
              n = p.endNode()
            } yield n.getProperty("name").toString).toSet
            result should not contain "Alice"
            result should contain("Bob")
            result should contain("Carl")
          }
          WithResource(context.db.beginTx()) { _ =>
            // query the neighbours
            val result = (for {
              p <- context.db.getNodeById(nodeId).neighbourhood
              n = p.endNode()
            } yield n.getProperty("name").toString).toSet
            result should contain("Alice")
            result should contain("Bob")
            result should contain("Carl")
          }
        }
      }
    }

    "return many neighbours" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
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
          ).single().get(0).asLong()

          // create a wrapper function
          WithResource(context.db.beginTx()) { _ =>
            // query the neighbours
            val result = (for {
              p <- context.db.getNodeById(nodeId).neighbours
              n = p.endNode
            } yield n.getProperty("name").toString).toSet
            result should not contain "Alice"
            result should contain("Bob")
            result should contain("Carl")
            result should contain("David")
          }
        }
      }
    }

    "return all neighbours within 2 hops" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
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
          ).single().get(0).asLong()

          // create a wrapper function
          WithResource(context.db.beginTx()) { _ =>
            // query the neighbours
            val result = (for {
              p <- context.db.getNodeById(nodeId).neighbours
              n = p.endNode()
            } yield n.getProperty("name").toString).toSet
            result should not contain "Alice"
            result should contain("Bob")
            result should contain("Carl")
            result should contain("David")
          }
          // create a wrapper function
          WithResource(context.db.beginTx()) { _ =>
            // query the neighbours
            val result = (for {
              p <- context.db.getNodeById(nodeId).neighbourhood
              n = p.endNode()
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

}
