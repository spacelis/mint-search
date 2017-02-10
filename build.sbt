import scala.annotation.tailrec

/**
  * The following are auxiliary functions for building files
  */

val neo4jDeployJars = TaskKey[Seq[(String, File)]]("Collect jars that needs to be deployed along with the Neo4J plugin")

val depGraph = TaskKey[Map[ModuleID, Set[ModuleID]]]("Build dependency graph for the project")

val jarCollection = TaskKey[Map[ModuleID, Seq[File]]]("The total collection of jars")

@tailrec
def expandDependencies(seeds: Set[String], graph: Map[String, Set[String]]): Set[String] = {
  val newDep = for {
    m <- seeds
    ds <- graph.get(m).toSet[Set[String]]
    d <- ds
  } yield d
  if (newDep subsetOf seeds)
    seeds
  else
    expandDependencies(seeds ++ newDep, graph)
}

def mkModuleRef(is: Option[IvyScala]) = (m: ModuleID) =>
  s"${m.organization}:${(for(cvf <- CrossVersion(m, is)) yield cvf(m.name)) getOrElse m.name}:${m.revision}"

val deployTask = TaskKey[Unit]("deploy", "Copies assembly jar to remote location")

lazy val devDeploySettings = Seq(
  deployTask := {

    val (msID, msJar) = packagedArtifact.in(Compile, packageBin).value
    val remote = "dev_server/plugins"

    for ((m, f) <- neo4jDeployJars.value :+ (msID.name, msJar))
    {
      println(s"Copy $m -> $remote")
      Seq("cp", f.getAbsolutePath, remote).!
    }
  },
  neo4jDeployJars := {
    val is = (ivyScala in Compile).value
    val mkModRef = mkModuleRef(is)

    val graph = depGraph.value map {
      case (k, v) => mkModRef(k) -> v.map(mkModRef)
    }

    val coll = jarCollection.value map {
      case (k, v) => mkModRef(k) -> v
    }

    val toDeploy = for {
      exDep <- (libraryDependencies in Compile).value.toSet
      if ! exDep.organization.startsWith("org.neo4j")
      if (exDep.configurations match {case Some(_) => false; case _ => true})
    } yield mkModRef(exDep)

    val toDeployEx: Seq[String] = expandDependencies(toDeploy, graph)
      .map(s => s.splitAt(s lastIndexOf ":"))
      .groupBy(_._1)
      .mapValues(_.toSeq.map(_._2).sorted.reverse.head).toSeq
      .map(v => s"${v._1}${v._2}")

    for {
      m <- toDeployEx
      f <- coll(m).toSet[File]
    } yield (m, f)
  },
  depGraph := {
    (for {
      c <- update.value.configurations
      r <- c.details
      m <- r.modules
      caller <- m.callers
    } yield caller.caller -> m.module)
      .groupBy(_._1)
      .mapValues((p: Seq[(ModuleID, ModuleID)]) =>
        p.map(_._2).toSet
      )
  },
  jarCollection := {
    (for {
      c <- update.value.configurations
      r <- c.details
      m <- r.modules
    } yield m.module -> m.artifacts.map(_._2)).toMap
  }

)

/**
  * Dev server tasks
  */

val devServerStartTask = TaskKey[Unit]("devStart", "Start the dev server within a docker container")

val devServerRestartTask = TaskKey[Unit]("devRestart", "Re-start the dev server within a docker container")

val devServerRelaunchTask = TaskKey[Unit]("devRelaunch", "Re-start the dev server within a docker container")

val devServerLogsTask = TaskKey[Unit]("devLogs", "Re-start the dev server within a docker container")

devServerStartTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose up -d").!
}


devServerRestartTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose restart").!
}


devServerLogsTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose logs --tail=100").!
}

devServerRelaunchTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose stop").!

  Seq("bash", "-c", "cd dev_server && sudo rm -rf data/databases/graph.db").!

  Seq("bash", "-c", "cd dev_server && docker-compose up -d").!

}


/**
  * Building tasks
  */


val neo4j_version = "3.1.1"

lazy val mintsearch = (project in file(".")).
  aggregate(neo4j_plugin).
  settings(inThisBuild(List(
      organization := "cdrc.ac.uk",
      scalaVersion := "2.11.8"
    )),
    packagedArtifacts := Map.empty,
    publish := {},
    publishLocal := {},
    name := "mintsearch",
    version := "1.0"
  )


lazy val neo4j_plugin = (project in file("neo4j-plugin")).
  settings(devDeploySettings: _*).
  settings(
    name := "mintsearch-neo4j-plugin",
    version := "1.0",
    resolvers += "jitpack" at "https://jitpack.io/",
    libraryDependencies ++= Seq(
      "com.github.yasserg" % "jforests" % "v0.5",
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0",
      "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
      "org.terrier" % "terrier-core" % "4.2" exclude ("org.apache.tika", "tika-parsers" ),
      "org.neo4j" % "neo4j" % neo4j_version,
      "org.neo4j.test" % "neo4j-harness" % neo4j_version,
      "org.neo4j.driver" % "neo4j-java-driver" % "1.1.1" % "test",
      "org.neo4j" % "neo4j-io" % neo4j_version % "test" classifier "tests",
      "org.neo4j" % "neo4j-kernel" % neo4j_version % "test",
      "org.neo4j" % "neo4j-graph-algo" % neo4j_version % "test",
      "com.sun.jersey" % "jersey-core" % "1.19" % "test",
      "com.sun.jersey" % "jersey-server" % "1.19" % "test",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    ),
    parallelExecution in Test := false
  )


