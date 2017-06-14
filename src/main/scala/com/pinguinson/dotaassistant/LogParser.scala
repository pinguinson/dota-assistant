package com.pinguinson.dotaassistant

import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * Dota log parser
  */
object LogParser {
  /**
    * Extracts 10 player IDs from the most recent lobby log
    *
    * @param path Path to the log file
    */
  def getLobbyPlayers(path: String): Option[List[String]] = {

    def extractPlayerIds(str: String): List[String] = str.split("\\[U:1:").map(_.takeWhile(_ != ']')).slice(1, 11).toList

    val logs: Seq[String] = Try(Source.fromFile(path).getLines.toList.reverse) match {
      case Success(lines) =>
        lines
      case Failure(ex) =>
        // file was not found
        println(ex)
        List.empty[String]
    }
    val lastKnownLobby = logs.find(_.contains("Lobby"))

    lastKnownLobby.map(extractPlayerIds)
  }
}
