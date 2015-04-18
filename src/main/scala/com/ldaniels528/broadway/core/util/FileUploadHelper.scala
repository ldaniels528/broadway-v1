package com.ldaniels528.broadway.core.util

import java.io.{File, FileInputStream, IOException}

import com.ldaniels528.commons.helpers.ResourceHelper._
import org.apache.http._
import org.apache.http.client._
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.impl.client.{BasicCookieStore, HttpClients}
import org.apache.http.impl.cookie.BasicClientCookie
import org.slf4j.LoggerFactory

/**
 * File Upload Helper
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class FileUploadHelper(url: String) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val cookieStore = createCookieStore()

  /**
   * Uploads a file to a remote SERVO Stream server
   * @param file the file to upload
   */
  def uploadFile(file: File) = {
    // create the HTTP client
    HttpClients.custom().setDefaultCookieStore(cookieStore).build() use { client =>
      // create an HTTP entity
      val httpEntity =
        MultipartEntityBuilder.create()
          .addPart(file.getName, new InputStreamBody(new FileInputStream(file), file.getName))
          .build()

      // create an HTTP POST containing the file data
      val httpPost = new HttpPost(url)
      httpPost.addHeader("Accept", "*/*")
      httpPost.setEntity(httpEntity)

      // perform the file upload
      client.execute(httpPost, new ResponseHandler[String] {

        @throws[ClientProtocolException]
        @throws[IOException]
        override def handleResponse(response: HttpResponse): String = {
          val sl = response.getStatusLine
          if (sl != null) {
            val reason = sl.getReasonPhrase
            val statusCode = sl.getStatusCode
            logger.info("Response: %s => reason '%s', statusCode %d".format(file.getName, reason, statusCode))
          }
          null
        }
      });
    }
  }

  /**
   * Creates a new cookie store instance containing the necessary
   * key-value pairs for authentication
   * @return a new [[CookieStore]] instance
   */
  def createCookieStore(): CookieStore = {
    val cookieStore = new BasicCookieStore()
    //cookieStore.addCookie(makeCookie("user_email", "lawrence.daniels@gmail.com"));
    //cookieStore.addCookie(makeCookie("user_password", 1234567"));
    cookieStore
  }

  /**
   * Creates a new cookie containing the given name-value pair
   * @param name the name of the cookie entry
   * @param value the value of the cookie entry
   * @return a new [[org.apache.http.cookie.Cookie]]
   */
  private def makeCookie(name: String, value: String, cookieDomain: Option[String] = None) = {
    val cookie = new BasicClientCookie(name, value)
    cookieDomain foreach cookie.setDomain
    //cookie.setPath("/something/");
    cookie
  }

}
