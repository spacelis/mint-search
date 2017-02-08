/**
  * Neo4J's bundled lucene is used for node indexing and search.
  * Alternative indexing/search facility can also be used here.
  * For example, MapDB may be a good choice.
  */

package uk.ac.cdrc.mintsearch.index

import org.neo4j.graphdb.Node
import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.neo4j.GraphDBContext

/**
  * No text processing is configured as full text search is not the target of
  * MintSearch. Later can be refactored to include full text indexing which allows
  * text processing such as stemming, stopping words.
  */

trait BaseIndexManager extends GraphDBContext {
  val indexName: String
}

trait BaseIndexReader extends BaseIndexManager {
  self: LabelTypeContext =>
  def getNodesByLabels(labelSet: Set[L]): IndexedSeq[Node]
  def retrieveWeightedLabels(n: Node): WeightedLabelSet[L]
}

trait BaseIndexWriter extends BaseIndexManager {
  self: LabelTypeContext =>
  def index(): Unit
  def index(n: Node): Unit
  def storeWeightedLabels(n: Node, wls: WeightedLabelSet[L]): Unit
}


