val neo4j_version = "3.0.7"


lazy val devDeploySettings = Seq(
  deployTask := {
    val (art, file) = packagedArtifact.in(Compile, packageBin).value
    val remote = "dev_server/plugins"
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
      "org.neo4j.test" % "neo4j-harness" % neo4j_version % "test",
      "org.neo4j.driver" % "neo4j-java-driver" % "1.0.4" % "test"
    )
  )

val deployTask = TaskKey[Unit]("deploy", "Copies assembly jar to remote location")

