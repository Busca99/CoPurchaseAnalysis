ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "CoPurchaseAnalysis",
    libraryDependencies ++= Seq(
      // Dipendenze Spark (fornite da DataProc)
      "org.apache.spark" %% "spark-core" % "3.1.3" % "provided", // NECESSARIO mettere "provided"
      "org.apache.spark" %% "spark-sql"  % "3.1.3" % "provided", // NECESSARIO mettere "provided"

      // Connettore Google Cloud Storage (fornito da DataProc)
      "com.google.cloud.bigdataoss" % "gcs-connector" % "hadoop3-2.2.11" % "provided" // NECESSARIO mettere "provided"
    )
  )