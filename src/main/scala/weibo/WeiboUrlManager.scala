package weibo

import scala.collection.mutable.Queue
import scala.concurrent.duration.DurationInt

import TaskManager.Task
import TaskManager.TaskFinished
import TaskManager.TaskRequest
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection.toScala
import akka.actor.Props
import akka.actor.actorRef2Scala
import weibo.WeiboAccountManager.NoneWeiboAccount
import weibo.WeiboMessages.PageInfo
import weibo.WeiboMessages.ShutDown
import weibo.WeiboMessages.UrlManagerStatus
/**
 * Created by dk on 2015/6/3.
 */

class WeiboUrlManager extends Actor {
  import TaskManager._

  val tweeturl_queue = new Queue[String]()
  val followurl_queue = new Queue[String]()

  val spider = context.actorOf(Props[UMSpider], "umspider")
  val taskmanager = context.actorSelection("/user/master/taskmanager")

  val idleSpider = new Queue[ActorRef]()
  val catching_queue = new Queue[String]()

  var taskInitial = false
  var hasTask = false
  var currentTask: Task = null
  var tweetpagecount = 0
  var followpagecount = 0

  val system = context.system
  import system.dispatcher

  context.system.scheduler.schedule(5 seconds, 5 seconds, new Runnable() {
    override def run(): Unit = {
      context.parent ! UrlManagerStatus(tweetpagecount, followpagecount, tweeturl_queue.size, followurl_queue.size, catching_queue.size, idleSpider.size)
    }
  })
  def receive = {

    case UrlRequest(hostname) => {
      if (hasTask) {
        if (!tweeturl_queue.isEmpty) {
          val url = tweeturl_queue.dequeue()
          sender ! WeiboNormalUrl("tweet", url)
          catching_queue.enqueue(url)
        } else if (!followurl_queue.isEmpty) {
          val url = followurl_queue.dequeue()
          sender ! WeiboNormalUrl("follow", url)
          catching_queue.enqueue(url)
        } else
          idleSpider.enqueue(sender)
      } else if (taskInitial) {
        idleSpider.enqueue(sender)
      } else {
        taskmanager ! TaskRequest
        taskInitial = true
        idleSpider.enqueue(sender)
      }
    }

    case task @ Task(uid, taskid) => {
      currentTask = task

      spider ! uid
    }

    case PageInfo(tc, fc) => {
      for (i <- 1 to tc) {
        tweeturl_queue.enqueue(s"http://m.weibo.cn/page/json?containerid=100505${currentTask.uid}_-_WEIBO_SECOND_PROFILE_WEIBO&page=${i}")
      }
      for (i <- 1 to fc) {
        followurl_queue.enqueue(s"http://m.weibo.cn/page/json?containerid=100505${currentTask.uid}_-_FOLLOWERS&page=${i}")
      }
      val spiders = idleSpider.dequeueAll { _ => true }

      spiders.foreach { ref =>
        if (!tweeturl_queue.isEmpty) {
          val url = tweeturl_queue.dequeue()
          ref ! WeiboNormalUrl("tweet", url)
          catching_queue.enqueue(url)
        } else if (!followurl_queue.isEmpty) {
          val url = followurl_queue.dequeue()
          ref ! WeiboNormalUrl("follow", url)
          catching_queue.enqueue(url)
        } else
          idleSpider.enqueue(ref)
      }

      hasTask = true
      taskInitial = false

      tweetpagecount = tc
      followpagecount = fc

    }
    case RetreatWeiboUrl(tag, url) => {
      tag match {
        case "tweet" => {
          if (!idleSpider.isEmpty)
            idleSpider.dequeue() ! WeiboNormalUrl("tweet", url)
          else {
            catching_queue.dequeueFirst { qu => qu == url }
            tweeturl_queue.enqueue(url)
          }
        }
        case "follow" => {
          if (!idleSpider.isEmpty)
            idleSpider.dequeue() ! WeiboNormalUrl("follow", url)
          else {
            followurl_queue.enqueue(url)
            catching_queue.dequeueFirst { qu => qu == url }
          }
        }
      }
    }
    case WeiboUrlFinished(url) => {
      catching_queue.dequeueFirst { qu => qu == url }
      if (!tweeturl_queue.isEmpty) {
        val url = tweeturl_queue.dequeue()
        sender ! WeiboNormalUrl("tweet", url)
        catching_queue.enqueue(url)
      } else if (!followurl_queue.isEmpty) {
        val url = followurl_queue.dequeue()
        sender ! WeiboNormalUrl("follow", url)
        catching_queue.enqueue(url)
      } else if (!catching_queue.isEmpty) {
        idleSpider.enqueue(sender)
      } else {
        idleSpider.enqueue(sender)
        taskmanager ! TaskFinished(currentTask.uid, currentTask.taskid)

        taskInitial = true
        hasTask = false
      }
    }
    case NoneWeiboAccount => {
      context.parent ! ShutDown
    }
  }
}

case class UrlRequest(hostname: String)

case class WeiboNormalUrl(tag: String, url: String)

case class RetreatWeiboUrl(tag: String, url: String)

case class WeiboUrlFinished(url: String)
