package uk.ac.cdrc.mintsearch.index.terrier

import java.io.{File, Reader}
import java.util

import org.neo4j.graphdb.Node
import org.terrier.indexing.{Collection, Document}
import org.terrier.structures.{BasicDocumentIndexEntry, BasicLexiconEntry, Index, IndexOnDisk}
import org.terrier.structures.indexing.{DocumentIndexBuilder, Indexer, LexiconBuilder, LexiconMap}
import org.terrier.structures.indexing.classical.{BasicIndexer => TerrierIndexer}
import org.terrier.utility.ApplicationSetup
import uk.ac.cdrc.mintsearch.graph.NeighbourAwareContext
import uk.ac.cdrc.mintsearch.index._
import uk.ac.cdrc.mintsearch.neo4j.WithResource
import uk.ac.cdrc.mintsearch.{TempDir, WeightedLabelSet}

import scala.collection.JavaConverters._

/**
  * The following are adapters for Terrier's indexing facility
  */
trait TerrierIndex extends BaseIndexManager{
  self: LabelTypeContext =>
  val prefix: String = "mintsearch-terrier"
  private val pathDir = TempDir()
  val path: File = pathDir.value
  //Make sure simple term pipeline is used
  ApplicationSetup.setProperty("termpipelines", "")
  protected var indexed = false
  val indexDB: IndexOnDisk = Index.createIndex(path.getAbsolutePath, prefix)
}

/**
  * Terrier's index reader
  */
trait TerrierIndexReader extends BaseIndexReader with TerrierIndex {
  self: LabelMaker =>

  def encodeQuery(labelSet: Set[L]): String = {
    (for {
      l <- labelSet
    } yield s"$labelStorePropKey:${labelEncodeQuery(l)}") mkString " "
  }

  override def getNodesByLabels(labelSet: Set[L]): IndexedSeq[Node] = ???
//    indexDB.query(encodeQuery(labelSet)).iterator().asScala.toIndexedSeq

  override def retrieveWeightedLabels(n: Node): WeightedLabelSet[L] =
    deJSONfy(n.getProperty(labelStorePropKey).toString)
}

/**
  * Building a node index based on nodes' neighbourhoods using the Lucene.
  */
trait TerrierIndexWriter extends TerrierIndexer with BaseIndexWriter with TerrierIndex {
  self: NeighbourAwareContext with LabelMaker =>

  class NodeDocument extends Document {
    override def getNextTerm: String = ???

    override def getProperty(name: String): String = ???

    override def endOfDocument(): Boolean = ???

    override def getReader: Reader = ???

    override def getAllProperties: util.Map[String, String] = ???

    override def getFields: util.Set[String] = ???
  }

  class NodeDocumentCollection extends Collection {
    override def getDocument: Document = ???

    override def endOfCollection(): Boolean = ???

    override def reset(): Unit = ???

    override def nextDocument(): Boolean = ???

    override def close(): Unit = {}
  }

  def getCollection: Collection = ???

  override def index(): Unit = WithResource(db.beginTx()){tx =>
    if (!indexed)
      index(Seq(getCollection).toArray)
    tx.success()
    indexed = true
  }

  override def index(collections: Array[Collection]): Unit = {}

  override def index(n: Node): Unit = {
    val labelWeights = n.collectNeighbourhoodLabels

    storeWeightedLabels(n, labelWeights)
  }

  override def storeWeightedLabels(n: Node, wls: WeightedLabelSet[L]): Unit =
    n.setProperty(labelStorePropKey, JSONfy(wls))

}

