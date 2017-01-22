package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.graphdb.Label
import org.scalatest.{ Matchers, WordSpec }
import uk.ac.cdrc.mintsearch.neo4j.WithResource
import uk.ac.cdrc.mintsearch.search.SimpleGraphQueryBuilder

import scala.collection.JavaConverters._

/**
 * Created by ucfawli on 14-Jan-17.
 */
class GraphSearchQuerySpec extends WordSpec with Matchers {

  "A GraphQuery from Cypher" should {

    "handle single node" in new SimpleGraphQueryBuilder {
      WithResource(fromCypherCreate("create (n: Person {name: 'James'})")) { gq =>
        WithResource(gq.qdb.beginTx()) { _ =>
          gq.qdb.findNodes(Label.label("Person")).asScala.take(1).toList(0).getProperty("name") shouldBe "James"
        }
      }
    }

    "handle two nodes" in new SimpleGraphQueryBuilder {
      WithResource(fromCypherCreate("create (n: Person {name: 'James'}), (m: Person {name: 'Mary'})")) { gq =>
        WithResource(gq.qdb.beginTx()) { _ =>
          gq.qdb.findNodes(Label.label("Person")).asScala.take(2).toList map { _.getProperty("name") } shouldBe List("James", "Mary")
        }
      }
    }

  }
}
