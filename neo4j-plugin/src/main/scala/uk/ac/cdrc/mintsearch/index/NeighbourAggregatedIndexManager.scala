package uk.ac.cdrc.mintsearch.index

import org.neo4j.graphdb.index.Index
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.graphdb.{GraphDatabaseService, Node, Path}
import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.neighbourhood.{NeighbourAwareNode, Propagation, TraversalStrategy}
import uk.ac.cdrc.mintsearch.neo4j.MintSearchContext
import uk.ac.cdrc.mintsearch.neo4j.Neo4JIndex._

import scala.collection.JavaConverters._
import scala.pickling._
import scala.pickling.json._

/**
  * Created by ucfawli on 04-Dec-16.
  */
trait NeighbourAggregatedIndexManager extends MintSearchContext{
  self: Propagation with TraversalStrategy =>
  val indexName: String

  val indexDB: Index[Node] = db.index().forNodes(indexName, EXACT_TEXT.asJava)
  case class NeighbourAwareNodeImpl(node: Node) extends NeighbourAwareNode with Propagation{
    override def propagate(p: Path): WeightedLabelSet = self.propagate(p)

    override val traversalDescription: TraversalDescription = self.traversalDescription
    override val db: GraphDatabaseService = self.db
    override val labelPropKey: String = self.labelPropKey

    override def collectLabels(n: Node): Seq[String] = self.collectLabels(n)
  }

  implicit def wrapNode(node: Node) = NeighbourAwareNodeImpl(node)

  def index(): Unit = for (n <- db.getAllNodes) index(n)
  def index(n: Node): Unit = {
    val labelWeights = n.collectNeighbourLabels

    // Indexing the node and store the neighbors' labels in the node's property
    indexDB.remove(n) // Make sure the node will be replaced in the index
    indexDB.add(n, labelPropKey, labelWeights.keys mkString " ")
    n.setProperty(labelPropKey, labelWeights.pickle.value)
  }
}
