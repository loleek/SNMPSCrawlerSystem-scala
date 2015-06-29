package weibo

import akka.actor.ActorRef

/**
 * Created by dk on 2015/5/31.
 */
object WeiboMessages {
  case object Start

  case object ShutDown

  case object SlaveBackendStart

  case class Register(hostname: String)

  case class UnRegister(hostname: String)

  case class MasterInfo(urlmanagerRef: ActorRef, accountmanagerRef: ActorRef, webapi: ActorRef)

  case class FirstPageContent(tweetcontent: String, followcontent: String)

  case class PageInfo(tweetcount: Int, followcount: Int)

  case object PageParseError

  case class DataPageContent(tag: String, content: String)

  case class DataResult(content: String)

  //以下消息用于监控爬虫状态

  case class CrawlerStatus(status: Int)

  case class SlaveStatus(hostname: String, status: Int)

  case class AccountManagerStatus(unusedSize: Int, wrongSize: Int, usingSize: Int, usingIds: List[String])

  case class TaskManagerStatus(taskListSize: Int, taskid: Long, uid: String)

  case class UrlManagerStatus(tweetPageCount: Int, followPageCount: Int, remainedTweetPageCount: Int, remianedFollowPageCount: Int, catchingSize: Int, idleSpiderCount: Int)

  case class WebApiStatus(usercount: Long, tweetcount: Long, followcount: Long)
  
  case object StatusQuery
  
  case class SystemStatus(atmStatus:AccountManagerStatus,tmStatus:TaskManagerStatus,umStatus:UrlManagerStatus,webapiStatus:WebApiStatus,slavesStatus:Map[String,Int])
}
