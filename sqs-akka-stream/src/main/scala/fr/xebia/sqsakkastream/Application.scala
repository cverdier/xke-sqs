package fr.xebia.sqsakkastream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
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

  val conf = ConfigFactory.load()
  implicit val system = ActorSystem("xke-sqs", conf)
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  val sqs = AmazonSQSClientBuilder.standard
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withRegion(EU_WEST_2)
    .build

  val queueUrl = "xke-test"

  def delayedLogging(message: Message): Future[Message] = {
    val promise = Promise[Message]()
    system.scheduler.scheduleOnce(2 seconds)(promise.complete(Success(message)))
    promise.future.map { res =>
      system.log.info(s"(${res.getMessageId}) ${res.getBody}")
      res
    }
  }

  def deleteMessages(messages: Seq[Message]): Try[Int] = {
    if (messages.isEmpty) Success(0) else {
      Try(sqs.deleteMessageBatch(
        queueUrl,
        messages.map(m => new DeleteMessageBatchRequestEntry(m.getMessageId, m.getReceiptHandle))
      )).map(_.getSuccessful.size)
    }
  }

  Source.fromIterator(() => Iterator.continually(sqs.receiveMessage(queueUrl).getMessages.toList))
    .mapConcat(identity)
    .mapAsync(4)(delayedLogging)
    .groupedWithin(10, 5 seconds)
    .map(deleteMessages)
    .runWith(Sink.ignore)
    .onComplete { done =>
      system.log.info(s"Completed : $done")
      system.terminate()
    }


  system.registerOnTermination { System.exit(0) }
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run() = { system.terminate() }
  })
}
