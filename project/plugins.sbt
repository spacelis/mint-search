logLevel := Level.Warn

resolvers += Resolver.sonatypeRepo("releases")

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases/"

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "0.5.5")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.2")
