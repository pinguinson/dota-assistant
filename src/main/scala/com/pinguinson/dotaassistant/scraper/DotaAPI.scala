package com.pinguinson.dotaassistant.scraper

import com.pinguinson.dotaassistant.model.{Results, UserGameInfo, UserHeroPerformance}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http._

/**
  * Created by pinguinson on 6/11/2017.
  */
class DotaAPI extends StatisticsAsync {

  private val matchHistoryApiEndpoint = "http://api.steampowered.com/IDOTA2Match_570/GetMatchHistory/v1"
  private val matchDetailsApiEndpoint = "http://api.steampowered.com/IDOTA2Match_570/GetMatchDetails/v1"
  private val steamApiKey = "226CBA05D5049934B32FBD997D6C537F"
  private val matchmakingModePublic = 0
  private val matchmakingModeRanked = 7

  case class Match(match_id: Long, lobby_type: Int)

  case class Player(account_id: Long, hero_id: Int, kills: Int, deaths: Int, assists: Int) {
    def kda = s"$kills/$deaths/$assists"
  }

  def fetchUserRecentGames(userId: String): Future[Seq[UserGameInfo]] = {
    val json = Http(matchHistoryApiEndpoint).params(Seq(
      "key" -> steamApiKey,
      "account_id" -> userId
    )).asString.body

    val cursor = parse(json).getOrElse(Json.Null).hcursor
    val matches = cursor.downField("result").get[List[Match]]("matches").getOrElse(List.empty)
    val detailsList = matches.filter { m =>
      m.lobby_type == matchmakingModePublic || m.lobby_type == matchmakingModeRanked
    } take 10 map { m =>
      getMatchDetails(userId, m.match_id.toString)
    }
    println(s"processed user #$userId")
    Future.sequence(detailsList)
  }

  /**
    * Fetch match details
    *
    * @param userId user ID
    * @param matchId match ID
    * @return a future containing UserGameInfo
    */
  def getMatchDetails(userId: String, matchId: String): Future[UserGameInfo] = {
    Future {
      val json = Http(matchDetailsApiEndpoint).params(Seq(
        "key" -> steamApiKey,
        "match_id" -> matchId
      )).asString.body

      val cursor = parse(json).getOrElse(Json.Null).hcursor

      val radiantVictory = cursor.downField("result").get[Boolean]("radiant_win").getOrElse(true)
      val players = cursor.downField("result").get[List[Player]]("players").getOrElse(List.empty)

      val playedForRadiant = players.indexWhere(_.account_id == userId.toLong) < 5
      val requiredPlayer = players.find(_.account_id == userId.toLong).getOrElse(Player(0, 0, 0, 0, 0))

      val result = if (radiantVictory == playedForRadiant) {
        Results.Victory
      } else {
        Results.Loss
      }
      println(s"processed user #$userId, match #$matchId")
      UserGameInfo(userId, requiredPlayer.hero_id.toString, result, requiredPlayer.kda)
    }
  }

  // TODO: implement
  def fetchUserMostPlayedHeroes(userId: String, n: Int): Future[Seq[UserHeroPerformance]] = {
    Future.successful(List.empty[UserHeroPerformance])
  }
}
