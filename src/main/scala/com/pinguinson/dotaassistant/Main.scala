package com.pinguinson.dotaassistant

import java.io.File

import com.pinguinson.dotaassistant.scraper.DotaAPI

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by pinguinson on 6/10/2017.
  */
object Main extends App {
  val pathString = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\game\\dota\\server_log.txt"
  val path = new File(pathString).toURI.toURL

  val api = new DotaAPI()
  val players = LogParser.getLobbyPlayers(path)
  players match {
    case None =>
      println("No lobby found. Are you sure you provided path to a valid log file?")
    case Some(list) =>
      println("Lobby found, wait...")
      val result = Await.result(api.fetchMatchPlayersInfo(list), 20 seconds)
      result foreach { playerGames =>
        val groupedResults = playerGames.groupBy(_.hero).mapValues(_.length)
        println(s"Player #${playerGames.head.userId}:")
        groupedResults foreach {
          case (hero, 1) =>
            println(s"Hero #$hero: 1 game played")
          case (hero, matches) =>
            println(s"Hero #$hero: $matches game(s) played")
        }
      }
  }
}
