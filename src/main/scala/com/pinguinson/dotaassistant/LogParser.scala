package com.pinguinson.dotaassistant

import scala.io.Source
/**
  * Dota log parser
  */
object LogParser {
  /**
    * Extracts 10 player IDs from the most recent lobby log
    * @param path Path to the log file
    */
  def getLobbyPlayers(path: String): Option[List[String]] = {

    def extractPlayerIds(str: String): List[String] = str.split("\\[U:1:").map(_.takeWhile(_ != ']')).slice(1, 11).toList

    val logs = Source.fromFile(path).getLines.toList.reverse
    val lastKnownLobby = logs.find(_.contains("Lobby"))

    lastKnownLobby.map(extractPlayerIds)
  }
}
