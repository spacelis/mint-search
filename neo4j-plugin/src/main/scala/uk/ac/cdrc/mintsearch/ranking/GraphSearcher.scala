/**
 * Interface for ranking graph based on a graph query
 */
package uk.ac.cdrc.mintsearch.ranking

import org.neo4j.cypher.export.SubGraph
import uk.ac.cdrc.mintsearch.search.GraphSearchQuery

trait GraphSearcher {
  def search(gsq: GraphSearchQuery): Iterator[SubGraph]
}

object GraphSearcher {
}
