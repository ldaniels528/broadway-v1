package com.ldaniels528.broadway.core.util

/**
 * Text File Helper Utility class
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object TextFileHelper {

  /**
   * Parses a line of character delimited text
   * @param line the given line of delimited line
   * @param splitter the given splitter expression (e.g. "[,]")
   * @return a list of string tokens
   */
  def parseTokens(line: String, splitter: String): List[String] = line.split(splitter).toList

  /**
   * Parses a line of CSV text
   * @param line the given line of CSV
   * @return a list of string tokens
   */
  def parseCSV(line: String): List[String] = {
    val sb = new StringBuilder()
    var inQuotes = false

    // extract the tokens
    val list = line.foldLeft[List[String]](Nil) { (list, ch) =>
      val result: Option[String] = ch match {
        // quoted text
        case '"' =>
          inQuotes = !inQuotes
          None

        // comma (unquoted)?
        case c if c == ',' && !inQuotes =>
          if (sb.nonEmpty) {
            val s = sb.toString()
            sb.clear()
            Option(s)
          } else None

        // any other character
        case c =>
          sb += c
          None
      }

      result map (_ :: list) getOrElse list
    }

    // add the last token
    (if (sb.nonEmpty) sb.toString :: list else list).reverse
  }

}
