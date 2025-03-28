val scala3Version = "3.6.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "TaskManagerGUI",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "org.scalafx" %% "scalafx" % "22.0.0-R33",
      "com.lihaoyi" %% "upickle" % "3.3.1" // Add upickle for JSON serialization
    ),
    libraryDependencies ++= {
      val javaFXVersion = "24"
      Seq(
        "org.openjfx" % "javafx-controls" % javaFXVersion,
        "org.openjfx" % "javafx-fxml" % javaFXVersion
      )
    },
    fork := true,
    javaOptions ++= Seq(
      "--enable-native-access=ALL-UNNAMED",
      "-Djavafx.verbose=false",
      "-Dprism.order=sw"
    )
  )
