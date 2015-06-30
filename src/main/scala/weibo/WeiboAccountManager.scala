package weibo

import java.io.File
import java.io.PrintWriter
import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue
import scala.io.Source
import akka.actor.Actor
import weibo.WeiboAccountManager._
import scala.concurrent.duration._
import weibo.WeiboMessages.AccountManagerStatus

/**
 * Created by dk on 2015/5/28.
 */
object WeiboAccountManager {

  val accountFilePath = "weiboconf" + File.separatorChar + "account.txt"
  val worngaccountFilePath = "weiboconf" + File.separatorChar + "wrongaccount.txt"

  case class WeiboAccountRequest(hostname: String)

  case class WeiboAccount(name: String, password: String)

  case class WrongWeiboAccount(hostname: String)

  case object NoneWeiboAccount

}

class WeiboAccountManager extends Actor {

  val accountFile = new File(WeiboAccountManager.accountFilePath)
  val unused_weiboaccount_queue = new Queue[WeiboAccount]()
  val using_weiboaccount_map = new HashMap[String, WeiboAccount]()
  val hostrequesttime_map = new HashMap[String, Long]()
  val wrongaccount_queue = new Queue[WeiboAccount]()

  val system = context.system
  import system.dispatcher
  context.system.scheduler.schedule(5 seconds, 5 seconds, new Runnable() {
    override def run(): Unit = {
      context.parent ! new AccountManagerStatus(unused_weiboaccount_queue.size, wrongaccount_queue.size, using_weiboaccount_map.size, using_weiboaccount_map.values.map { _.name }.toList)
    }
  })
  def receive = {
    case WeiboAccountRequest(hostname) => {
      if (!unused_weiboaccount_queue.isEmpty) {
        val account = unused_weiboaccount_queue.dequeue()
        sender ! account
        val offertime = System.currentTimeMillis()
        using_weiboaccount_map += (hostname -> account)
        hostrequesttime_map += (hostname -> offertime)
      } else {
        sender ! NoneWeiboAccount
      }
    }
    case WrongWeiboAccount(hostname) => {
      val offertime = hostrequesttime_map(hostname)
      val currenttime = System.currentTimeMillis()
      val account = using_weiboaccount_map(hostname)
      if (currenttime - offertime <= 5 * 60 * 1000) {
        wrongaccount_queue.enqueue(account)
      } else {
        unused_weiboaccount_queue.enqueue(account)
      }
      using_weiboaccount_map -= hostname
      hostrequesttime_map -= hostname

      if (!unused_weiboaccount_queue.isEmpty) {
        val account = unused_weiboaccount_queue.dequeue()
        sender ! account
        val offertime = System.currentTimeMillis()
        using_weiboaccount_map += (hostname -> account)
        hostrequesttime_map += (hostname -> offertime)
      } else {
        sender ! NoneWeiboAccount
      }
    }
  }

  override def preStart(): Unit = {
    for (line <- Source.fromFile(accountFile).getLines()) {
      val args = line.split(" ")
      unused_weiboaccount_queue.enqueue(WeiboAccount(args(0), args(1)))
    }
  }

  override def postStop(): Unit = {
    val out = new PrintWriter(new File(WeiboAccountManager.worngaccountFilePath))
    wrongaccount_queue.toList.foreach({ account =>
      out.println(account.name + " " + account.password)
    })
    out.flush()
    out.close()
  }
}
