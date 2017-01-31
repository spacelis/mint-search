/**
 * Neo4J's bundled lucene is used for node indexing and search.
 * Alternative indexing/search facility can also be used here.
 * For example, MapDB may be a good choice.
 */

package uk.ac.cdrc.mintsearch.index

import java.util.concurrent.TimeUnit.SECONDS

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.index.Index
import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.index.Neo4JIndexTypes._
import uk.ac.cdrc.mintsearch.neighbourhood.NeighbourAwareContext
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext
import uk.ac.cdrc.mintsearch.ranking.{NeighbourSimilarity, NodeRanking}

import scala.collection.JavaConverters._
import scala.pickling._
import scala.pickling.json._

/**
 * No text processing is configured as full text search is not the target of
 * MintSearch. Later can be refactored to include full text indexing which allows
 * text processing such as stemming, stopping words.
 */

trait NodeIndexManager extends GraphDBContext {
  val indexName: String
}

trait NodeIndexReader extends NodeIndexManager {
  self: LabelMaker =>
  def getNodes(labelSet: Set[L]): Iterator[Node]
  def rankNode(weightedLabelSet: WeightedLabelSet[L]): Iterator[Node]
}

trait NodeIndexWriter extends NodeIndexManager {
  def index(): Unit
  def index(n: Node): Unit
}


/**
  * The following traits are implementation of NodeIndex via Neo4J's index interface
  * though it provides limited access to the underlying lucene indices.
  */
trait Neo4JNodeIndexManager extends NodeIndexManager {
  self: LabelMaker =>
  lazy val indexDB: Index[Node] = db.index().forNodes(indexName, FULL_TEXT.asJava)
  def awaitForIndexReady(): Unit = db.schema().awaitIndexesOnline(5, SECONDS)
}

/**
 * Reading the Lucene index for getting a list of potential matched nodes.
 * Those matched nodes will be further ranked, filtered and composed to matched sub graphs.
 */
trait LegacyNeighbourNodeIndexReader extends Neo4JNodeIndexManager with NodeIndexReader{
  self: NeighbourAwareContext with LabelMaker with NeighbourSimilarity with NodeRanking =>

  def encodeQuery(labelSet: Set[L]): String = {
    (for {
      l <- labelSet
    } yield s"$labelStorePropKey:${labelEncodeQuery(l)}") mkString " "
  }

  override def getNodes(labelSet: Set[L]): Iterator[Node] = indexDB.query(encodeQuery(labelSet)).iterator().asScala

  override def rankNode(weightedLabelSet: WeightedLabelSet[L]): Iterator[Node] = getNodes(weightedLabelSet.keySet)
}

/**
 * Building a node index based on nodes' neighbourhoods using the Lucene.
 */
trait LegacyNeighbourNodeIndexWriter extends Neo4JNodeIndexManager with NodeIndexWriter {
  self: NeighbourAwareContext with LabelMaker =>
  override def index(): Unit = for (n <- db.getAllNodes.asScala) index(n)
  override def index(n: Node): Unit = {
    val labelWeights = n.collectNeighbourhoodLabels

    // Indexing the node and store the neighbors' labels in the node's property
    indexDB.remove(n) // Make sure the node will be replaced in the index
    indexDB.add(n, labelStorePropKey, labelWeights.keys map labelEncode mkString " ")
    n.setProperty(labelStorePropKey, labelWeights.map((e) => (labelEncode(e._1), e._2)).pickle.value)
  }
}

