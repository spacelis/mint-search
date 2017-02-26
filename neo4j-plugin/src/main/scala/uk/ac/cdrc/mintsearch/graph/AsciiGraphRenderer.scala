package uk.ac.cdrc.mintsearch.graph

import org.neo4j.graphdb.Node


/**
  * A simple way of draw ASCII graph
  */

trait Renderable {
  def render: String
}

trait AsciiGraphRenderer {
  implicit def toAscii(n: Node): Renderable
  implicit def toAscii(nodeAdjacency: Map[Node, Seq[Node]]): Renderable
  implicit def toAscii(embedding: GraphEmbedding): Renderable
}

case class NodeOnlyAsciiRenderer(nodeProperties: Seq[String]) extends AsciiGraphRenderer {
  case class NodeRenderable(n: Node) extends Renderable {
    override def render: String = {
      val props = (for {
        p <- nodeProperties
      } yield n.getProperty(p).toString) mkString " "
      s"(${n.getId}: $props)"
    }
  }

  case class GraphRenderable(nodeAdjacency: Map[Node, Seq[Node]]) extends Renderable {
    override def render: String = {
      (for {
        n <- nodeAdjacency.keySet
      } yield n.render) mkString "\n"
    }
  }

  case class EmbeddingRenderable(embedding: GraphEmbedding) extends Renderable {
    override def render: String = {
      val nodes: String = (for {
        n <- embedding.nodes
      } yield n -> Seq.empty).toMap.render
      val projection = (for {
        n <- embedding.nodes
        if embedding.projection contains n.getId
        (v, s) = embedding.projection(n.getId)
      } yield s"${n.render}->V$v [$s]") mkString " , "
      s"""# $projection
         |${embedding.relationships.length}
         |$nodes
      """.stripMargin
    }
  }

  override implicit def toAscii(n: Node): Renderable = NodeRenderable(n)

  override implicit def toAscii(nodeAdjacency: Map[Node, Seq[Node]]): Renderable = GraphRenderable(nodeAdjacency)

  override implicit def toAscii(embedding: GraphEmbedding): Renderable = EmbeddingRenderable(embedding)
}

object SimpleRenderer$ extends AsciiGraphRenderer {

  case class SimpleRenderable(n: AnyRef) extends Renderable{
    override def render: String = n.toString
  }

  override implicit def toAscii(n: Node): Renderable = SimpleRenderable(n)

  override implicit def toAscii(nodeAdjacency: Map[Node, Seq[Node]]): Renderable = SimpleRenderable(nodeAdjacency)

  override implicit def toAscii(embedding: GraphEmbedding): Renderable = SimpleRenderable(embedding)
}
