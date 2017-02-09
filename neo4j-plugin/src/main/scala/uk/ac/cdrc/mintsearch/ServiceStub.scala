package uk.ac.cdrc.mintsearch

import org.neo4j.graphdb.GraphDatabaseService
import uk.ac.cdrc.mintsearch.index.BaseIndexWriter
import uk.ac.cdrc.mintsearch.search.{NeighbourBasedSearcher, SimpleQueryBuilder}

/**
  * This is a service holder trait.
  * It is for avoiding the non-static/final fields in Neo4J procedure definition.
  */
trait ServiceStub {

  protected var graphSearcher: NeighbourBasedSearcher with SimpleQueryBuilder = _
  protected var indexWriter: BaseIndexWriter = _

  /**
    * Get a searcher instance for the given graph database
    * @param gdb a graph database service
    * @return a searcher instance (singleton)
    */
  def getSearcher(gdb: GraphDatabaseService): NeighbourBasedSearcher with SimpleQueryBuilder

  /**
    * Get an index writer for the given graph database
    * @param gdb a graph database service
    * @return an index writer (singleton)
    */
  def getIndexWriter(gdb: GraphDatabaseService): BaseIndexWriter
}
