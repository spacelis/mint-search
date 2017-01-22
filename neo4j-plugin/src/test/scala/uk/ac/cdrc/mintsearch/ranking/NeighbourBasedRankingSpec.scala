package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.driver.v1.{ Config, Driver, GraphDatabase }
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ ServerControls, TestServerBuilder, TestServerBuilders }
import org.scalatest._
import uk.ac.cdrc.mintsearch._
import uk.ac.cdrc.mintsearch.neighbourhood.{ ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius }
import uk.ac.cdrc.mintsearch.neo4j.{ GraphContext, GraphSnippet, PropertyLabelMaker, WithResource }

import scala.collection.JavaConverters._

/**
 * Testing NeighbourAwareNode
 */

class NeighbourBasedRankingSpec extends WordSpec with Matchers {

  "WeightedLabelSet" should {

    "have cover-diff operation (non-negative substraction)" in {
      val a = Map("a" -> 1.0, "b" -> 2.0)
      val b = Map("a" -> 3.0)
      a ~ b should be(Map("a" -> 0.0, "b" -> 2.0))
      a ~~ b should be(Map("b" -> 2.0))
    }

    "have sum" in {
      val a = Map("a" -> 1.0, "b" -> 2.0)
      val b = Map("a" -> 3.0)
      val c = Map("c" -> 1.0)
      sum(Seq(a, b, c)) should be(Map("a" -> 4.0, "b" -> 2.0, "c" -> 1.0))
    }

  }

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

  "asCypherResultSubGraph" should {
    "return a CypherResultSubGraph representing the sub graph store" in new Neo4JFixture {

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
            p <- context.db.getNodeById(nodeId).neighbours()
            n <- p.nodes().asScala
          } yield n).toList

          val relationships = (for {
            p <- context.db.getNodeById(nodeId).neighbours()
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
