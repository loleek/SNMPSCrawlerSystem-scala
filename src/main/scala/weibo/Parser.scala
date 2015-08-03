package weibo
import weibo.WeiboMessages.DataPageContent
import weibo.WeiboMessages.DataResult
import weibo.WeiboMessages.PageParseError
import akka.actor.Actor
import com.fasterxml.jackson.core.JsonParseException
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jvalue2extractable
import org.json4s.string2JsonInput
import weibo.WeiboModel._

/**
 * @author dk
 */
class Parser extends Actor {
  def receive = {
    case DataPageContent(tag, content) => {
      tag match {
        case "tweet" => {
          try {
            implicit val formats = DefaultFormats
            val jsonText = parse(content)
            val tp = jsonText.extract[TweetPage]

            val result = tp.cards(0).card_group match {
              case Some(list: List[TweetCardgroup]) => {
                list.map { tc => WeiboToString(tc.mblog) }
              }
              case None => {
                List.empty[String]
              }
            }

            sender ! DataResult(result.mkString("\n"))
          } catch {
            case _: JsonParseException => {
              sender ! PageParseError
            }
          }
        }
        case "follow" => {
          try {
            implicit val formats = DefaultFormats
            val jsonText = parse(content)
            val fp = jsonText.extract[FollowPage]

            val result = fp.cards(0).card_group match {
              case Some(list) => {
                list.map { fu => fu.user.id }.mkString(",")
              }
              case None => {
                ""
              }
            }

            sender ! DataResult(result)
          } catch {
            case _: JsonParseException => {
              sender ! PageParseError
            }
          }
        }
      }
    }
  }
  def WeiboToString(weibo: Weibo): String = {
    val mid = weibo.mid
    val text = weibo.text
    val uid = weibo.user.id
    val name = weibo.user.screen_name
    val source = weibo.source
    val time = weibo.created_at
    val rid = weibo.retweeted_status match {
      case Some(retweet) => retweet.mid
      case None          => "null"
    }
    val pid = weibo.pid.getOrElse("null")
    val reposts_count = weibo.reposts_count
    val comments_count = weibo.comments_count
    val attitudes_count = weibo.attitudes_count
    val pic = weibo.pic_ids.mkString("[", ",", "]")
    val topic = weibo.topic_struct.map(_.topic_title).mkString(" ").trim match {
      case ""=>"null"
      case other:String=>other
    }
    s"${mid}\t${text}\t${uid}\t${name}\t${source}\t${time}\t${rid}\t${pid}\t${reposts_count}\t${comments_count}\t${attitudes_count}\t${pic}\t${topic}"
  }
}