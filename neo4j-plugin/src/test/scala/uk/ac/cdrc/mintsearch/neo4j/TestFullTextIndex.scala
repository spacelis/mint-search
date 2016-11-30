package uk.ac.cdrc.mintsearch.neo4j

import org.scalatest._
import org.neo4j.driver.v1._
import org.neo4j.harness.TestServerBuilders

/**
 * Created by ucfawli on 29-Nov-16.
 */
class TestFullTextIndexSpec extends FlatSpec {

  trait Neo4JFixture {
    private val _builder = TestServerBuilders.newInProcessBuilder()
    def builder = _builder
    lazy val neo4jServer = builder.newServer()
  }

  trait Neo4JFixtureFullTextSearchProcedure extends Neo4JFixture {
    override val builder = super.builder.withProcedure(classOf[FullTextIndex])
  }

  "A test" should "run" in new Neo4JFixtureFullTextSearchProcedure {
    val driver: Driver = GraphDatabase.driver(neo4jServer.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig)
    // Given I've started Neo4j with the FullTextIndex procedure class
    //       which my 'neo4j' rule above does.
    val session = driver.session()

    // And given I have a node in the database
    val nodeId: Long = session.run("CREATE (p:User {name:'Brookreson'}) RETURN id(p)")
      .single()
      .get(0).asLong()

    // When I use the index procedure to index a node
    session.run(s"CALL mint.fulltext_index($nodeId, ['name'])")

    // Then I can search for that node with lucene query syntax
    val result = session.run("CALL mint.fulltext_search('User', 'name:Brook*')")
    assert(result.single().get("nodeId").asLong() === nodeId)
  }

}
