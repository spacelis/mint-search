package uk.ac.cdrc.mintsearch.neo4j

import org.scalatest._
import collection.JavaConverters._
import org.neo4j.driver.v1._
import org.neo4j.graphdb.{GraphDatabaseService, Node}
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.harness.{ServerControls, TestServerBuilder, TestServerBuilders}
import uk.ac.cdrc.mintsearch.ranking.NeighbourAwareNode
import uk.ac.cdrc.mintsearch.ranking.NeighbourBasedRanking._
import uk.ac.cdrc.mintsearch.ranking.NeighbourAwareNode._

/**
 * Testing the fulltext_index/search procedure
 */

class NeighbourAwareNodeSpec extends WordSpec with Matchers{

  trait Neo4JFixture {
    private val _builder = TestServerBuilders.newInProcessBuilder()
    def builder: TestServerBuilder = _builder
    lazy val neo4jServer: ServerControls = builder.newServer()
    lazy val driver: Driver = GraphDatabase.driver(neo4jServer.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)
    implicit lazy val gdb: GraphDatabaseService = neo4jServer.graph()
    implicit lazy val ntd: TraversalDescription = neighbourhoodTraversalDescription(2, Seq("Friend"))
    implicit lazy val nodeWrapper: (Node) => NeighbourAwareNode = wrapNode
  }

  "A neighbour aware node" should {

    "return paths to neighbours" in new Neo4JFixture {
      WithResource(driver.session()) { session =>
        // create a simple graph with two order of relationship friend
        val nodeId: Long = session.run(
          """CREATE
            | (a: Person {name:'Alice'}),
            | (b: Person {name: 'Bob'}),
            | (c: Person {name: 'Carl'}),
            | (a)-[:Friend]->(b)-[:Friend]->(c)
            | RETURN id(a)""".stripMargin)
          .single()
          .get(0).asLong()

        // create a wrapper function
        WithResource(gdb.beginTx()) { _ =>
          // query the neighbours
          val result = (for {
            p <- gdb.getNodeById(nodeId).neighbours()
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
            | RETURN id(a)""".stripMargin)
          .single()
          .get(0).asLong()

        // create a wrapper function
        WithResource(gdb.beginTx()) { _ =>
          // query the neighbours
          val result = (for {
            p <- gdb.getNodeById(nodeId).neighbours()
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
        // create a simple graph with two order of relationship friend
        val nodeId: Long = session.run(
          """CREATE
            | (a: Person {name: 'Alice'}),
            | (b: Person {name: 'Bob'}),
            | (c: Person {name: 'Carl'}),
            | (d: Person {name: 'David'}),
            | (a)-[:Friend]->(b)-[:Friend]->(c),
            | (a)-[:Friend]->(d)-[:Friend]->(c)
            | RETURN id(a)""".stripMargin)
          .single()
          .get(0).asLong()

        // create a wrapper function
        WithResource(gdb.beginTx()) { _ =>
          // query the neighbours
          val result = (for {
            p <- gdb.getNodeById(nodeId).neighbours()
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
