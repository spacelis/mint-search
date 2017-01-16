package uk.ac.cdrc.mintsearch.neo4j

import java.util.stream.{Stream => JStream}

import org.neo4j.graphdb.{GraphDatabaseService, Node}
import org.neo4j.procedure.{Name, PerformsWrites, Procedure}
import uk.ac.cdrc.mintsearch.index.NeighbourAggregatedIndexReader
import uk.ac.cdrc.mintsearch.neighbourhood.{ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius}

import scala.collection.JavaConverters._
import scala.compat.java8.StreamConverters._

/**
  * This class implements the NEighborhood based Similarity Search
  */
class NeighbourAggregatedSearch extends Neo4JProcedure {

  val indexManager = new NeighbourAggregatedIndexReader
    with GraphContext
    with ExponentialPropagation
    with PropertyLabelMaker
    with NeighbourhoodByRadius
    with NeighbourAwareContext
  {

    override val radius: Int = 2
    override val propagationFactor: Double = 0.5

    override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
    override val labelStorePropKey: String = s"__nagg_$radius"
    override val db: GraphDatabaseService = NeighbourAggregatedSearch.this.db
  }

  /**
    *
    * @param propName the property name for the labels used in the index
    * @param relType  the name of the relationtype used in the index the index
    * @param query    the lucene query.
    * @return the nodes found by the query
    */
  @Procedure("mint.nagg_search")
  @PerformsWrites // TODO: This is here as a workaround, because index().forNodes() is not read-only
  def search(
              @Name("relType") relType: String,
              @Name("propName") propName: String,
              @Name("query") query: String
            ): JStream[SearchHit] = {
    Option(db.index().forNodes(indexManager.indexName)) match {
      case Some(index) => index.query(query).iterator().asScala.map((n: Node) => new SearchHit(n)).seqStream
      case None => JStream.empty()
    }
  }

  /**
    *
    * @param nodeId   the id of the node to index
    * @param relType  the relation type used as neighboring
    * @param propName the property name for the labels, the labels should be stored as a space-separated words
    * @param depth    the label propagation range, i.e., number of hops a label can propagate though neighbor nodes
    * @param alpha    the alpha (propagating damper) and 0 < alpha < 1, i.e., labels are propagated to the neighbor with
    *                 a less weight
    */
  @Procedure("mint.nagg_index")
  @PerformsWrites
  def index(
             @Name("nodeId") nodeId: Long,
             @Name("relType") relType: String,
             @Name("propName") propName: String,
             @Name("depth") depth: Long,
             @Name("alpha") alpha: Double
           ): Unit = {

    indexManager.index()
  }
}

