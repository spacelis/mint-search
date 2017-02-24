package uk.ac.cdrc.mintsearch.graph

import org.neo4j.graphdb.Node


/**
  * A simple way of draw ASCII graph
  */

trait AsciiGraphRender {
  def toAscii(n: Node): String
  def toAscii(nodeAdjacency: Map[Node, Seq[Node]]): String
  def toAscii(embedding: GraphEmbedding): String
}

case class NodeOnlyAsciiRender(nodeProperties: Seq[String]) extends AsciiGraphRender {
  override def toAscii(n: Node): String = {
    val props = (for {
      p <- nodeProperties
    } yield n.getProperty(p).toString) mkString " "
    s"(${n.getId}: $props)"
  }

  override def toAscii(nodeAdjacency: Map[Node, Seq[Node]]): String = {
    (for {
      n <- nodeAdjacency.keySet
    } yield toAscii(n)) mkString "\n"
  }

  override def toAscii(embedding: GraphEmbedding): String = {
    val nodes = toAscii((for {
      n <- embedding.nodes
    } yield n -> Seq.empty).toMap)
    val keyNodes = embedding.keyNodes map {x => s"($x)"} mkString ", "
    s"""
       | # $keyNodes
       | $nodes
     """.stripMargin
  }
}
