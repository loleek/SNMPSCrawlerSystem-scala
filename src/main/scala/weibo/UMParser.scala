package weibo

import akka.actor.Actor
import weibo.WeiboMessages.FirstPageContent
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jvalue2extractable
import org.json4s.string2JsonInput
import com.fasterxml.jackson.core.JsonParseException
import weibo.WeiboModel.TweetPageInfo
import weibo.WeiboModel.FollowPageInfo
import weibo.WeiboMessages.PageInfo
import weibo.WeiboMessages.PageParseError
/**
 * @author dk
 */
class UMParser extends Actor {
  def receive = {
    case FirstPageContent(tweetcontent, followcontent) => {
      try {
        implicit val formats = DefaultFormats

        val tweetjson = parse(tweetcontent)
        val followjson = parse(followcontent)
        val tweetpage=tweetjson.extract[TweetPageInfo]
        val followpage=followjson.extract[FollowPageInfo]
        
        val tc=tweetpage.count match {
          case Some(i)=>i/10+1
          case None=>0
        }
        
        val fc=followpage.count match {
          case Some(i)=>i/10+1
          case None=>0
        }
        
        sender ! PageInfo(tc,fc)
      } catch {
        case _: Exception => {
          sender ! PageParseError
        }
      }
    }
  }
}