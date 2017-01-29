/**
  * Testing NeighbourAwareNode
  */

package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.driver.v1.{Config, Driver, GraphDatabase}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ServerControls, TestServerBuilder, TestServerBuilders}
import org.scalatest._
import uk.ac.cdrc.mintsearch.graph.SubGraphEnumeratorContext
import uk.ac.cdrc.mintsearch.index.{NeighbourNodeIndexReader, NeighbourNodeIndexWriter, PropertyLabelMaker}
import uk.ac.cdrc.mintsearch.neighbourhood.{ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius}
import uk.ac.cdrc.mintsearch.neo4j.{GraphDBContext, WithResource}
import uk.ac.cdrc.mintsearch.search.{NeighbourAggregatedAnalyzer, SimpleGraphQueryBuilder}

class NeighbourhoodRankingSpec extends fixture.WordSpec with Matchers {

  case class FixtureParam(neo4jServer: ServerControls) extends AutoCloseable {
    val driver: Driver = GraphDatabase.driver(neo4jServer.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)
    val indexWriter = new NeighbourNodeIndexWriter with ExponentialPropagation with PropertyLabelMaker with NeighbourhoodByRadius with NeighbourAwareContext {

      override val radius: Int = 2
      override val propagationFactor: Double = 0.5

      override val labelStorePropKey: String = s"__nagg_$radius"
      override val db: GraphDatabaseService = neo4jServer.graph()
      override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
    }

    val graphSearcher = new NeighbourhoodRanking
      with NeighbourNodeIndexReader
      with GraphDBContext
      with ExponentialPropagation
      with PropertyLabelMaker
      with NeighbourhoodByRadius
      with NeighbourAwareContext
      with NeighbourAggregatedAnalyzer
      with SimpleNeighbourSimilarity
      with SimpleNodeRanking
      with SubGraphEnumeratorContext
      with SimpleGraphQueryBuilder {

      override val radius: Int = 2
      override val propagationFactor: Double = 0.5

      override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
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

  "NeighbourhoodRanking" should {
    "find the only sub graph" in { f =>
      import f._
      WithResource(driver.session()) { session =>
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
        WithResource(indexWriter.db.beginTx()) { tx =>
          // query the neighbours
          indexWriter.index(indexWriter.db.getNodeById(nodeA))
          indexWriter.index(indexWriter.db.getNodeById(nodeB))
          indexWriter.index(indexWriter.db.getNodeById(nodeC))
          indexWriter.awaitForIndexReady()
          val q = graphSearcher.fromCypherCreate(
            """CREATE
              | (a: Person {name: 'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (a)-[:Friend]->(b)
            """.stripMargin)
          val top = graphSearcher.rankEmbeddings(q).toList.head
          top.nodeIds.toSet should be (Set(nodeA, nodeB, nodeC))
          tx.success()
        }
      }
    }
    "find a single sub graph" in { f =>
      import f._
      WithResource(driver.session()) { session =>
        val res = session.run(
          """CREATE
            | (a: Person {name:'Alice'}),
            | (b: Person {name: 'Bob'}),
            | (c: Person {name: 'Carl'}),
            | (x: Person {name:'Xman'}),
            | (y: Person {name: 'Yuri'}),
            | (z: Person {name: 'Zebra'}),
            | (a)-[:Friend]->(b)-[:Friend]->(c),
            | (x)-[:Friend]->(y)-[:Friend]->(z)
            | RETURN id(a), id(b), id(c)""".stripMargin
        )
          .single()
        val Seq(nodeA, nodeB, nodeC) = for (i <- 0 to 2) yield res.get(i).asLong
        WithResource(indexWriter.db.beginTx()) { tx =>
          // query the neighbours
          indexWriter.index(indexWriter.db.getNodeById(nodeA))
          indexWriter.index(indexWriter.db.getNodeById(nodeB))
          indexWriter.index(indexWriter.db.getNodeById(nodeC))
          indexWriter.awaitForIndexReady()
          val q = graphSearcher.fromCypherCreate(
            """CREATE
              | (a: Person {name: 'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (a)-[:Friend]->(b)
            """.stripMargin)
          val res = graphSearcher.rankEmbeddings(q).toList
          res should have length 1
          res.head.nodeIds.toSet should be (Set(nodeA, nodeB, nodeC))
          tx.success()
        }
      }
    }
    "find two sub graphs" in { f =>
      import f._
      WithResource(driver.session()) { session =>
        val res = session.run(
          """CREATE
            | (a: Person {name:'Alice'}),
            | (b: Person {name: 'Bob'}),
            | (c: Person {name: 'Carl'}),
            | (x: Person {name:'David'}),
            | (y: Person {name: 'Alice'}),
            | (z: Person {name: 'Bob'}),
            | (a)-[:Friend]->(b)-[:Friend]->(c),
            | (x)-[:Friend]->(y)-[:Friend]->(z)
            | RETURN id(a), id(b), id(c), id(x), id(y), id(z)""".stripMargin
        )
          .single()
        val Seq(nodeA, nodeB, nodeC, nodeX, nodeY, nodeZ) = for (i <- 0 until 6) yield res.get(i).asLong
        WithResource(indexWriter.db.beginTx()) { tx =>
          // query the neighbours
          indexWriter.index(indexWriter.db.getNodeById(nodeA))
          indexWriter.index(indexWriter.db.getNodeById(nodeB))
          indexWriter.index(indexWriter.db.getNodeById(nodeC))
          indexWriter.index(indexWriter.db.getNodeById(nodeX))
          indexWriter.index(indexWriter.db.getNodeById(nodeY))
          indexWriter.index(indexWriter.db.getNodeById(nodeZ))
          indexWriter.awaitForIndexReady()
          val q = graphSearcher.fromCypherCreate(
            """CREATE
              | (a: Person {name: 'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (a)-[:Friend]->(b)
            """.stripMargin)
          val res = graphSearcher.rankEmbeddings(q).toList
          res should have length 2
          val resNodeSets = res.map(_.nodeIds.toSet)
          resNodeSets should contain (Set(nodeA, nodeB, nodeC))
          resNodeSets should contain (Set(nodeX, nodeY, nodeZ))
          tx.success()
        }
      }
    }

    "find two sub graphs after batch indexing" in { f =>
      import f._
      WithResource(driver.session()) { session =>
        val res = session.run(
          """CREATE
            | (a: Person {name:'Alice'}),
            | (b: Person {name: 'Bob'}),
            | (c: Person {name: 'Carl'}),
            | (x: Person {name:'David'}),
            | (y: Person {name: 'Alice'}),
            | (z: Person {name: 'Bob'}),
            | (a)-[:Friend]->(b)-[:Friend]->(c),
            | (x)-[:Friend]->(y)-[:Friend]->(z)
            | RETURN id(a), id(b), id(c), id(x), id(y), id(z)""".stripMargin
        )
          .single()
        val Seq(nodeA, nodeB, nodeC, nodeX, nodeY, nodeZ) = for (i <- 0 until 6) yield res.get(i).asLong
        WithResource(indexWriter.db.beginTx()) { tx =>
          // query the neighbours
          indexWriter.index()
          indexWriter.awaitForIndexReady()
          val q = graphSearcher.fromCypherCreate(
            """CREATE
              | (a: Person {name: 'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (a)-[:Friend]->(b)
            """.stripMargin)
          val res = graphSearcher.rankEmbeddings(q).toList
          res should have length 2
          val resNodeSets = res.map(_.nodeIds.toSet)
          resNodeSets should contain (Set(nodeA, nodeB, nodeC))
          resNodeSets should contain (Set(nodeX, nodeY, nodeZ))
          tx.success()
        }
      }
    }
  }

}
