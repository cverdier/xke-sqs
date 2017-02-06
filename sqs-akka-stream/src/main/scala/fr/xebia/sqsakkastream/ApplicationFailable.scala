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
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object ApplicationFailable extends App {

  val queueUrl = "xke-test"

  val conf = ConfigFactory.load()
  implicit val system = ActorSystem("xke-sqs-failable", conf)
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withAutoFusing(false))

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  // The SQS Client
  val sqs = AmazonSQSClientBuilder.standard
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withRegion(EU_WEST_2)
    .build

  // The Stream
  Source.fromIterator(() => receiveMessages)
    .mapConcat(identity)
    .map(failableLogging)
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
  def failableLogging(message: Message): Try[Message] = {
    if (message.getBody.contains("p")) {
      system.log.info(s"Error (${message.getMessageId}) ${message.getBody}")
      Failure(new RuntimeException("Message contained 'p'"))
    } else {
      system.log.info(s"Success (${message.getMessageId}) ${message.getBody}")
      Success(message)
    }
  }

  // We delete the consumed messages if there are any
  def deleteMessages(messages: Seq[Try[Message]]): Try[Int] = {
    val messagesToDelete = messages.collect {
      case Success(m) => new DeleteMessageBatchRequestEntry(m.getMessageId, m.getReceiptHandle)
    }
    system.log.info(s"Deleting ${messagesToDelete.size} messages - (${messages.size - messagesToDelete.size} failed messages)")
    if (messagesToDelete.isEmpty) Success(0) else {
      Try(sqs.deleteMessageBatch(queueUrl, messagesToDelete)).map(_.getSuccessful.size)
    }
  }

  system.registerOnTermination { System.exit(0) }
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run() = { system.terminate() }
  })
}
