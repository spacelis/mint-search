package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.driver.v1._
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.graphdb.{GraphDatabaseService, Node}
import org.neo4j.harness.{ServerControls, TestServerBuilder, TestServerBuilders}
import org.scalatest._
import uk.ac.cdrc.mintsearch.neo4j.WithResource
import uk.ac.cdrc.mintsearch.ranking.NeighbourAwareNode._
import uk.ac.cdrc.mintsearch.ranking.NeighbourBasedRanking._

import scala.collection.JavaConverters._

/**
  * Testing the SubGraphEnumerator
  */

class SubGraphEnumeratorSpec extends WordSpec with Matchers{

  trait Neo4JFixture {
    private val _builder = TestServerBuilders.newInProcessBuilder()
    def builder: TestServerBuilder = _builder
    lazy val neo4jServer: ServerControls = builder.newServer()
    lazy val driver: Driver = GraphDatabase.driver(neo4jServer.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)
    implicit lazy val gdb: GraphDatabaseService = neo4jServer.graph()
    implicit lazy val ntd: TraversalDescription = neighbourhoodTraversalDescription(2, Seq("Friend"))
    implicit lazy val nodeWrapper: (Node) => NeighbourAwareNode = wrapNode
  }

  "A SubGraphEnumerator" should {

    "find a connect component" in new Neo4JFixture {
      WithResource(driver.session()) { session =>
        // create a simple graph with two order of relationship friend
        val res = session.run(
          """CREATE
            | (a: Person {name:'Alice'}),
            | (b: Person {name: 'Bob'}),
            | (c: Person {name: 'Carl'}),
            | (a)-[:Friend]->(b)-[:Friend]->(c)
            | RETURN id(a), id(b), id(c)""".stripMargin)
          .single()
        val Seq(nodeA, nodeB, nodeC) = for (i <- 0 to 2) yield res.get(i).asLong


        WithResource(gdb.beginTx()) { _ =>
          val sge = SubGraphEnumerator(ntd, gdb)
          val expanded = sge.expandingSubGraph(Set(nodeA), Set(nodeA, nodeB, nodeC) ).nodes map {_.getId}
          expanded should contain (nodeB)
          expanded should contain (nodeC)
        }
      }
    }

    "find a large connect component" in new Neo4JFixture {
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
            | RETURN id(a), id(b), id(c), id(d), id(e), id(f), id(g), id(h)""".stripMargin)
          .single()
        val nodes = for (i <- 0 to 7) yield res.get(i).asLong


        WithResource(gdb.beginTx()) { _ =>
          val sge = SubGraphEnumerator(ntd, gdb)
          val expanded = sge.expandingSubGraph(Set(nodes(0)), nodes.toSet ).nodes map {_.getId}
          expanded should contain (nodes(6))
          expanded should contain (nodes(7))
        }
      }
    }

    "find another large connect component" in new Neo4JFixture {
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
            | RETURN id(a), id(b), id(c), id(d), id(e), id(f), id(g), id(h)""".stripMargin)
          .single()
        val nodes = for (i <- 0 to 7) yield res.get(i).asLong


        WithResource(gdb.beginTx()) { _ =>
          val sge = SubGraphEnumerator(ntd, gdb)
          val expanded = sge.expandingSubGraph(Set(nodes(0)), nodes.take(4).toSet ).nodes map {_.getId}
          expanded should not contain nodes(6)
          expanded should not contain nodes(7)
        }
      }
    }
//    "return many neighbours" in new Neo4JFixture {
//      WithResource(driver.session()) { session =>
//        // create a simple graph with two order of relationship friend
//        val nodeId: Long = session.run(
//          """CREATE
//            | (a: Person {name: 'Alice'}),
//            | (b: Person {name: 'Bob'}),
//            | (c: Person {name: 'Carl'}),
//            | (d: Person {name: 'David'}),
//            | (e: Person {name: 'Elizabeth'}),
//            | (a)-[:Friend]->(b)-[:Friend]->(c),
//            | (a)-[:Friend]->(d)-[:Friend]->(e)
//            | RETURN id(a)""".stripMargin)
//          .single()
//          .get(0).asLong()
//
//        // create a wrapper function
//        WithResource(gdb.beginTx()) { _ =>
//          // query the neighbours
//          val result = (for {
//            p <- gdb.getNodeById(nodeId).neighbours()
//            n <- p.nodes().asScala
//          } yield n.getProperty("name").toString).toSet
//          result should contain("Alice")
//          result should contain("Bob")
//          result should contain("Carl")
//          result should contain("David")
//        }
//      }
//    }
//
//    "return all neighbours within 2 hops" in new Neo4JFixture {
//      WithResource(driver.session()) { session =>
//        // create a simple graph with two order of relationship friend
//        val nodeId: Long = session.run(
//          """CREATE
//            | (a: Person {name: 'Alice'}),
//            | (b: Person {name: 'Bob'}),
//            | (c: Person {name: 'Carl'}),
//            | (d: Person {name: 'David'}),
//            | (a)-[:Friend]->(b)-[:Friend]->(c),
//            | (a)-[:Friend]->(d)-[:Friend]->(c)
//            | RETURN id(a)""".stripMargin)
//          .single()
//          .get(0).asLong()
//
//        // create a wrapper function
//        WithResource(gdb.beginTx()) { _ =>
//          // query the neighbours
//          val result = (for {
//            p <- gdb.getNodeById(nodeId).neighbours()
//            n <- p.nodes().asScala
//          } yield n.getProperty("name").toString).toSet
//          result should contain("Alice")
//          result should contain("Bob")
//          result should contain("Carl")
//          result should contain("David")
//        }
//      }
//    }
  }
}

