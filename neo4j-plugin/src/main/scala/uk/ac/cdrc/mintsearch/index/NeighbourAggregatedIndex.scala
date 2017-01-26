package uk.ac.cdrc.mintsearch.index

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.index.Index
import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.neighbourhood.NeighbourAwareContext
import uk.ac.cdrc.mintsearch.neo4j.Neo4JIndexTypes._
import uk.ac.cdrc.mintsearch.neo4j.{ GraphContext, LabelMaker }
import uk.ac.cdrc.mintsearch.ranking.{ NeighbourSimilarity, NodeRanking }

import scala.collection.JavaConverters._
import scala.pickling._
import scala.pickling.json._

/**
 * Neo4J's bundled lucene is used for node indexing and search.
 * Alternative indexing/search facility can also be used here.
 * For example, MapDB may be a good choice.
 */

/**
 * No text processing is configured as full text search is not the target of
 * MintSearch. Later can be refactored to include full text indexing which allows
 * text processing such as stemming, stopping words.
 */
trait IndexManager extends GraphContext {
  val indexName: String

  lazy val indexDB: Index[Node] = db.index().forNodes(indexName, FULL_TEXT.asJava)
}

/**
 * Reading the Lucene index for getting a list of potential matched nodes.
 * Those matched nodes will be further ranked, filtered and composed to matched sub graphs.
 */
trait NeighbourAggregatedIndexReader extends IndexManager {
  self: NeighbourAwareContext with LabelMaker with NeighbourSimilarity with NodeRanking =>
  import uk.ac.cdrc.mintsearch.asWightedLabelSetWrapper

  def getSimilarNodes(wls: WeightedLabelSet): Iterator[Node] = {
    indexDB.query(wls.tokenized).iterator().asScala
  }
}

/**
 * Building a node index based on nodes' neighbourhoods using the Lucene.
 */
trait NeighbourAggregatedIndexWriter extends IndexManager {
  self: NeighbourAwareContext with LabelMaker =>
  def index(): Unit = for (n <- db.getAllNodes.asScala) index(n)
  def index(n: Node): Unit = {
    val labelWeights = n.collectNeighbourLabels

    // Indexing the node and store the neighbors' labels in the node's property
    indexDB.remove(n) // Make sure the node will be replaced in the index
    indexDB.add(n, labelStorePropKey, labelWeights.keys mkString " ")
    n.setProperty(labelStorePropKey, labelWeights.pickle.value)
  }
}