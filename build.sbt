val neo4j_version = "3.0.6"

lazy val root = (project in file(".")).
  aggregate(neo4j_plugin).
  settings(inThisBuild(List(
      organization := "cdrc.ac.uk",
      scalaVersion := "2.11.8"
    )),
    packagedArtifacts := Map.empty
  )


lazy val neo4j_plugin = (project in file("neo4j-plugin")).
  settings(
    name := "mintsearch-neo4j-plugin",
    libraryDependencies ++= Seq(
      "org.neo4j" % "neo4j" % neo4j_version,
      "org.neo4j.test" % "neo4j-harness" % neo4j_version % "test",
      "org.neo4j.driver" % "neo4j-java-driver" % "1.0.4" % "test"
    )
  )
