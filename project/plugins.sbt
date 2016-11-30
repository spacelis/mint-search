logLevel := Level.Warn

resolvers += Resolver.sonatypeRepo("releases")

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.1")
