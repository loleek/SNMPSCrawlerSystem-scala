package weibo

import scala.collection.mutable

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import webapi.WebApi
import weibo.WeiboMessages.AccountManagerStatus
import weibo.WeiboMessages.MasterInfo
import weibo.WeiboMessages.Register
import weibo.WeiboMessages.ShutDown
import weibo.WeiboMessages.SlaveStatus
import weibo.WeiboMessages.StatusQuery
import weibo.WeiboMessages.SystemStatus
import weibo.WeiboMessages.TaskManagerStatus
import weibo.WeiboMessages.UnRegister
import weibo.WeiboMessages.UrlManagerStatus
import weibo.WeiboMessages.WebApiStatus
/**
 * Created by dk on 2015/6/3.
 */
class WeiboCrawlerMasterBackend extends Actor {

  val urlmanager = context.actorOf(Props[WeiboUrlManager], "urlmanager")
  val accountmanager = context.actorOf(Props[WeiboAccountManager], "accountmanager")
  val taskmanager = context.actorOf(Props[TaskManager], "taskmanager")

  val webapi = context.actorOf(Props[WebApi], "webapi")

  val hosts = new mutable.HashMap[String, ActorRef]()

  val hostsStatus = new mutable.HashMap[String, Int]
  var atmStatus = AccountManagerStatus(0, 0, 0, List.empty[String])
  var tmStatus = TaskManagerStatus(0, 0L, "0")
  var umStatus = UrlManagerStatus(0, 0, 0, 0, 0, 0)
  var webapiStatus = WebApiStatus(0L, 0L, 0L)

  def receive = {
    case Register(hostname) => {
      hosts += (hostname -> sender)
      sender ! MasterInfo(urlmanager, accountmanager, webapi)

      hostsStatus += (hostname -> 0)
    }
    case UnRegister(hostname) => {
      hosts -= hostname
      hostsStatus += (hostname -> -1)
      if (hosts.isEmpty)
        context.system.shutdown()
    }
    case ShutDown => {
      hosts.foreach({ pair =>
        pair._2 ! ShutDown
      })
    }
    case SlaveStatus(hostname, status) => {
      hostsStatus += (hostname -> status)
    }
    case status @ AccountManagerStatus(_, _, _, _) => {
      atmStatus = status
    }
    case status @ TaskManagerStatus(_, _, _) => {
      tmStatus = status
    }
    case status @ UrlManagerStatus(_, _, _, _, _, _) => {
      umStatus = status
    }
    case status @ WebApiStatus(_, _, _) => {
      webapiStatus = status
    }
    case StatusQuery => {
      sender ! SystemStatus(atmStatus, tmStatus, umStatus, webapiStatus, hostsStatus.toMap)
    }
  }
}
