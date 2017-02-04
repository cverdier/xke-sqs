package fr.xebia.sqsakkastream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions.EU_WEST_2
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.{DeleteMessageBatchRequestEntry, Message}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, ExecutionException, Future, Promise}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success


object Application extends App {

  val conf = ConfigFactory.load()
  implicit val system = ActorSystem("cleaner", conf)
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  val sqs = AmazonSQSClientBuilder.standard
    .withCredentials(new ProfileCredentialsProvider())
    .withRegion(EU_WEST_2)
    .build

  val queueUrl = ""

  def delayedLogging(message: Message): Future[Message] = {
    val promise = Promise[Message]()
    system.scheduler.scheduleOnce(2 seconds)(promise.complete(Success(message)))
    promise.future.map { res =>
      system.log.info(s"(${res.getMessageId}) ${res.getBody}")
      res
    }
  }

  Source.fromIterator(() => Iterator.continually(sqs.receiveMessage(queueUrl).getMessages.toList))
    .mapConcat(identity)
    .mapAsync(4)(delayedLogging)
    .groupedWithin(10, 5 seconds)
    .map(messages => sqs.deleteMessageBatch(
      queueUrl,
      messages.map(m => new DeleteMessageBatchRequestEntry(m.getMessageId, m.getReceiptHandle)))
    )
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
