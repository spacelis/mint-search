/**
  * Interface for ranking graph based on a graph query
  */
package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.cypher.export.SubGraph
import org.neo4j.graphdb.GraphDatabaseService
import uk.ac.cdrc.mintsearch.search.GraphSearchQuery

trait GraphRanking {
  def search(gsq: GraphSearchQuery): Iterator[SubGraph]
}

object GraphRanking {
}
