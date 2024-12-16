// See README.md for license details.

ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.2.0"
ThisBuild / organization     := "com.github.kazutomo"
ThisBuild / logLevel := Level.Warn

val chiselVersion = "6.4.0"



lazy val root = (project in file("."))
  .settings(
    name := "pca-comp",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "org.scalatest" %% "scalatest" % "3.2.16" % "test",
      "edu.berkeley.cs" %% "chiseltest" % "6.0.0",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-Ymacro-annotations",
      "-P:chiselplugin:genBundleElements",
    ),
    addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
  )
