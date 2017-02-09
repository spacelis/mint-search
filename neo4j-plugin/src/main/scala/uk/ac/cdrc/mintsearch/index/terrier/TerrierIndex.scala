package uk.ac.cdrc.mintsearch.index.terrier

import java.io.{File, Reader}
import java.util

import org.apache.commons.lang.NotImplementedException
import org.neo4j.graphdb.Node
import org.terrier.indexing.{Collection, Document}
import org.terrier.matching.MatchingQueryTerms
import org.terrier.matching.daat.Full
import org.terrier.matching.models.WeightingModelFactory
import org.terrier.querying.Request
import org.terrier.querying.parser.{MultiTermQuery, SingleTermQuery}
import org.terrier.structures.Index
import org.terrier.structures.indexing.classical.{BasicIndexer => TerrierIndexer}
import org.terrier.utility.ApplicationSetup
import uk.ac.cdrc.mintsearch.graph.NeighbourAwareContext
import uk.ac.cdrc.mintsearch.index._
import uk.ac.cdrc.mintsearch.neo4j.WithResource
import uk.ac.cdrc.mintsearch.{NodeId, TempDir, WeightedLabelSet}

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

  def indexExists: Boolean = Index.existsIndex(path.getAbsolutePath, prefix)
}

/**
  * Terrier's index reader
  */
trait TerrierIndexReader extends BaseIndexReader with TerrierIndex {
  self: LabelMaker =>

  val weightingModel: String = "BM25"

  lazy val indexDB: Index = Index.createIndex(path.getAbsolutePath, prefix)

  def encodeQuery(labelSet: Set[L], index: Index): MatchingQueryTerms = {
    val query = new MultiTermQuery()
    for {
      l <- labelSet
      term = s"$labelStorePropKey:${labelEncodeQuery(l)}"
    } query.add(new SingleTermQuery(term))
    val request = new Request()
    request.setIndex(index)
    request.setQuery(query)
    val ret = new MatchingQueryTerms("1", request)
    ret.setDefaultTermWeightingModel(WeightingModelFactory.newInstance(weightingModel, index))
    ret
  }


  override def getNodesByLabels(labelSet: Set[L]): IndexedSeq[Node] = {
    val mqt = encodeQuery(labelSet, indexDB)
    val indexMatcher = new Full(indexDB)
    val rs = indexMatcher.`match`("1", mqt) // backquotes to escape keyword match
    for {
      nodeId <- rs.getMetaItems("node-id")
    } yield db.getNodeById(nodeId.toLong)
  }

  override def retrieveWeightedLabels(n: Node): WeightedLabelSet[L] =
    deJSONfy(n.getProperty(labelStorePropKey).toString)
}

/**
  * Building a node index based on nodes' neighbourhoods using the Lucene.
  */
trait TerrierIndexWriter extends BaseIndexWriter with TerrierIndex {
  self: NeighbourAwareContext with LabelMaker =>

  /**
    * Implement Document type from Terrier for nodes in Neo4J
    * A node document is defined as the labels within the neighbourhood.
    * How the labels are collected in defined will be mixed in.
    *
    * @param node a Neo4J Node
    */
  class NodeDocument(node: Node) extends Document {

    val nodeId: NodeId = node.getId

    val labelWeights: WeightedLabelSet[L] = node.collectNeighbourhoodLabels

    storeWeightedLabels(node, labelWeights)

    val terms: Set[String] = labelWeights.keys map labelEncode

    val termString: String = terms mkString " "

    private val termIterator = terms.toIterator

    private val properties: Map[String, String] = Map(
      "node-id" -> nodeId.toString,
      "label-number" -> labelWeights.size.toString
    )

    override def getNextTerm: String = termIterator.next()

    override def getProperty(name: String): String = properties(name)

    override def endOfDocument(): Boolean = ! termIterator.hasNext

    override def getReader: Reader = new Reader {
      override def close(): Unit = {}

      override def read(cbuf: Array[Char], off: Int, len: Int): Int = {
        val end = Math.min(off + len, termString.length)
        for {
          i <- off until end
        } cbuf(i) = termString(i)
        end - off
      }
    }

    override def getAllProperties: util.Map[String, String] = properties.asJava

    override def getFields: util.Set[String] = Set("neighbourhood").asJava
  }

  /**
    * A collection of node documents from all the nodes in the database
    */
  class NodeDocumentCollection extends Collection {
    private var allNodes = db.getAllNodes.iterator()

    override def getDocument: Document = new NodeDocument(allNodes.next())

    override def endOfCollection(): Boolean = ! allNodes.hasNext

    override def reset(): Unit = {
      allNodes = db.getAllNodes.iterator()
    }

    override def nextDocument(): Boolean = allNodes.hasNext

    override def close(): Unit = {}
  }

  def getCollections: Array[Collection] = Seq(new NodeDocumentCollection).toArray

  override def index(): Unit = WithResource(db.beginTx()){tx =>
    if (indexExists)
      throw new RuntimeException(
        """There is an index in place,
          |try drop it before rebuild.
        """.stripMargin)
    val indexer = new TerrierIndexer(path.getAbsolutePath, prefix)
    indexer.index(getCollections)

    tx.success()
  }

  override def index(n: Node): Unit = throw NotImplementedException

  override def storeWeightedLabels(n: Node, wls: WeightedLabelSet[L]): Unit =
    n.setProperty(labelStorePropKey, JSONfy(wls))

}

