package com.pinguinson.dotaassistant.services

import cats._
import cats.data._
import cats.implicits._
import com.pinguinson.dotaassistant.config.DotaApiConfig.config
import com.pinguinson.dotaassistant.models.Exceptions._
import com.pinguinson.dotaassistant.models.Players._
import com.pinguinson.dotaassistant.models.UserReports._
import com.pinguinson.dotaassistant.models.Outcomes
import dispatch.{Http, url}
import io.circe.{DecodingFailure, HCursor, Json, Printer}
import io.circe.generic.auto._
import io.circe.parser._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
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

  /**
    * Parse response into JSON
    * @param body response body as a string
    * @return an exception if failed to parse
    * or response code was anything but 1
    */
  private def processResponseHistory(body: String): Either[ApiError, HCursor] = {
    val statusWithCursor = for {
      json <- parse(body).right
      cursor = json.hcursor
      s <- cursor.downField("result").get[Int]("status").right
    } yield (s, cursor)

    statusWithCursor match {
      //couldn't parse json
      case Left(_) if body.contains("429") =>
        Left(TooManyRequestsException)
      case Left(_) =>
        Left(AccessForbiddenException)
      case Right((1, cursor)) =>
        //all good
        Right(cursor)
      case Right((15, _)) =>
        // Cannot get match history for a user that hasn't allowed it
        Left(PrivateProfileException)
      case _ =>
        Left(UnknownException(body))
    }
  }

  private def processResponseMatchDetails(body: String): Either[ApiError, HCursor] = {
    val statusWithCursor = for {
      json <- parse(body).right
    } yield json.hcursor

    statusWithCursor match {
      case Left(_) if body.contains("429") =>
        Left(TooManyRequestsException)
      case Left(_) =>
        Left(AccessForbiddenException)
      case Right(cursor) if cursor.downField("result").get[String]("error").isRight =>
        Left(MatchNotFound)
      case Right(cursor) =>
        // all good
        Right(cursor)
    }
  }

  /**
    * Send request to Steam API
    * @param endpoint API endpoint
    * @param params query params
    * @return EitherT.right with response as a string
    */
  private def getApiResponse(endpoint: String, params: Traversable[(String,String)]): FutureEither[String] = {
    val request = url(endpoint) <<? params
    EitherT.right(Http.default(request)).map(_.getResponseBody)
  }

  def fetchUserRecentGames(userId: String): FutureEither[List[UserGameInfo]] = {

    def parseUserRecentGames(cursor: HCursor): FutureEither[List[UserGameInfo]] = {
      val matches = cursor.downField("result").get[List[MatchField]]("matches").getOrElse(List.empty)
      val detailsList = matches.filter { m =>
        config.validLobbyTypes contains m.lobby_type
      } take config.maxRecentGames map { m =>
        getMatchDetails(userId, m.match_id.toString)
      }
      detailsList.sequenceU
    }

    val params = Map(
      "key" -> apiKey,
      "account_id" -> userId
    )

    getApiResponse(config.endpoints.matchHistory, params)
      .subflatMap(body => processResponseHistory(body))
      .flatMap(cursor => parseUserRecentGames(cursor))
  }

  def getMatchDetails(userId: String, matchId: String): FutureEither[UserGameInfo] = {

    def getMatchDetailsAux(userId: String, matchId: String, attemptsMade: Int, maxAttempts: Int): FutureEither[UserGameInfo] = {
      tryToGetMatchDetails(userId, matchId) recoverWith {
        case TooManyRequestsException if attemptsMade < maxAttempts =>
          getMatchDetailsAux(userId, matchId, attemptsMade + 1, maxAttempts)
      }
    }

    def tryToGetMatchDetails(userId: String, matchId: String): FutureEither[UserGameInfo] = {

      val params: Map[String, String] = Map(
        "key" -> apiKey,
        "match_id" -> matchId
      )

      getApiResponse(config.endpoints.matchDetails, params)
        .subflatMap(body => processResponseMatchDetails(body))
        .subflatMap(cursor => parseUserGameInfo(cursor))
    }

    def parseUserGameInfo(cursor: HCursor): Either[ParsingException, UserGameInfo] = {
      val parsed = for {
        radiantVictory <- cursor.downField("result").get[Boolean]("radiant_win")
        players <- cursor.downField("result").get[List[PlayerField]]("players")

        playedForRadiant = players.indexWhere(_.account_id == userId.toLong) < 5
        requiredPlayer = players.find(_.account_id == userId.toLong).getOrElse(PlayerField(0, 0, 0, 0, 0))
        heroName = HeroService.getName(requiredPlayer.hero_id)

        result = if (radiantVictory == playedForRadiant) {
          Outcomes.Victory
        } else {
          Outcomes.Loss
        }
      } yield UserGameInfo(IdentifiedPlayer(userId), heroName, result, requiredPlayer.kda)
      parsed.left.map {
        case DecodingFailure(msg, _) => ParsingException(msg)
      }
    }

    // Try at most config.maxRetries times
    getMatchDetailsAux(userId, matchId, 0, config.maxRetries)
  }

  def fetchUserMostPlayedHeroes(userId: String, n: Int): FutureEither[List[UserHeroPerformance]] = {
    //TODO: handle errors
    val f: Future[Either[Throwable, List[UserHeroPerformance]]] = Future {
      Try {
        val doc = browser.get(s"https://www.dotabuff.com/players/$userId/heroes")
//        val entries = doc >> elementList("section > article > table > tbody > tr") >> elementList("td")
        val rows: List[Element] = doc >> elementList("section > article > table > tbody > tr")
        val entries: List[List[Element]] = rows.map(_ >> elementList("td"))
        entries.map { columns =>
          val hero = columns(1) >> text("a")
          val matches = (columns(2) >> attr("data-value")).toInt
          val winrate = (columns(3) >> attr("data-value")).toDouble
          UserHeroPerformance(IdentifiedPlayer(userId), hero, matches, winrate)
        } take n
      }.toEither
    }
    EitherT(f).leftMap(e => UnknownException(e.getMessage))
  }
}
