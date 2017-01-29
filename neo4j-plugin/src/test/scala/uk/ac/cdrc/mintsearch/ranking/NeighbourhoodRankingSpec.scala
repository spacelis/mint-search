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
      override val indexName: String = "ness_index"
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
    "find a single sub graph" in { f =>
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
  }

}
