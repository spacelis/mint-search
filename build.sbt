val neo4j_version = "3.0.7"

val deployTask = TaskKey[Unit]("deploy", "Copies assembly jar to remote location")

val devServerStartTask = TaskKey[Unit]("devStart", "Start the dev server within a docker container")

val devServerRestartTask = TaskKey[Unit]("devRestart", "Re-start the dev server within a docker container")

val devServerRelaunchTask = TaskKey[Unit]("devRelaunch", "Re-start the dev server within a docker container")

val devServerLogsTask = TaskKey[Unit]("devLogs", "Re-start the dev server within a docker container")

lazy val devDeploySettings = Seq(
  deployTask := {
    val (art, file) = packagedArtifact.in(Compile, packageBin).value
    val remote = "dev_server/plugins"
    println(s"Copy $file -> $remote")
    s"cp $file $remote" !
  }
)

lazy val root = (project in file(".")).
  aggregate(neo4j_plugin).
  settings(inThisBuild(List(
      organization := "cdrc.ac.uk",
      scalaVersion := "2.11.8"
    )),
    packagedArtifacts := Map.empty,
    publish := {},
    publishLocal := {}
  )


lazy val neo4j_plugin = (project in file("neo4j-plugin")).
  settings(devDeploySettings: _*).
  settings(
    name := "mintsearch-neo4j-plugin",
    libraryDependencies ++= Seq(
      "org.neo4j" % "neo4j" % neo4j_version,
      "org.scala-lang.modules" % "scala-java8-compat_2.11" % "0.8.0",
      "org.neo4j.test" % "neo4j-harness" % neo4j_version % "test",
      "org.neo4j.driver" % "neo4j-java-driver" % "1.0.4" % "test"
    )
  )

devServerStartTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose up -d") !
}


devServerRestartTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose restart") !
}


devServerLogsTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose logs") !
}

devServerRelaunchTask := {
  Seq("bash", "-c", "cd dev_server && docker-compose stop") !

  Seq("bash", "-c", "cd dev_server && sudo rm -rf data/databases/graph.db") !

  Seq("bash", "-c", "cd dev_server && docker-compose up -d") !

}
