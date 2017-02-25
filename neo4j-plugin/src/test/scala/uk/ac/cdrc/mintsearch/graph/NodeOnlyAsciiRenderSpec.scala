package uk.ac.cdrc.mintsearch.graph

import org.neo4j.driver.v1.{Config, Driver, GraphDatabase}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.{ServerControls, TestServerBuilder, TestServerBuilders}
import org.scalatest.{Matchers, Outcome, fixture}
import uk.ac.cdrc.mintsearch.neo4j.{GraphDBContext, WithResource}

/**
  * Test NodeOnlyAsciiGraph
  */
class NodeOnlyAsciiRenderSpec extends fixture.WordSpec with Matchers {

  case class FixtureParam(neo4jServer: ServerControls) extends AutoCloseable {

    lazy val driver: Driver = GraphDatabase.driver(
      neo4jServer.boltURI(),
      Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig
    )

    val context = new GraphDBContext {
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

  "NodeOnlyAsciiGraph" should {
    "render node" in { f =>
      import f._
      val r = NodeOnlyAsciiRenderer(Seq("value"))
      import r._
      WithResource(context.db.beginTx()){_ =>
        val n = context.db.createNode()
        n.setProperty("value", "Alice")
        n.render should be ("(0: Alice)")
      }
    }

    "render embedding" in { f =>
      import f._
      val r = NodeOnlyAsciiRenderer(Seq("value"))
      import r._
      WithResource(context.db.beginTx()){_ =>
        val n = context.db.createNode()
        val m = context.db.createNode()
        n.setProperty("value", "Alice")
        m.setProperty("value", "Bob")
        val s = GraphEmbedding(List(n, m), List.empty, Map(n.getId -> (-1l, 1.0d))).render
        s should include (n.render)
        s should include (m.render)
        s should startWith (s"# ${n.render}->V-1 [1.0]")
      }
    }
  }
}
