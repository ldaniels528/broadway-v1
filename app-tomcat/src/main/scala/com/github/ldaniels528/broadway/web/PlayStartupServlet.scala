package com.github.ldaniels528.broadway.web

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.slf4j.LoggerFactory

/**
  * Play Framework 2.4.x Startup Servlet
  * @author lawrence.daniels@gmail.com
  */
class PlayStartupServlet() extends HttpServlet {
  private val logger = LoggerFactory.getLogger(getClass)

  override def init(config: ServletConfig) = {
    super.init(config)

    // startup the Play Application
    logger.info("Starting Play Application...")
    play.core.server.ProdServerStart.main(Array.empty)
  }

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {
    response.setContentType("text/html")
    response.setCharacterEncoding("UTF-8")
    response.getWriter.print(
      """
        <!DOCTYPE html>
        <html>
          <head>
            <title>Play Startup Servlet</title>
          </head>
          <body>
            <h1 style="text-align: center; color: blue">Hello World</h1>
          </body>
        </html>
      """)
  }

}
