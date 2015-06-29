package weibo

/**
 * @author dk
 */
object WeiboModel {
  case class TweetPageInfo(count: Option[Int])
  case class FollowPageInfo(count: Option[Int])

  case class TweetPage(cards: List[TweetCard])
  case class TweetCard(card_group: Option[List[TweetCardgroup]])
  case class TweetCardgroup(mblog: Weibo)
  case class Weibo(created_at: String, mid: String, text: String, source: String, pic_ids: List[String], user: User, pid: Option[String], retweeted_status: Option[ReWeibo], reposts_count: Int, comments_count: Int, attitudes_count: Int, topic_struct: List[Topic])
  case class ReWeibo(mid: String)
  case class User(id: String, screen_name: String)
  case class Topic(topic_title: String)
  
  case class FollowPage(cards: List[FollowCard])
  case class FollowCard(card_group : Option[List[FollowUser]])
  case class FollowUser(user:User)
}