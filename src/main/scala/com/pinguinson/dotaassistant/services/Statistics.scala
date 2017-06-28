package com.pinguinson.dotaassistant.services

import cats.implicits._
import com.pinguinson.dotaassistant.models.Outcomes._
import com.pinguinson.dotaassistant.models.UserReports._
import com.pinguinson.dotaassistant.models.HeroPerformance

import scala.concurrent.ExecutionContext

/**
  * Base trait for an assistant
  */
trait Statistics {

  /**
    * Fetch recent games (at most 50, might be less if user played custom games)
    *
    * @param userId user ID
    * @return a future containing recent games
    */
  def fetchUserRecentGames(userId: String): FutureEither[List[UserGameInfo]]

  /**
    * Fetch match details from Dota API
    *
    * @param userId  user ID
    * @param matchId match ID
    * @return a future containing UserGameInfo
    */
  def getMatchDetails(userId: String, matchId: String): FutureEither[UserGameInfo]

  /**
    * Fetch most played heroes
    *
    * @param userId user ID
    * @param n number of heroes to return
    * @return a future containing n most played heroes
    */
  def fetchUserMostPlayedHeroes(userId: String, n: Int): FutureEither[List[UserHeroPerformance]]

  /**
    * Fetch information about 10 players in a match
    *
    * @param userIds Sequence with 10 userIds
    * @return a future containing sequence of length 10 (one per player), each containing another sequence of
    * up to 20 UserGameInfo's
    */
  def fetchMatchPlayersInfo(userIds: List[String])(implicit context: ExecutionContext): List[FutureEither[List[UserGameInfo]]] = {
    assert(userIds.length == 10)
    userIds.map(fetchUserRecentGames)
  }

  /**
    * Get team's performance on heroes they pick
    * @param playerReports list with players' `UserGameInfo`s
    * @return a list of `HeroPerformance`, one entry per picked hero
    */
  def analyzeTeam(playerReports: List[List[UserGameInfo]]): List[HeroPerformance] = {
    val heroPerformances = playerReports.flatten.groupBy(_.hero).toList.sortBy(-_._2.length)
    heroPerformances map {
      case (hero, performances) =>
        val totalMatches = performances.length
        val wonMatches = performances.count(_.outcome == Victory)
        val winrate = wonMatches.toDouble / totalMatches.toDouble
        HeroPerformance(hero, totalMatches, winrate)
    }
  }

  /**
    * Get teams' performances on heroes they pick
    * @param playerReports list with players' `UserGameInfo`s
    * @return a tuple of two lists (radiant and dire) of `HeroPerformance`s, one entry per picked hero
    */
  def analyzeTeams(playerReports: List[List[UserGameInfo]]): (List[HeroPerformance], List[HeroPerformance]) = {
    assert(playerReports.length == 10)
    val (radiant, dire) = playerReports.splitAt(5)
    val radiantAnalysis = analyzeTeam(radiant)
    val direAnalysis = analyzeTeam(dire)
    (radiantAnalysis, direAnalysis)
  }
}
