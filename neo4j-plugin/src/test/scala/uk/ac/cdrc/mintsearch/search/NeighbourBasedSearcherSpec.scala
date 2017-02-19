/**
  * Testing NeighbourAwareNode
  */

package uk.ac.cdrc.mintsearch.search

import org.apache.commons.io.FileUtils
import org.neo4j.harness.{ServerControls, TestServerBuilder, TestServerBuilders}
import org.scalatest._
import uk.ac.cdrc.mintsearch.neo4j.WithResource


trait NeighbourBasedSearcherSpec extends fixture.WordSpec with Matchers {

  override type FixtureParam <: ServiceStubForTest

  "NeighbourhoodRanking" should {
    "find the only sub graph" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
        WithResource(driver.session()) { session =>
          val res = session.run(
            """CREATE
              | (a: Person {name:'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (c: Person {name: 'Carl'}),
              | (a)-[:Friend]->(b)-[:Friend]->(c)
              | RETURN id(a), id(b), id(c)""".stripMargin
          ).single()
          val Seq(nodeA, nodeB, nodeC) = for (i <- 0 to 2) yield res.get(i).asLong
          WithResource(indexWriter.db.beginTx()) { tx =>
            // query the neighbours
            indexWriter.index()
            indexWriter.awaitForIndexReady()
            WithResource(graphSearcher.fromCypherCreate(
              """CREATE
                | (a: Person {name: 'Alice'}),
                | (b: Person {name: 'Bob'}),
                | (a)-[:Friend]->(b)
              """.stripMargin
            )) { q =>
//              val lex = graphSearcher.asInstanceOf[TerrierIndexReader].indexDB.getLexicon
//              println(lex.getLexiconEntry(0).getKey)
              val top = graphSearcher.search(q).graphSnippets.head
              top.nodeIds.toSet should be(Set(nodeA, nodeB, nodeC))
              tx.success()
            }
          }
        }
      }
    }

    "be tolerant to spaces in the property values" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
        WithResource(driver.session()) { session =>
          val res = session.run(
            """CREATE
              | (a: Person {name:'Alice'}),
              | (b: Person {name: 'Bob Luke'}),
              | (c: Person {name: 'Carl'}),
              | (a)-[:Friend]->(b)-[:Friend]->(c)
              | RETURN id(a), id(b), id(c)""".stripMargin
          ).single()
          val Seq(nodeA, nodeB, nodeC) = for (i <- 0 to 2) yield res.get(i).asLong
          WithResource(indexWriter.db.beginTx()) { tx =>
            // query the neighbours
            indexWriter.index()
            indexWriter.awaitForIndexReady()
            WithResource(graphSearcher.fromCypherCreate(
              """CREATE
                | (a: Person {name: 'Alice'}),
                | (b: Person {name: 'Bob Luke'}),
                | (a)-[:Friend]->(b)
              """.stripMargin
            )) { q =>
              val top = graphSearcher.search(q).graphSnippets.head
              top.nodeIds.toSet should be(Set(nodeA, nodeB, nodeC))
              tx.success()
            }
          }
        }
      }
    }

    "find a single sub graph" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
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
          ).single()
          val Seq(nodeA, nodeB, nodeC) = for (i <- 0 to 2) yield res.get(i).asLong
          WithResource(indexWriter.db.beginTx()) { tx =>
            // query the neighbours
            indexWriter.index()
            indexWriter.awaitForIndexReady()
            WithResource(graphSearcher.fromCypherCreate(
              """CREATE
                | (a: Person {name: 'Alice'}),
                | (b: Person {name: 'Bob'}),
                | (a)-[:Friend]->(b)
              """.stripMargin
            )) { q =>
              val res = graphSearcher.search(q).graphSnippets
              res should have length 1
              res.head.nodeIds.toSet should be(Set(nodeA, nodeB, nodeC))
            }
            tx.success()
          }
        }
      }
    }
    "find two sub graphs" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
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
          ).single()
          val Seq(nodeA, nodeB, nodeC, nodeX, nodeY, nodeZ) = for (i <- 0 until 6) yield res.get(i).asLong
          WithResource(indexWriter.db.beginTx()) { tx =>
            // query the neighbours
            indexWriter.index()
            indexWriter.awaitForIndexReady()
            WithResource(graphSearcher.fromCypherCreate(
              """CREATE
                | (a: Person {name: 'Alice'}),
                | (b: Person {name: 'Bob'}),
                | (a)-[:Friend]->(b)
              """.stripMargin
            )) { q =>
              val res = graphSearcher.search(q).graphSnippets
              res should have length 2
              val resNodeSets = res.map(_.nodeIds.toSet)
              resNodeSets should contain(Set(nodeA, nodeB, nodeC))
              resNodeSets should contain(Set(nodeX, nodeY, nodeZ))
            }
            tx.success()
          }
        }
      }
    }

    "find two sub graphs after batch indexing" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
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
          ).single()
          val Seq(nodeA, nodeB, nodeC, nodeX, nodeY, nodeZ) = for (i <- 0 until 6) yield res.get(i).asLong
          WithResource(indexWriter.db.beginTx()) { tx =>
            // query the neighbours
            indexWriter.index()
            indexWriter.awaitForIndexReady()
            WithResource(graphSearcher.fromCypherCreate(
              """CREATE
              | (a: Person {name: 'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (a)-[:Friend]->(b)
            """.stripMargin
            )) { q =>
              val res = graphSearcher.search(q).graphSnippets
              res should have length 2
              val resNodeSets = res.map(_.nodeIds.toSet)
              resNodeSets should contain(Set(nodeA, nodeB, nodeC))
              resNodeSets should contain(Set(nodeX, nodeY, nodeZ))
            }
            tx.success()
          }
        }
      }
    }
    "find incomplete matching sub graphs" in { f =>
      import f._
      WithResource(neo4jServer) { _ =>
        WithResource(driver.session()) { session =>
          val res = session.run(
            """CREATE
              | (x: Person {name:'David'}),
              | (y: Person {name: 'Alice'}),
              | (a: Person {name:'Alice'}),
              | (b: Person {name: 'Bob'}),
              | (c: Person {name: 'Carl'}),
              | (x)-[:Friend]->(y),
              | (a)-[:Friend]->(b)-[:Friend]->(c)
              | RETURN id(a), id(b), id(c), id(x), id(y)""".stripMargin
          ).single()
          val Seq(nodeA, nodeB, nodeC, nodeX, nodeY) = for (i <- 0 until 5) yield res.get(i).asLong
          WithResource(indexWriter.db.beginTx()) { tx =>
            // query the neighbours
            indexWriter.index()
            indexWriter.awaitForIndexReady()
            WithResource(graphSearcher.fromCypherCreate(
              """CREATE
                | (a: Person {name: 'Alice'}),
                | (b: Person {name: 'Bob'}),
                | (a)-[:Friend]->(b)
              """.stripMargin
            )) { q =>
              val res = graphSearcher.search(q).graphSnippets
              res should have length 2
              val resNodeSets = res.map(_.nodeIds.toSet)
              resNodeSets.head should be(Set(nodeA, nodeB, nodeC))
              resNodeSets should contain(Set(nodeX, nodeY))
            }
            tx.success()
          }
        }
      }
    }
  }

}

class LegacySearcherSpec extends NeighbourBasedSearcherSpec {

  case class FixtureParam(neo4jServer: ServerControls) extends ServiceStubLegacyNeo4J

  override def withFixture(test: OneArgTest): Outcome = {
    val builder: TestServerBuilder = TestServerBuilders.newInProcessBuilder()
    WithResource(FixtureParam(builder.newServer())) { f =>
      withFixture(test.toNoArgTest(f))
    }
  }

}

class TerrierSearcherSpec extends NeighbourBasedSearcherSpec {

  case class FixtureParam(neo4jServer: ServerControls) extends ServiceStubTerrier

  override def withFixture(test: OneArgTest): Outcome = {
    val builder: TestServerBuilder = TestServerBuilders.newInProcessBuilder()
    WithResource(FixtureParam(builder.newServer())) { f =>
      val r =withFixture(test.toNoArgTest(f))
      f.graphSearcher.indexDB.close()
      f.graphSearcher.path.listFiles().foreach(FileUtils.forceDelete(_))
      r
    }
  }

}
