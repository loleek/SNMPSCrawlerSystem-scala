package weibo

import java.net.InetAddress
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import webapi.WebApi.ApiData
import weibo.WeiboAccountManager.WeiboAccount
import weibo.WeiboAccountManager.WeiboAccount
import weibo.WeiboAccountManager.WeiboAccountRequest
import weibo.WeiboAccountManager.WeiboAccountRequest
import weibo.WeiboAccountManager.WrongWeiboAccount
import weibo.WeiboAccountManager.NoneWeiboAccount
import weibo.WeiboMessages.DataPageContent
import weibo.WeiboMessages.DataResult
import weibo.WeiboMessages.MasterInfo
import weibo.WeiboMessages.PageParseError
import weibo.WeiboMessages.ShutDown
import weibo.WeiboMessages.CrawlerStatus
import weibo.WeiboUtil._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.Await

/**
 * @author dk
 */
class Spider extends Actor {

  val parser = context.actorOf(Props[Parser], "parser")

  var um: ActorRef = null
  var atm: ActorRef = null
  var webapi: ActorRef = null
  var currentAccount: WeiboAccount = null
  var client: CloseableHttpClient = null
  val hostname = InetAddress.getLocalHost.getHostName

  var currentTag = ""
  var currentUrl = ""

  val regex = """100505(\d+)""".r

  var initialized = false

  var status = 0

  val system = context.system
  import system.dispatcher
  context.system.scheduler.schedule(10 seconds, 10 seconds, new Runnable() {
    override def run(): Unit = {
      context.parent ! CrawlerStatus(status)
    }
  })
  def receive = {
    case MasterInfo(urlmanagerRef, accountmanagerRef, webapi) => {
      this.um = urlmanagerRef
      this.atm = accountmanagerRef
      this.webapi = webapi

      atm ! WeiboAccountRequest(hostname)
    }
    case account @ WeiboAccount(name, pass) => {

      if (initialized)
        Thread.sleep(3 * 60 * 1000)

      this.currentAccount = account
      client = HttpClients.createDefault()
      try {
        login(client, account)
        um ! UrlRequest(hostname)

        initialized = true
      } catch {
        case _: Exception => {
          atm ! WrongWeiboAccount(hostname)
          this.currentAccount = null
        }
      }

    }
    case NoneWeiboAccount => {
      context.parent ! ShutDown
    }
    case WeiboNormalUrl(tag, url) => {
      try {
        this.currentTag = tag
        this.currentUrl = url
        Thread.sleep(4000)

        val future = Future {
          getContent(client, url)
        }
        
        val result=Await.result(future, 10 seconds)
        parser ! DataPageContent(tag, result)

        status = 2
      } catch {
        case _: Exception => {
          self ! PageParseError
        }
      }
    }
    case DataResult(content) => {
      currentTag match {
        case "tweet" => {
          webapi ! ApiData(currentTag, content)
          um ! WeiboUrlFinished(currentUrl)
        }
        case "follow" => {
          val r = regex.findFirstIn(currentUrl).get
          val regex(uid) = r

          val result = s"${uid}\t${content}"
          webapi ! ApiData(currentTag, result)
          um ! WeiboUrlFinished(currentUrl)
        }
      }

      status = 3
    }
    case PageParseError => {
      atm ! WrongWeiboAccount(hostname)
      this.currentAccount = null

      um ! RetreatWeiboUrl(currentTag, currentUrl)
      currentTag = ""
      currentUrl = ""

      status = 1
    }
  }
}