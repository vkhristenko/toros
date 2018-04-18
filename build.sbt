import sbtcrossproject.{crossProject, CrossType}

val sharedSettings = Seq(
  scalaVersion := "2.11.12",
  organization := "ch.cern",
  version := "0.0.0"
)

// to make sbt able to search mvn local repository
resolvers += Resolver.mavenLocal

// root project definition
lazy val toros = 
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .settings(sharedSettings)
    .in(file("."))
    .aggregate(io, apps).dependsOn(io, apps)

// apps sub-project
lazy val apps = 
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .settings (
      sharedSettings, 
      name := "toros-apps")
    .jsSettings()
    .jvmSettings()
    .nativeSettings()
    .in(file("apps"))
    .dependsOn(io)

// io sub-project
lazy val io = 
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .settings (
      sharedSettings, 
      name := "toros-io")
    .jsSettings()
    .jvmSettings()
    .nativeSettings()
    .in(file("io"))

lazy val ioJS     = io.js
lazy val ioJVM    = io.jvm
lazy val ioNative = io.native

lazy val appsJS     = apps.js
lazy val appsJVM    = apps.jvm
lazy val appsNative = apps.native
