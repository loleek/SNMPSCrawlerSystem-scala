package webapi

import java.io.File
import java.io.FileWriter
import java.util.Calendar
import java.util.GregorianCalendar

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import webapi.WebApi.ApiData
import webapi.WebApi.PersistData
import webapi.WebApi.Switch
import weibo.WeiboMessages.WebApiStatus
import weibo.WeiboMessages.WebApiStatus

/**
 * Created by dk on 2015/5/28.
 */
object WebApi {
  val refreshTime = 2 hours

  case class ApiData(tag: String, data: String)

  case object Switch

  case object PersistData

}

class WebApi extends Actor {
  var writers = new HashMap[String, FileWriter]()
  import context.dispatcher

  val dataMap = new HashMap[String, ArrayBuffer[String]]()

  var userCount = 0L
  var tweetCount = 0L
  var followCount = 0L

  context.system.scheduler.schedule(WebApi.refreshTime, WebApi.refreshTime, self, Switch)
  context.system.scheduler.schedule(5 seconds, 5 seconds, new Runnable() {
    override def run(): Unit = {
      context.parent ! WebApiStatus(userCount, tweetCount, followCount)
    }
  })
  def receive = {
    case ApiData(tag, data) => {
      if (dataMap.contains(tag)) {
        dataMap(tag) += data
      } else {
        val buffer = new ArrayBuffer[String]()
        buffer += data
        dataMap += (tag -> buffer)
      }

      tag match {
        case "tweet"  => tweetCount = tweetCount + data.split("\n").length
        case "follow" => followCount = followCount + data.split(",").length
      }

    }
    case PersistData => {
      dataMap.foreach({ pair =>
        val tag = pair._1
        val buffer = pair._2
        if (writers.contains(tag)) {
          val writer = writers(tag)
          writer.write(buffer.mkString("\n") + "\n")
          writer.flush()
        } else {
          val fileName = generateFileName(tag)
          val file = new File(fileName)
          if (file.exists()) file.delete()
          file.createNewFile()
          val writer = new FileWriter(file)

          writer.write(buffer.mkString("\n") + "\n")
          writer.flush()

          writers += new Tuple2(tag, writer)
        }
      })
      dataMap.clear()

      userCount = userCount + 1
    }
    case Switch => {
      writers.values.foreach { writer =>
        writer.flush()
        writer.close()
      }
      writers = new HashMap[String, FileWriter]()
    }
  }

  def generateFileName(tag: String) = {
    val calendar = new GregorianCalendar()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val date = calendar.get(Calendar.DATE)
    val hour = calendar.get(Calendar.HOUR)
    //    val minute=calendar.get(Calendar.MINUTE)
    //    val second=calendar.get(Calendar.SECOND)
    s"${tag}-${year}-${month}-${date}-${hour}.txt"
  }
}
