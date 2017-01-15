package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.driver.v1.{Config, Driver, GraphDatabase}
import org.neo4j.graphdb.{GraphDatabaseService, Label}
import org.neo4j.harness.{ServerControls, TestServerBuilder, TestServerBuilders}
import org.scalatest.{Matchers, WordSpec}
import uk.ac.cdrc.mintsearch.index.NeighbourAggregatedIndexManager
import uk.ac.cdrc.mintsearch.neighbourhood.{ExponentialPropagation, NeighbourAware, NeighbourhoodByRadius}
import uk.ac.cdrc.mintsearch.neo4j.{Neo4JContainer, PropertyLabelMaker, WithResource}
import scala.collection.JavaConverters._

/**
  * Created by ucfawli on 14-Jan-17.
  */
class GraphSearchQuerySpec extends WordSpec with Matchers{

  "A GraphQuery from Cypher" should {

    "handle single node" in  {
      WithResource(GraphSearchQuery.fromCypherCreate("create (n: Person {name: \"James\"})")) { gq =>
        WithResource(gq.qdb.beginTx()) { _ =>
          gq.qdb.findNodes(Label.label("Person")).asScala.take(1).toList(0).getProperty("name") shouldBe "James"
        }
      }
    }

    "handle two nodes" in  {
      WithResource(GraphSearchQuery.fromCypherCreate("create (n: Person {name: \"James\"}), (m: Person {name: \"Mary\"})")) { gq =>
        WithResource(gq.qdb.beginTx()) { _ =>
          gq.qdb.findNodes(Label.label("Person")).asScala.take(2).toList map {_.getProperty("name")} shouldBe List("James", "Mary")
        }
      }
    }

  }
}
