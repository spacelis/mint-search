package uk.ac.cdrc.mintsearch.index

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.index.Index
import uk.ac.cdrc.mintsearch.neighbourhood.NeighbourAwareContext
import uk.ac.cdrc.mintsearch.neo4j.Neo4JIndex._
import uk.ac.cdrc.mintsearch.neo4j.{GraphContext, LabelMaker}

import scala.collection.JavaConverters._
import scala.pickling._
import scala.pickling.json._

/**
  * Created by ucfawli on 04-Dec-16.
  */
trait NeighbourAggregatedIndexManager{
  self: GraphContext with NeighbourAwareContext with LabelMaker =>
  val indexName: String

  lazy val indexDB: Index[Node] = db.index().forNodes(indexName, EXACT_TEXT.asJava)

  def index(): Unit = for (n <- db.getAllNodes.asScala) index(n)
  def index(n: Node): Unit = {
    val labelWeights = n.collectNeighbourLabels

    // Indexing the node and store the neighbors' labels in the node's property
    indexDB.remove(n) // Make sure the node will be replaced in the index
    indexDB.add(n, labelPropKey, labelWeights.keys mkString " ")
    n.setProperty(labelPropKey, labelWeights.pickle.value)
  }
}
