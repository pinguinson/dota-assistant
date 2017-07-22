package com.pinguinson.dotaassistant.services

import cats._
import cats.data._
import cats.implicits._
import com.pinguinson.dotaassistant.config.DotaApiConfig.config
import com.pinguinson.dotaassistant.models.Exceptions._
import com.pinguinson.dotaassistant.models.UserReports._
import com.pinguinson.dotaassistant.models._
import dispatch.{Http, url}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

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

  /**
    * Send request to Steam API
    * @param endpoint API endpoint
    * @param params query params
    * @return EitherT.right with response as a string
    */
  private def getApiResponse(endpoint: String, params: Traversable[(String,String)] = Traversable.empty): FutureEither[String] = {
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

    def processResponse(body: String): Either[ApiError, HCursor] = {
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

    val params = Map(
      "key" -> apiKey,
      "account_id" -> userId
    )

    getApiResponse(config.endpoints.matchHistory, params)
      .subflatMap(body => processResponse(body))
      .flatMap(cursor => parseUserRecentGames(cursor))
  }

  def getMatchDetails(userId: String, matchId: String): FutureEither[UserGameInfo] = {

    def getMatchDetailsAux(userId: String, matchId: String, attemptsMade: Int, maxAttempts: Int): FutureEither[UserGameInfo] = {
      tryToGetMatchDetails(userId, matchId) recoverWith {
        case TooManyRequestsException if attemptsMade < maxAttempts =>
          getMatchDetailsAux(userId, matchId, attemptsMade + 1, maxAttempts)
      }
    }

    def processResponse(body: String): Either[ApiError, HCursor] = {
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

    def tryToGetMatchDetails(userId: String, matchId: String): FutureEither[UserGameInfo] = {

      val params: Map[String, String] = Map(
        "key" -> apiKey,
        "match_id" -> matchId
      )

      getApiResponse(config.endpoints.matchDetails, params)
        .subflatMap(body => processResponse(body))
        .subflatMap(cursor => parseUserGameInfo(cursor))
    }

    def parseUserGameInfo(cursor: HCursor): Either[ParsingException, UserGameInfo] = {
      val parsed = for {
        radiantVictory <- cursor.downField("result").get[Boolean]("radiant_win")
        players <- cursor.downField("result").get[List[PlayerField]]("players")
        requiredPlayer <- players.find(_.account_id == userId.toLong).toRight(ParsingException("player not found"))

        playedForRadiant = players.indexOf(requiredPlayer) < 5
        hero = Hero(requiredPlayer.hero_id)
        player = Player(userId)

        outcome = if (radiantVictory == playedForRadiant) {
          Outcomes.Victory
        } else {
          Outcomes.Loss
        }
      } yield UserGameInfo(player, hero, outcome, requiredPlayer.kda)

      // convert circe's `DecodingFailure`s to `ParsingException`s
      parsed.left.map {
        case DecodingFailure(msg, _) => ParsingException(msg)
        case p: ParsingException => p
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
        val rows: List[Element] = doc >> elementList("section > article > table > tbody > tr")
        val entries: List[List[Element]] = rows.map(_ >> elementList("td"))
        entries.flatMap { columns =>
          val heroName = columns(1) >> text("a")
          val matches = (columns(2) >> attr("data-value")).toInt
          val winrate = (columns(3) >> attr("data-value")).toDouble
          Hero.getHeroByName(heroName) map { hero =>
            UserHeroPerformance(Player(userId), hero, matches, winrate)
          }
        } take n
      }.toEither
    }
    EitherT(f).leftMap(e => UnknownException(e.getMessage))
  }

  def fetchUserInfo(userId: String): FutureEither[UserInfo] = {

    def fetchUserInfoSteam(userId: String): FutureEither[UserInfo] = {

      def processResponse(body: String): Either[ApiError, HCursor] = {
        val statusWithCursor = for {
          json <- parse(body).right
        } yield json.hcursor

        statusWithCursor match {
          case Left(_) =>
            Left(AccessForbiddenException)
          case Right(cursor) =>
            // all good
            Right(cursor)
        }
      }

      def parseUserInfo(cursor: HCursor): Either[ParsingException, UserInfo] = {
        val parsed = for {
          nickname <- cursor
            .downField("response")
            .downField("players")
            .downArray
            .get[String]("personaname")
        } yield UserInfo(userId, nickname, None, None)

        parsed.left.map {
          case DecodingFailure(msg, _) => ParsingException(msg)
        }
      }

      // converting 32 bit Steam ID used in logs to 64 bit one
      val userId64 = {
        val head = 765
        val tail = userId.toLong + 61197960265728L
        head.toString + tail.toString
      }

      val params: Map[String, String] = Map(
        "key" -> apiKey,
        "steamids" -> userId64
      )

      getApiResponse(config.endpoints.playerInfoBackup, params)
        .subflatMap(body => processResponse(body))
        .subflatMap(cursor => parseUserInfo(cursor))
    }

    def fetchUserInfoOpenDota(userId: String): FutureEither[UserInfo] = {

      def processResponse(body: String): Either[ApiError, HCursor] = {
        val statusWithCursor = for {
          json <- parse(body).right
        } yield json.hcursor

        statusWithCursor match {
          case Left(_) =>
            Left(UnknownException("OpenDotaException"))
          case Right(cursor) =>
            // all good
            Right(cursor)
        }
      }

      def parseUserInfo(cursor: HCursor): Either[ParsingException, UserInfo] = {
        val parsed = for {
          id <- cursor.downField("profile").get[Int]("account_id").map(_.toString)
          nickname <- cursor.downField("profile").get[String]("personaname")
          solo = cursor.get[String]("solo_competitive_rank").toOption.map(_.toInt)
          party = cursor.get[String]("competitive_rank").toOption.map(_.toInt)
        } yield UserInfo(id, nickname, solo, party)

        parsed.left.map {
          case DecodingFailure(msg, _) => ParsingException(msg)
        }
      }

      getApiResponse(config.endpoints.playerInfo + userId)
        .subflatMap(body => processResponse(body))
        .subflatMap(cursor => parseUserInfo(cursor))
    }

    fetchUserInfoOpenDota(userId) recoverWith {
      // at least get player's nickname
      case _ => fetchUserInfoSteam(userId)
    }
  }
}
