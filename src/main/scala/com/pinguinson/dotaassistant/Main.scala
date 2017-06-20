package com.pinguinson.dotaassistant

import java.io.File

import com.pinguinson.dotaassistant.services.DotaAPI
import com.pinguinson.dotaassistant.models.Heroes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by pinguinson on 6/10/2017.
  */
object Main extends App {
  val pathString = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\game\\dota\\server_log.txt"
  val path = new File(pathString).toURI.toURL

    val players = LogParser.getLobbyPlayers(path)
  players match {
    case None =>
      println("No lobby found. Are you sure you provided path to a valid log file?")
    case Some(list) =>
      println("Lobby found, wait...")
      val result = Await.result(DotaAPI.fetchMatchPlayersInfo(list), 20 seconds)
      result foreach { playerGames =>
        val groupedResults = playerGames.groupBy(_.hero).mapValues(_.length).toList.sortBy(-_._2)
        playerGames.headOption match {
          case Some(game) => println(s"Player #${game.userId}:")
          case None => println(s"Anonymous player")
        }
//        println(s"Player #${playerGames.head.userId}:")
        groupedResults foreach {
          case (hero, matches) if matches % 10 == 1 =>
            println(s"${Heroes(hero)}: $matches game played")
          case (hero, matches) =>
            println(s"${Heroes(hero)}: $matches games played")
        }
        println()
      }
  }
}
