
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases/"

val neo4j_version = "3.1.1"

val deployTask = TaskKey[Unit]("deploy", "Copies assembly jar to remote location")

val devServerStartTask = TaskKey[Unit]("devStart", "Start the dev server within a docker container")

val devServerRestartTask = TaskKey[Unit]("devRestart", "Re-start the dev server within a docker container")

val devServerRelaunchTask = TaskKey[Unit]("devRelaunch", "Re-start the dev server within a docker container")

val devServerLogsTask = TaskKey[Unit]("devLogs", "Re-start the dev server within a docker container")

val neo4jDeployJars = TaskKey[Seq[File]]("Collect jars that needs to be deployed along with the Neo4J plugin")


lazy val devDeploySettings = Seq(
  deployTask := {
    val (_, file) = packagedArtifact.in(Compile, packageBin).value
    val remote = "dev_server/plugins"

    for (f <- neo4jDeployJars.value :+ file)
      {
        println(s"Copy $f -> $remote")
        Seq("cp", f.getAbsolutePath, remote) !
      }
  },
  neo4jDeployJars := {
    val is = (ivyScala in Compile).value
    def mkModuleRef(m: ModuleID) = s"${m.organization}:${(for(cvf <- CrossVersion(m, is)) yield cvf(m.name)) getOrElse m.name}:${m.revision}"
    val libMap = (for (p <- (fullClasspath in Compile).value;
         m <- p.metadata.get(moduleID.key))
      yield mkModuleRef(m) -> p.data).toMap
    for (m <- libraryDependencies.value
         if m.organization != "org.neo4j";
         verM = mkModuleRef(m)
         if libMap contains verM)
      yield libMap(verM)
  }
)

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
    version := "0.1-SNAPSHOT"
  )


lazy val neo4j_plugin = (project in file("neo4j-plugin")).
  settings(devDeploySettings: _*).
  settings(
    name := "mintsearch-neo4j-plugin",
    libraryDependencies ++= Seq(
      "org.neo4j" % "neo4j" % neo4j_version,
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0",
      "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
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

devServerStartTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose up -d") !
}


devServerRestartTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose restart") !
}


devServerLogsTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose logs --tail=100") !
}

devServerRelaunchTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose stop") !

  Seq("bash", "-c", "cd dev_server && sudo rm -rf data/databases/graph.db") !

  Seq("bash", "-c", "cd dev_server && docker-compose up -d") !

}
