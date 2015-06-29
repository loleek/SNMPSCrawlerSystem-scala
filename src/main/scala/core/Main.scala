package core

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.io.IO
import spray.can.Http
import weibo.WeiboMessages.Start
import com.typesafe.config.ConfigFactory
import weibo.WeiboCrawlerMasterBackend
import weibo.WeiboClawlerSlaveBackend
import weibo.WeiboMessages.StatusQuery

object Main {
  def main(args: Array[String]): Unit = {
    args(0) match {
      case "master" => {
        implicit val system = ActorSystem("SNMPSCrawlerSystem",ConfigFactory.load())

        val service = system.actorOf(Props[SNMPSCrawlerSystemHttpService], "cs-service")
        val master = system.actorOf(Props[WeiboCrawlerMasterBackend], "master")

        IO(Http) ! Http.Bind(service, "SNMPS", port = 12316)
      }
      case "slave" => {
        val system = ActorSystem("SNMPSCrawlerSystem")

        val slave = system.actorOf(Props[WeiboClawlerSlaveBackend], "slave")
        slave ! Start
      }
      case "test" => {
        implicit val system = ActorSystem("SNMPSCrawlerSystem")

        val service = system.actorOf(Props[SNMPSCrawlerSystemHttpService], "cs-service")
        val master = system.actorOf(Props[WeiboCrawlerMasterBackend], "master")

        val slave = system.actorOf(Props[WeiboClawlerSlaveBackend], "slave")
        slave ! Start

        IO(Http) ! Http.Bind(service, "localhost", port = 12316)
      }
    }
  }
}