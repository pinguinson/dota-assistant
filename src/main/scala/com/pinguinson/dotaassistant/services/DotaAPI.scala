package com.pinguinson.dotaassistant.services

import com.pinguinson.dotaassistant.config.DotaApiConfig.config
import com.pinguinson.dotaassistant.models.Players._
import com.pinguinson.dotaassistant.models.{Heroes, Outcomes, UserGameInfo, UserHeroPerformance}
import dispatch.{Http, url}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * Created by pinguinson on 6/11/2017.
  */
class DotaAPI(apiKey: String) extends Statistics {

  private[this] lazy val browser = JsoupBrowser()

  private case class MatchField(match_id: Long, lobby_type: Int)

  private case class PlayerField(account_id: Long, hero_id: Int, kills: Int, deaths: Int, assists: Int) {
    def kda = s"$kills/$deaths/$assists"
  }

  def fetchUserRecentGames(userId: String): Future[Seq[UserGameInfo]] = {
    val params = Map(
      "key" -> apiKey,
      "account_id" -> userId
    )

    val request = url(config.endpoints.matchHistory) <<? params

    Http.default(request) flatMap { response =>
      val json = response.getResponseBody
      val cursor = parse(json).getOrElse(Json.Null).hcursor
      val matches = cursor.downField("result").get[List[MatchField]]("matches").getOrElse(List.empty)
      val detailsList = matches.filter { m =>
        config.validLobbyTypes contains m.lobby_type
      } take config.maxRecentGames map { m =>
        getMatchDetails(userId, m.match_id.toString)
      }
      Future.sequence(detailsList)
    }
  }

  /**
    * Fetch match details from Dota API
    *
    * @param userId  user ID
    * @param matchId match ID
    * @return a future containing UserGameInfo
    */
  def getMatchDetails(userId: String, matchId: String): Future[UserGameInfo] = {

    def getMatchDetailsAux(userId: String, matchId: String, retries: Int, maxRetries: Int): Future[UserGameInfo] = {
      // TODO: switch to Option/Either
      if (retries >= maxRetries) {
        Future.successful(UserGameInfo(UnknownPlayer, "", Outcomes.Loss, ""))
      } else {
        getOptionalMatchDetails(userId, matchId) flatMap {
          case None =>
            getMatchDetailsAux(userId, matchId, retries + 1, maxRetries)
          case Some(userGameInfo) =>
            Future.successful(userGameInfo)
        }
      }
    }

    def getOptionalMatchDetails(userId: String, matchId: String): Future[Option[UserGameInfo]] = {
      val params: Map[String, String] = Map(
        "key" -> apiKey,
        "match_id" -> matchId
      )
      val request = url(config.endpoints.matchDetails) <<? params

      Http.default(request) map { response =>
        val body = response.getResponseBody

        val optionalJson = parse(body).toOption

        optionalJson map { json =>
          val cursor = json.hcursor

          val radiantVictory = cursor.downField("result").get[Boolean]("radiant_win").getOrElse(true)
          val players = cursor.downField("result").get[List[PlayerField]]("players").getOrElse(List.empty)

          val playedForRadiant = players.indexWhere(_.account_id == userId.toLong) < 5
          val requiredPlayer = players.find(_.account_id == userId.toLong).getOrElse(PlayerField(0, 0, 0, 0, 0))

          val result = if (radiantVictory == playedForRadiant) {
            Outcomes.Victory
          } else {
            Outcomes.Loss
          }

          val heroName = Heroes(requiredPlayer.hero_id)
          UserGameInfo(IdentifiedPlayer(userId), heroName, result, requiredPlayer.kda)
        }
      }
    }

    // Retry at most config.maxRetries times
    getMatchDetailsAux(userId, matchId, 0, config.maxRetries)
  }

  def fetchUserMostPlayedHeroes(userId: String, n: Int): Future[Seq[UserHeroPerformance]] = {
    Future {
      Try {
        val doc = browser.get(s"https://www.dotabuff.com/players/$userId/heroes")
        val entries = doc >> elementList("section > article > table > tbody > tr") >> elementList("td")
        entries.map { columns =>
          val hero = columns(1) >> text("a")
          val matches = (columns(2) >> attr("data-value")).toInt
          val winrate = (columns(3) >> attr("data-value")).toDouble
          UserHeroPerformance(IdentifiedPlayer(userId), hero, matches, winrate)
        }
      } getOrElse List.empty[UserHeroPerformance] take n
    }
  }
}
