package org.invati.twitter

import twitter4j.Status
import org.apache.log4j.Logger
import org.apache.log4j.Level
import org.apache.spark.storage.StorageLevel
import scala.io.Source
import scala.collection.mutable.HashMap
import java.io.File
import org.apache.log4j.Logger
import org.apache.log4j.Level
import sys.process.stringSeqToProcess
import twitter4j.auth.Authorization
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.client.Put
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import org.apache.spark.SparkConf
import com.cloudera.spark.hbase.HBaseContext
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.StreamingContext._
						

object SparkTwitterConnector {
  
  /** Configures the Oauth Credentials for accessing Twitter */
  def configureTwitterCredentials(apiKey: String, apiSecret: String, accessToken: String, accessTokenSecret: String) {
    val configs = new HashMap[String, String] ++= Seq(
      "apiKey" -> apiKey, "apiSecret" -> apiSecret, "accessToken" -> accessToken, "accessTokenSecret" -> accessTokenSecret)
    println("Configuring Twitter OAuth")
    configs.foreach{ case(key, value) =>
        if (value.trim.isEmpty) {
          throw new Exception("Error setting authentication - value for " + key + " not set")
        }
        val fullKey = "twitter4j.oauth." + key.replace("api", "consumer")
        System.setProperty(fullKey, value.trim)
        println("\tProperty " + fullKey + " set as [" + value.trim + "]")
    }
    println()
  }
  
  def inferSentiment(Tweet: String): Int ={
    //TODO Write the method that actually infers sentiment and gives result on a scale of 5.
    (0)
  } 
  
  def main(args: Array[String]): Unit = {
     // Checkpoint directory
    val checkpointDir = "/user/root/checkpoint/"
    
    //set log level to warn..dont want too much stuff on console 
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.WARN)


    // Configure Twitter credentials. These are my twitter app stuff..will be great if you use your own.
    //bad code: should be arsed as arguments..hard coding is always bad 
    val apiKey = "oq8MrbMPQhIQMhd8zMUJDjb5F"
    val apiSecret = "glbo9VFdwqFMo2Ss02BmVCcAYGBbrO3ziPAwBwsdaZ5C5t6EWH"
    val accessToken = "78141274-eYJTzUiTF1BKL1tfZpcmnrWIMX4x8EH8ktCQJOw5i"
    val accessTokenSecret = "Q8VlQqOZd6xY2vL0uFI8OH29giNPvsOCPv2lQThkhgsLG"
    configureTwitterCredentials(apiKey, apiSecret, accessToken, accessTokenSecret)
   //creating a sparkContext
    val sc = new SparkContext(new SparkConf())
    
    //creating a new streaming context
    val ssc = new StreamingContext(sc,Seconds(10))
    
    //get tweets from twitter with no filters. should use some filter.. IOT?
    val tweets = TwitterUtils.createStream(ssc, None).filter(_.getLang() == "en")
    //From each tweet we are interested in three things location, text and id of the tweet.  
    //Take id, text as pair to insert in a table
    val requiredDetails = tweets.map(status => 
      if(status.getGeoLocation != null)
      (status.getId, status.getText, status.getGeoLocation.getLatitude, status.getGeoLocation.getLongitude)
      else
       (status.getId, status.getText,-1.0,-1.0)
      )
    
    
    //create Hbase configuration
    val conf = HBaseConfiguration.create()
    //create HbaseContext object that will be gateway for hbase
    val hbaseContext = new HBaseContext(sc, conf)
    //pushing tweets into hbase (id, tweet text, latitude, longitude) in to table called twit
    hbaseContext.streamBulkPut[(Long,String,Double,Double)](requiredDetails, "twit", 
        (detail) => {
          if(detail!=null){
          val put = new Put(Bytes.toBytes(detail._1))
          put.add(Bytes.toBytes("tweets"), Bytes.toBytes("id"), Bytes.toBytes(detail._1))
          put.add(Bytes.toBytes("tweets"), Bytes.toBytes("tweet"), Bytes.toBytes(detail._2))
          put.add(Bytes.toBytes("tweets"), Bytes.toBytes("latitude"),Bytes.toBytes(detail._3))
          put.add(Bytes.toBytes("tweets"), Bytes.toBytes("longitude"), Bytes.toBytes(detail._4))
        }else{null}
          }
        , false)
        
    //identify trends based on hash tags
    val hashTags = tweets.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))
    
    //calculate top hashtags in last 5 minutes
    val topCounts5 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(600))
                     .map{case (topic, count) => (count, topic)}
                     .transform(_.sortByKey(false))

    val topCounts1 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(60))
                     .map{case (topic, count) => (count, topic)}
                     .transform(_.sortByKey(false))


    // Print popular hashtags
    topCounts5.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\n\n\nPopular topics in last 2 minutes (%s total):".format(rdd.count()))
      topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
    })

    topCounts1.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\n\n\nPopular topics in last 60 seconds (%s total):".format(rdd.count()))
      topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
    })

        
    //TODO figure out sentiment in a numeric form   
    //TODO get location separately with id and sentiment
    //TODO insert (id, text, location) in one column family and (id, location, sentiment) in another family
    
    
    ssc.checkpoint(checkpointDir)
    //start of the streaming process. all code should be above this point
    ssc.start()
    //For test use ctrl-C to terminate the streaming
    ssc.awaitTermination()

  }

}