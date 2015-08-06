package weibo

import java.net.InetAddress
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import akka.actor.Actor
import akka.actor.ActorSelection.toScala
import akka.actor.Props
import weibo.WeiboAccountManager.WeiboAccount
import weibo.WeiboAccountManager.WeiboAccountRequest
import weibo.WeiboAccountManager.WrongWeiboAccount
import weibo.WeiboUtil._
import weibo.WeiboAccountManager.NoneWeiboAccount
import weibo.WeiboMessages.FirstPageContent
import weibo.WeiboMessages.PageInfo
import weibo.WeiboMessages.PageParseError
import org.apache.http.client.config.RequestConfig
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await
/**
 * @author dk
 */
class UMSpider extends Actor {
  var client: CloseableHttpClient = null
  var currentAccount: WeiboAccount = null
  var currentUid = ""
  var tweetpagecount = 0
  var followpagecount = 0

  val hostname = InetAddress.getLocalHost.getHostName

  val atm = context.actorSelection("/user/master/accountmanager")

  val parser = context.actorOf(Props[UMParser], "umparser")

  val system = context.system
  import system.dispatcher

  def receive = {
    case uid: String => {
      currentUid = uid
      if (currentAccount == null)
        atm ! WeiboAccountRequest(hostname)
      else {
        try {

          val futuretweetcontent = Future {
            getContent(client, s"http://m.weibo.cn/page/json?containerid=100505${uid}_-_WEIBO_SECOND_PROFILE_WEIBO&page=1")
          }

          val tweetcontent = Await.result(futuretweetcontent, 10 seconds)

          val futurefollowcontent = Future {
            getContent(client, s"http://m.weibo.cn/page/json?containerid=100505${uid}_-_FOLLOWERS&page=1")
          }

          val followcontent = Await.result(futurefollowcontent, 10 seconds)

          parser ! FirstPageContent(tweetcontent, followcontent)
        } catch {
          case _: Exception => {
            atm ! WrongWeiboAccount(hostname)
            this.currentAccount = null
          }
        }

      }
    }
    case account @ WeiboAccount(name, pass) => {
      currentAccount = account
      client = HttpClients.createDefault()

      try {
        login(client, account)
      } catch {
        case _: Exception => {
          atm ! WrongWeiboAccount(hostname)
          this.currentAccount = null
        }
      }

      try {
        val tweetcontent = getContent(client, s"http://m.weibo.cn/page/json?containerid=100505${this.currentUid}_-_WEIBO_SECOND_PROFILE_WEIBO&page=1")
        val followcontent = getContent(client, s"http://m.weibo.cn/page/json?containerid=100505${this.currentUid}_-_FOLLOWERS&page=1")
        parser ! FirstPageContent(tweetcontent, followcontent)
      } catch {
        case _: Exception => {
          atm ! WrongWeiboAccount(hostname)
          this.currentAccount = null
        }
      }

    }
    case NoneWeiboAccount => {
      context.parent ! NoneWeiboAccount
    }
    case pi @ PageInfo(tc, fc) => {
      context.parent ! pi
    }
    case PageParseError => {
      atm ! WrongWeiboAccount(hostname)
      this.currentAccount = null
    }
  }
}