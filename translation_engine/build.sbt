name := "translation_engine"

version := "0.0.1-WORKPRINT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.4.1"
)


// see http://stackoverflow.com/a/9901616
//
// instantiating the `SBTSetupHook` and `SBTCleanupHook`
// classes causes code to run that prepares the testing
// environment & works around some classloader issues with
// sbt and orient / gremlin
testOptions in Test += Tests.Setup( loader => {
  println("test setup")
  loader.loadClass("org.mediachain.SBTSetupHook").newInstance
})

testOptions in Test += Tests.Cleanup( loader => {
  println("test cleanup")
  loader.loadClass("org.mediachain.SBTCleanupHook").newInstance
})
