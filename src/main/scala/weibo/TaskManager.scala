package weibo

import scala.collection.mutable.Queue
import akka.actor.Actor
import scala.io.Source
import java.io.File
import java.io.PrintWriter
import weibo.WeiboMessages.ShutDown
import webapi.WebApi.PersistData
import scala.concurrent.duration._
import weibo.WeiboMessages.TaskManagerStatus
import weibo.WeiboMessages.TaskManagerStatus

/**
 * @author dk
 */

object TaskManager {
  case object TaskRequest

  case class Task(uid: String, taskid: Long)

  case class TaskFinished(uid: String, taskid: Long)
}

class TaskManager extends Actor {
  import TaskManager._

  val idlist = new Queue[String]()

  val webapiRef = context.actorSelection("/user/master/webapi")

  var currentTask = Task("", 0L)

  val system = context.system
  import system.dispatcher
  context.system.scheduler.schedule(5 seconds, 5 seconds, new Runnable() {
    override def run(): Unit = {
      context.parent ! TaskManagerStatus(idlist.size, currentTask.taskid, currentTask.uid)
    }
  })
  override def preStart() {
    val file = new File("weibotask" + File.separatorChar + "userids.txt")
    Source.fromFile(file).getLines().foreach { uid => idlist.enqueue(uid) }
  }

  override def postStop() {
    if (currentTask != null)
      idlist.enqueue(currentTask.uid)
    val out = new PrintWriter(new File("weibotask" + File.separatorChar + "remained-userids.txt"))
    idlist.foreach { uid => out.println(uid) }
    out.flush()
    out.close()
  }

  def receive = {
    case TaskRequest => {
      if (idlist.isEmpty)
        context.parent ! ShutDown
      val task = Task(idlist.dequeue(), System.currentTimeMillis())
      currentTask = task

      sender ! task
    }
    case TaskFinished(uid, taskid) => {

      webapiRef ! PersistData

      if (idlist.isEmpty)
        context.parent ! ShutDown
      val task = Task(idlist.dequeue(), System.currentTimeMillis())
      currentTask = task

      sender ! task
    }
  }
}