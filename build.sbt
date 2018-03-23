import sbtcrossproject.{crossProject, CrossType}

val sharedSettings = Seq(
  scalaVersion := "2.11.12",
  organization := "ch.cern",
  version := "0.0.0"
)

// to make sbt able to search mvn local repository
resolvers += Resolver.mavenLocal

// hello task
lazy val hello = taskKey[Unit]("An example task")

// io project
lazy val io = 
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .settings (sharedSettings, hello := {println("Hello World!")} )
    .jsSettings( hello := {println("Hello JS!")})
    .jvmSettings()
    .nativeSettings()
    .in(file("io"))

lazy val ioJS     = io.js
lazy val ioJVM    = io.jvm
lazy val ioNative = io.native
