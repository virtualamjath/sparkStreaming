name := "Spark-streaming-and-twitter"


version := "0.0.1"


scalaVersion := "2.10.4"

mainClass in assembly := Some("org.invati.twitter.SparkTwitterConnector")

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

resolvers ++= Seq( 

"Cloudera Repos" at "http://repository.cloudera.com/artifactory/cloudera-repos/",
"Spark-hbase" at "https://repository.cloudera.com/artifactory/repo/com/cloudera/spark-hbase/0.0.1-clabs/"
)

libraryDependencies ++= Seq(
	"org.apache.hbase" % "hbase-client" % "0.98.6-cdh5.2.1",
	"org.apache.hbase" % "hbase-common" % "0.98.6-cdh5.2.1",
    "org.apache.hbase" % "hbase-hadoop2-compat" % "0.98.6-cdh5.2.1" % "provided",
    "org.apache.hbase" % "hbase-protocol" % "0.98.6-cdh5.2.1" % "provided",
    "org.apache.hbase" % "hbase-server" % "0.98.6-cdh5.2.1" % "provided",
    "org.apache.spark" % "spark-core_2.10" % "1.1.0-cdh5.2.1" % "provided",
    "org.apache.spark" % "spark-mllib_2.10" % "1.1.0-cdh5.2.1" % "provided" ,
    "org.apache.spark" % "spark-streaming_2.10" % "1.1.0-cdh5.2.1" % "provided",
    "org.apache.spark" %% "spark-streaming-twitter" % "1.1.0-cdh5.2.1",
    "org.twitter4j" % "twitter4j-core" % "3.0.6",
    "com.cloudera" % "spark-hbase" % "0.0.1-clabs",
    "org.twitter4j" % "twitter4j" % "3.0.6"
)

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.first // Changed deduplicate to first
    }
    case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.first
    case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
    case PathList("org", "jboss", xs @ _*) => MergeStrategy.first
    
    case "about.html"  => MergeStrategy.rename
    case "reference.conf" => MergeStrategy.concat
    case _ => MergeStrategy.first
  }
}
