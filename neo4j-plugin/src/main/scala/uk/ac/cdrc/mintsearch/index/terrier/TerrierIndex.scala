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
  val path: File = TerrierIndex.pathDir.value

  def indexExists: Boolean = Index.existsIndex(path.getAbsolutePath, prefix)
}

object TerrierIndex {
  // Make sure every runtime there is only one path for Terrier index
  val pathDir = TempDir()
  ApplicationSetup.setProperty("termpipelines", "") // Make sure simple term pipeline is used
  ApplicationSetup.setProperty("indexer.meta.forward.keys", "node-id,label-number") //Forward these properties to meta index builder
  ApplicationSetup.setProperty("indexer.meta.forward.keylens", "20,20")
}

/**
  * Terrier's index reader
  */
trait TerrierIndexReader extends BaseIndexReader with TerrierIndex {
  self: LabelMaker =>

  /**
    * Different weighting model can be used for getting nodes.
    * @see org.terrier.matching.models
    */
  val weightingModel: String = "BM25"

  lazy val indexDB: Index = Index.createIndex(path.getAbsolutePath, prefix)

  /**
    * Make a Terrier query out of the labelSet
    * @param labelSet a query label set
    * @param index a Terrier index
    * @return a MatchingQueryTerms as the input to a Terrier index matcher
    */
  def encodeQuery(labelSet: Set[L], index: Index): MatchingQueryTerms = {
    val query = new MultiTermQuery()
    for {
      l <- labelSet
      term = s"${labelEncode(l).toLowerCase}"
    } query.add(new SingleTermQuery(term))
    val request = new Request()
    request.setIndex(index)
    request.setQuery(query)
    val ret = new MatchingQueryTerms("1", request)
    ret.setDefaultTermWeightingModel(WeightingModelFactory.newInstance(weightingModel, index))
    query.obtainQueryTerms(ret)
    ret
  }


  /**
    * Search the Terrier index base for node documents per query
    * @param labelSet the query label set
    * @return a set of nodes matching to the query
    */
  override def getNodesByLabels(labelSet: Set[L]): IndexedSeq[Node] = {
    val mqt = encodeQuery(labelSet, indexDB)
    val indexMatcher = new Full(indexDB)
    val rs = indexMatcher.`match`("1", mqt) // backquotes to escape keyword match
    val meta = indexDB.getMetaIndex
    for {
      docId <- rs.getDocids.toIndexedSeq
      nodeId = meta.getItem("node-id", docId)
    } yield db.getNodeById(nodeId.toLong)
  }

  /**
    * @inheritdoc
    * @param n a node
    * @return a weighted label set from the node
    */
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

    storeWeightedLabels(node, labelWeights) // Save the composed node doc to the node property

    val terms: Set[String] = for {
      lw: L <- labelWeights.keys.toSet
    } yield labelEncode(lw).toLowerCase


    private val termIterator = terms.toIterator

    private val properties: Map[String, String] = Map(
      "node-id" -> nodeId.toString,
      "label-number" -> labelWeights.size.toString
    )

    override def getNextTerm: String = termIterator.next()

    override def getProperty(name: String): String = properties(name)

    override def endOfDocument(): Boolean = ! termIterator.hasNext

    override def getReader: Reader = new Reader {

      private final val termString: String = terms mkString " "

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

    override def getFields: util.Set[String] = Set[String]().asJava
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

  /**
    * Compose a collection array to cater Terrier's indexers
    * @return an array of collections
    */
  def getCollections: Array[Collection] = Seq(new NodeDocumentCollection).toArray

  /**
    * Index all the nodes in Neo4J database via Terrier's BasicIndexer
    */
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

  /**
    * This is not in use in Terrier indexing infrastructure
    * @param n a node
    */
  override def index(n: Node): Unit = throw new NotImplementedException()

  /**
    * @inheritdoc
    * @param n a node
    * @param wls the node's weighted label set
    */
  override def storeWeightedLabels(n: Node, wls: WeightedLabelSet[L]): Unit =
    n.setProperty(labelStorePropKey, JSONfy(wls))

  override def awaitForIndexReady(): Unit = {}
}

