package core

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.duration.DurationInt
import akka.pattern.ask
import akka.util.Timeout
import spray.httpx.marshalling._
import spray.json.DefaultJsonProtocol
import spray.json.JsonFormat
import spray.routing.Directive.pimpApply
import spray.routing.HttpServiceActor
import spray.routing.HttpServiceActor
import weibo.WeiboMessages._
import scala.concurrent.Await
/**
 * @author dk
 */

object SNMPSCrawlerSYstemProtocol extends DefaultJsonProtocol {
  implicit val AccountManagerStatusFormat = jsonFormat4(AccountManagerStatus)
  implicit val TaskManagerStatusFormat = jsonFormat3(TaskManagerStatus)
  implicit val UrlManagerStatusFormat = jsonFormat6(UrlManagerStatus)
  implicit val WebApiStatusFormat = jsonFormat3(WebApiStatus)

  implicit val StatusFormat = jsonFormat5(SystemStatus)
}
class SNMPSCrawlerSystemHttpService extends HttpServiceActor {
  import SNMPSCrawlerSYstemProtocol._
  import spray.httpx.SprayJsonSupport._
  import spray.util._
  import scala.concurrent.ExecutionContext.Implicits.global

  val masterRef = context.actorSelection("/user/master")

  implicit val timeout = Timeout(3 seconds)

  def receive = runRoute {
    path("monitor") { ctx =>
      val future = masterRef ? StatusQuery
      val result = Await.result(future, 5 seconds).asInstanceOf[SystemStatus]
      ctx.complete(result)
    } ~
      path("shutdown") { ctx =>
        masterRef ! ShutDown
        ctx.complete("Command has been sent.The System will shutdown!!!")
      }
  }
}