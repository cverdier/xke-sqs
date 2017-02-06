package fr.xebia.sqsakkastream

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions.EU_WEST_2
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext

object Injector extends App {

  val data = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua Ut enim ad minim veniam quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur Excepteur sint occaecat cupidatat non proident sunt in culpa qui officia deserunt mollit anim id est laborum".split(" ")

  val queueUrl = "xke-lorem"

  val conf = ConfigFactory.load()
  implicit val system = ActorSystem("xke-sqs-injector", conf)
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withAutoFusing(false))

  implicit val ec: ExecutionContext = system.dispatcher

  // The SQS Client
  val sqs = AmazonSQSClientBuilder.standard
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withRegion(EU_WEST_2)
    .build

  Source(data.to)
    .grouped(10)
    .log("batch", words => s"Sending batch $words")
    .map(words => sqs.sendMessageBatch(queueUrl, words.map(w => new SendMessageBatchRequestEntry(UUID.randomUUID().toString, w))))
    .runFold(0)((a, res) => a + res.getSuccessful.size())
    .onComplete { done =>
      system.log.info(s"Completed : $done")
      system.terminate()
    }

  system.registerOnTermination { System.exit(0) }
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run() = { system.terminate() }
  })
}
