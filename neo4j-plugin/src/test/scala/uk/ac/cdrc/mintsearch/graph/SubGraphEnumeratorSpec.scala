/**
 * Testing the SubGraphEnumerator
 */

package uk.ac.cdrc.mintsearch.graph

import org.neo4j.driver.v1._
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ServerControls, TestServerBuilder, TestServerBuilders}
import org.scalatest._
import uk.ac.cdrc.mintsearch.index.PropertyLabelMaker
import uk.ac.cdrc.mintsearch.neo4j.{GraphDBContext, WithResource}

import scala.collection.JavaConverters._

class SubGraphEnumeratorSpec extends fixture.WordSpec with Matchers {

  case class FixtureParam(neo4jServer: ServerControls) extends AutoCloseable {

    lazy val driver: Driver = GraphDatabase.driver(
      neo4jServer.boltURI(),
      Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)

    val context = new GraphDBContext
        with ExponentialPropagation
        with PropertyLabelMaker
        with NeighbourhoodByRadius
        with NeighbourAwareContext
        with SubGraphEnumeratorContext {

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

  "A SubGraphEnumerator" should {

    "find a connect component" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
        WithResource(driver.session()) { session =>
          // create a simple graph with two order of relationship friend
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

          WithResource(context.db.beginTx()) { _ =>
            val expanded = context.expandingSubGraph(Set(nodeA), Set(nodeA, nodeB, nodeC)).nodes map { _.getId }
            expanded should contain(nodeB)
            expanded should contain(nodeC)
          }
        }
      }
    }

    "find a large connect component" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
        WithResource(driver.session()) { session =>
          // create a simple graph with two order of relationship friend
          val res = session.run(
            """CREATE
              | (a: Person {name:'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (c: Person {name: 'Carl'}),
              | (d: Person {name: 'David'}),
              | (e: Person {name: 'Elizabeth'}),
              | (f: Person {name: 'Frank'}),
              | (g: Person {name: 'Grace'}),
              | (h: Person {name: 'Henry'}),
              | (a)-[:Friend]->(b)-[:Friend]->(c)-[:Friend]->(d)-[:Friend]->(e)-[:Friend]->(f)-[:Friend]->(g)-[:Friend]->(h)
              | RETURN id(a), id(b), id(c), id(d), id(e), id(f), id(g), id(h)""".stripMargin
          ).single()
          val nodes = for (i <- 0 to 7) yield res.get(i).asLong

          WithResource(context.db.beginTx()) { _ =>
            val expanded = context.expandingSubGraph(Set(nodes(0)), nodes.toSet).nodes map {
              _.getId
            }
            expanded should contain(nodes(6))
            expanded should contain(nodes(7))
          }
        }
      }
    }

    "find another large connect component" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
        WithResource(driver.session()) { session =>
          // create a simple graph with two order of relationship friend
          val res = session.run(
            """CREATE
              | (a: Person {name:'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (c: Person {name: 'Carl'}),
              | (d: Person {name: 'David'}),
              | (e: Person {name: 'Elizabeth'}),
              | (f: Person {name: 'Frank'}),
              | (g: Person {name: 'Grace'}),
              | (h: Person {name: 'Henry'}),
              | (a)-[:Friend]->(b)-[:Friend]->(c)-[:Friend]->(d)-[:Friend]->(e)-[:Friend]->(f)-[:Friend]->(g)-[:Friend]->(h)
              | RETURN id(a), id(b), id(c), id(d), id(e), id(f), id(g), id(h)""".stripMargin
          ).single()
          val nodes = for (i <- 0 to 7) yield res.get(i).asLong

          WithResource(context.db.beginTx()) { _ =>
            val expanded = context.expandingSubGraph(Set(nodes(0)), nodes.take(4).toSet).nodes map {
              _.getId
            }
            expanded should not contain nodes(6)
            expanded should not contain nodes(7)
          }
        }
      }
    }

    "assemble connect components" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
        WithResource(driver.session()) { session =>
          // create a simple graph with two order of relationship friend
          val res = session.run(
            """CREATE
              | (a: Person {name:'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (c: Person {name: 'Carl'}),
              | (d: Person {name: 'David'}),
              | (e: Person {name: 'Elizabeth'}),
              | (f: Person {name: 'Frank'}),
              | (g: Person {name: 'Grace'}),
              | (h: Person {name: 'Henry'}),
              | (a)-[:Friend]->(b)-[:Friend]->(c),
              | (d)-[:Friend]->(e)-[:Friend]->(f)-[:Friend]->(g)-[:Friend]->(h)
              | RETURN id(a), id(b), id(c), id(d), id(e), id(f), id(g), id(h)""".stripMargin
          ).single()
          val nodes = for (i <- 0 to 7) yield res.get(i).asLong

          WithResource(context.db.beginTx()) { _ =>
            val graphs = context.composeGraphs(nodes.toSet).toVector
            graphs(0).nodeIds should contain oneOf (nodes(0), nodes(3))
            graphs(0).nodeIds should contain oneOf (nodes(1), nodes(4))
            graphs(0).nodeIds should contain oneOf (nodes(2), nodes(5))
          }
        }
      }
    }
  }

  "GraphSnippet" should {
    "return a CypherResultSubGraph representing the sub graph store" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
        WithResource(driver.session()) { session =>

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
            import context.nodeWrapper
            // query the neighbours
            val nodes = (for {
              p <- context.db.getNodeById(nodeId).neighbours
              n <- p.nodes().asScala
            } yield n).toList

            val relationships = (for {
              p <- context.db.getNodeById(nodeId).neighbours
              r <- p.relationships().asScala
            } yield r).toList

            val sgs = GraphSnippet(nodes, relationships)
            val sgsNodeNames = sgs.nodes.map(_.getProperty("name")).toSet
            sgsNodeNames should be(Set("Alice", "Bob", "Carl"))
          }
        }
      }
    }
  }
}

