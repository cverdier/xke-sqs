package fr.xebia.sqsakkastream

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions.EU_WEST_2
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.{DeleteMessageBatchRequestEntry, Message}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.postfixOps
import scala.util.{Success, Try}


object Application extends App {

  val queueUrl = "xke-lorem"

  val conf = ConfigFactory.load()
  implicit val system = ActorSystem("xke-sqs-delayed", conf)
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withAutoFusing(false))

  implicit val ec: ExecutionContext = system.dispatcher

  // The SQS Client
  val sqs = AmazonSQSClientBuilder.standard
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withRegion(EU_WEST_2)
    .build

  // The Stream
  Source.fromIterator(() => receiveMessages)
    .mapConcat(identity)
    .mapAsync(4)(delayedLogging)
    .groupedWithin(10, 2 seconds)
    .map(deleteMessages)
    .runWith(Sink.ignore)

  // The Source is an Iterator to SQS, it is continuous but the Long Polling on the SQS Queue avoids too frequent requests
  def receiveMessages: Iterator[List[Message]] = {
    Iterator.continually {
      system.log.info("Polling for messages")
      sqs.receiveMessage(queueUrl).getMessages.toList
    }
  }

  // We simulate an API Call with some delay
  def delayedLogging(message: Message): Future[Message] = {
    val promise = Promise[Message]()
    system.scheduler.scheduleOnce(250 millis)(promise.complete(Success(message)))
    promise.future.map { res =>
      system.log.info(s"(${res.getMessageId}) ${res.getBody}")
      res
    }
  }

  // We delete the consumed messages if there are any
  def deleteMessages(messages: Seq[Message]): Try[Int] = {
    system.log.info(s"Deleting ${messages.size} messages")
    if (messages.isEmpty) Success(0) else {
      Try(sqs.deleteMessageBatch(
        queueUrl,
        messages.map(m => new DeleteMessageBatchRequestEntry(m.getMessageId, m.getReceiptHandle))
      )).map(_.getSuccessful.size)
    }
  }

  system.registerOnTermination { System.exit(0) }
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run() = { system.terminate() }
  })
}
