import sbtcrossproject.{crossProject, CrossType}

val sharedSettings = Seq(scalaVersion := "2.11.12")

// io project
lazy val io = 
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .settings(sharedSettings)
    .jsSettings()
    .jvmSettings()
    .nativeSettings()
    .in(file("io"))

lazy val ioJS     = io.js
lazy val ioJVM    = io.jvm
lazy val ioNative = io.native
