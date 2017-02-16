package uk.ac.cdrc.mintsearch.index

import java.util.concurrent.TimeUnit.SECONDS

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.index.Index
import uk.ac.cdrc.mintsearch.WeightedLabelSet
import uk.ac.cdrc.mintsearch.graph.NeighbourAwareContext
import uk.ac.cdrc.mintsearch.index.Neo4JIndexTypes._
import uk.ac.cdrc.mintsearch.neo4j.WithResource

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * The following traits are implementation of NodeIndex via Neo4J's index interface
  * though it provides limited access to the underlying lucene indices.
  */
trait Neo4JBaseIndexManager extends BaseIndexManager {
  self: LabelTypeContext =>
  lazy val indexDB: Index[Node] = db.index().forNodes(indexName, FULL_TEXT.asJava)
}

/**
  * Reading the Lucene index for getting a list of potential matched nodes.
  * Those matched nodes will be further ranked, filtered and composed to matched sub graphs.
  */
trait LegacyNeighbourBaseIndexReader extends BaseIndexReader with Neo4JBaseIndexManager {
  self: LabelMaker =>

  def encodeQuery(labelSet: Set[L]): String = {
    (for {
      l <- labelSet
    } yield s"$labelStorePropKey:${labelEncodeQuery(l)}") mkString " "
  }

  override def getNodesByLabels(labelSet: Set[L]): IndexedSeq[Node] =
    indexDB.query(encodeQuery(labelSet)).iterator().asScala.toIndexedSeq

  override def retrieveWeightedLabels(n: Node): WeightedLabelSet[L] =
    deJSONfy(n.getProperty(labelStorePropKey).toString)
}

/**
  * Building a node index based on nodes' neighbourhoods using the Lucene.
  */
trait LegacyNeighbourBaseIndexWriter extends BaseIndexWriter with Neo4JBaseIndexManager {
  self: NeighbourAwareContext with LabelMaker =>
  override def index(): Unit = {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
    val executed = for (nodeSet <- db.getAllNodes.asScala.toIterator.grouped(1000))
      yield Future {
        WithResource(db.beginTx()) { tx =>
          for (n <- nodeSet) index(n)
          tx.success()
        }
        nodeSet.size
      }
    val succeeded = (for {
      f <- executed
    } yield Await.result(f, Duration.Inf)).sum
  }

  override def index(n: Node): Unit = {
    val labelWeights = n.collectNeighbourhoodLabels

    // Indexing the node and store the neighbors' labels in the node's property
    indexDB.remove(n) // Make sure the node will be replaced in the index
    indexDB.add(n, labelStorePropKey, labelWeights.keys map labelEncode mkString " ")
    storeWeightedLabels(n, labelWeights)
  }

  override def storeWeightedLabels(n: Node, wls: WeightedLabelSet[L]): Unit =
    n.setProperty(labelStorePropKey, JSONfy(wls))

  override def awaitForIndexReady(): Unit = db.schema().awaitIndexesOnline(5, SECONDS)
}

