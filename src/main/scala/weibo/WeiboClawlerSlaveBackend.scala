package weibo

import java.net.InetAddress

import scala.concurrent.duration.DurationInt

import WeiboMessages.CrawlerStatus
import WeiboMessages.MasterInfo
import WeiboMessages.Register
import WeiboMessages.ShutDown
import WeiboMessages.Start
import WeiboMessages.UnRegister
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection.toScala
import akka.actor.Props
import akka.actor.actorRef2Scala
import weibo.WeiboMessages.SlaveStatus
/**
 * Created by dk on 2015/5/31.
 */

class WeiboClawlerSlaveBackend extends Actor {

  val master = context.actorSelection("akka.tcp://SNMPSCrawlerSystem@49.122.47.30:5250/user/master")
  //  val master = context.actorSelection("/user/master")
  val hostname = InetAddress.getLocalHost.getHostName

  val spider = context.actorOf(Props[Spider], "spider")

  var status = 0

  def receive = {
    case Start => {
      master ! Register(hostname)
    }
    case masterinfo @ MasterInfo(_, _, _) => {
      spider ! masterinfo
      val system = context.system
      import system.dispatcher
      context.system.scheduler.schedule(5 seconds, 5 seconds, new Runnable() {
        override def run(): Unit = {
          master ! SlaveStatus(hostname, status)
        }
      })
    }
    case ShutDown => {
      master ! UnRegister(hostname)
      context.system.shutdown()
    }
    case CrawlerStatus(status) => {
      this.status = status
    }
  }
}

