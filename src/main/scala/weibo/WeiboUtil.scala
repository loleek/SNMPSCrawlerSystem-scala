package weibo

import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.mutable.Buffer
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicNameValuePair
import org.jsoup.Jsoup
import weibo.WeiboAccountManager.WeiboAccount
import org.apache.http.client.config.RequestConfig
import java.io.IOException

/**
 * Created by dk on 2015/6/15.
 */
object WeiboUtil {

  def getContent(client: CloseableHttpClient, url: String): String = {
    val request = new HttpGet(url)
    request.setConfig(config)
    val response = client.execute(request)
    try {
      val in = response.getEntity.getContent

      var bytes = new Array[Byte](8192)
      var len = 1
      var s = ""
      while (len > 0) {
        len = in.read(bytes)
        if (len > 0)
          s = s + new String(bytes, 0, len)
      }
      s
    } catch {
      case e: IOException => throw e
    } finally {
      response.close()
    }
  }

  def login(client: CloseableHttpClient, account: WeiboAccount): Unit = {
    try {
      val loginpage = getContent(client, login_url)
      val location = postForm(client, loginpage, account)
      val homepage = getContent(client, location)
      gotoTouchPage(client, homepage)
    } catch {
      case e: IOException => throw e
    }
  }

  private def postForm(client: CloseableHttpClient, loginpage: String, account: WeiboAccount): String = {
    val doc = Jsoup.parse(loginpage)
    val element = doc.getElementsByTag("form").get(0)
    val formurl = element.attr("action")
    val es = element.getElementsByTag("input")
    val bf = Buffer.empty[BasicNameValuePair]
    val it = es.iterator()
    while (it.hasNext) {
      val e = it.next()
      val attr = e.attr("name")
      if (attr.contains("mobile"))
        bf += new BasicNameValuePair(attr, account.name)
      else if (attr.contains("password"))
        bf += new BasicNameValuePair(attr, account.password)
      else if (attr.contains("remember"))
        bf += new BasicNameValuePair(attr, "on")
      else
        bf += new BasicNameValuePair(attr, e.attr("value"))
    }

    val post = new HttpPost(form_url_prefix + formurl)
    post.setEntity(new UrlEncodedFormEntity(bf))

    val response = client.execute(post)

    val iterator = response.headerIterator

    var location = ""
    while (iterator.hasNext) {
      val header = iterator.nextHeader()
      if (header.getName == "Location")
        location = header.getValue
    }

    location
  }

  private def gotoTouchPage(client: CloseableHttpClient, homepage: String): Unit = {
    val doc = Jsoup.parse(homepage)
    val elements = doc.getElementsByClass("c")
    var touchurl = ""
    val it = elements.listIterator()
    while (it.hasNext) {
      val e = it.next()
      val children = e.children().iterator()
      while (children.hasNext) {
        val ec = children.next()
        if (ec.text() == "触屏")
          touchurl = ec.attr("href")
      }
    }

    //    println(getContent(client, touchurl))
    val get = new HttpGet(touchurl)
    client.execute(get).close()
  }

  private val login_url = "http://login.weibo.cn/login/?ns=1&revalid=2&backURL=http%3A%2F%2Fweibo.cn%2F%3Frl%3D1&backTitle=%CE%A2%B2%A9&vt="
  private val form_url_prefix = "http://login.weibo.cn/login/"
  val config = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(5000).build()

}
