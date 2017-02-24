/**
  * Testing the SubGraphEnumeratorContext
  */

package uk.ac.cdrc.mintsearch.graph

import org.neo4j.driver.v1._
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ServerControls, TestServerBuilder, TestServerBuilders}
import org.scalatest._
import uk.ac.cdrc.mintsearch.NodeMatchingSet
import uk.ac.cdrc.mintsearch.index.PropertyLabelMaker
import uk.ac.cdrc.mintsearch.neo4j.{GraphDBContext, WithResource}
import uk.ac.cdrc.mintsearch.ranking.NessNodeSimilarity

class NessEmbeddingEnumSpec extends fixture.WordSpec with Matchers {

  case class FixtureParam(neo4jServer: ServerControls) extends AutoCloseable {

    lazy val driver: Driver = GraphDatabase.driver(
      neo4jServer.boltURI(),
      Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig
    )

    val context = new GraphDBContext
      with ExponentialPropagation
      with PropertyLabelMaker
      with NeighbourhoodByRadius
      with NeighbourAwareContext
      with NessNodeSimilarity
      with NessEmbeddingEnumContext {

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
            val embeddings = context.composeEmbeddings(NodeMatchingSet(Map(-1L -> IndexedSeq(nodeA, nodeB, nodeC).map((_, 1d))))).toList
            embeddings should have length 3
            val firstNodes = embeddings.head.nodes map {
              _.getId
            }
            firstNodes should contain(nodeA)
            firstNodes should contain(nodeB)
            firstNodes should contain(nodeC)
            embeddings flatMap (_.projection.keySet) should contain theSameElementsAs Set(nodeA, nodeB, nodeC)
          }
        }
      }
    }

    "find three connect component" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
        WithResource(driver.session()) { session =>
          // create a simple graph with two order of relationship friend
          val res = session.run(
            """CREATE
              | (a: Person {name:'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (c: Person {name: 'Carl'})
              | RETURN id(a), id(b), id(c)""".stripMargin
          )
            .single()
          val Seq(nodeA, nodeB, nodeC) = for (i <- 0 to 2) yield res.get(i).asLong

          WithResource(context.db.beginTx()) { _ =>
            val embeddings = context.composeEmbeddings(NodeMatchingSet(Map(-1L -> IndexedSeq(nodeA, nodeB, nodeC).map((_, 1d))))).toList
            embeddings should have length 3
            val firstNodes = embeddings.head.nodes map {
              _.getId
            }
            firstNodes should contain(nodeA)
            firstNodes should not contain nodeB
            firstNodes should not contain nodeC
            embeddings flatMap (_.projection.keySet) should contain theSameElementsAs Set(nodeA, nodeB, nodeC)
          }
        }
      }
    }

    "find only a part of large connect component" in { f =>
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
            val expanded = context.composeEmbeddings(NodeMatchingSet(Map(-1L -> Seq(nodes(0), nodes(7), nodes(4)).map((_, 1d))))).toList
            expanded should have length 3

            val first = expanded.head
            first.nodeIds should contain theSameElementsAs nodes.slice(0, 3)
            first.projection.keySet should be(Set(nodes(0)))

            val second = expanded(1)
            second.nodeIds should contain theSameElementsAs nodes.slice(5, nodes.length)
            second.projection.keySet should be(Set(nodes(7)))

            val third = expanded(2)
            third.nodeIds should contain theSameElementsAs nodes.slice(2, 7)
            third.projection.keySet should be(Set(nodes(4)))
          }
        }
      }
    }

    "not expand via nodes in the same rank list" in { f =>
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
            val expanded = context.composeEmbeddings(NodeMatchingSet(Map(-1L -> Seq(nodes(0), nodes(1)).map((_, 1d))))).toList
            expanded should have length 2

            val first = expanded.head
            first.nodeIds should contain theSameElementsAs nodes.slice(0, 3)
            first.projection.keySet should be(Set(nodes(0)))

            val second = expanded(1)
            second.nodeIds should contain theSameElementsAs nodes.slice(0, 4)
            second.projection.keySet should be(Set(nodes(1)))
          }
        }
      }
    }

    "find all key nodes" in { f =>
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
            val expanded = context.composeEmbeddings(NodeMatchingSet(Map(-1L -> Seq(nodes(0) -> 1d), -2L -> Seq(nodes(2) -> 1d)))).toList
            expanded should have length 1
            val expandedNodes = expanded.head.nodeIds.toSet
            expandedNodes should contain theSameElementsAs Set(nodes(0), nodes(1), nodes(2))
            expanded.head.projection.keySet should contain theSameElementsAs Set(nodes(0), nodes(2))
          }
        }
      }
    }

    "not cross connected component boundary" in { f =>
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
            val expanded = context.composeEmbeddings(NodeMatchingSet(Map(-1L -> Seq(nodes(0) -> 1d), -2L -> Seq(nodes(3) -> 1d)))).toList
            expanded should have length 2
            val expandedNodes = expanded map {
              _.nodeIds.toSet
            }
            expandedNodes should contain theSameElementsAs Set(Set(nodes(0), nodes(1), nodes(2)), Set(nodes(3), nodes(4), nodes(5)))
          }
        }
      }
    }
  }
}
