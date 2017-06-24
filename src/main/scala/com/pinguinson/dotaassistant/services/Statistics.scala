package com.pinguinson.dotaassistant.services

import com.pinguinson.dotaassistant.models.Outcomes._
import com.pinguinson.dotaassistant.models.{HeroPerformance, UserGameInfo, UserHeroPerformance}

import scala.concurrent.{ExecutionContext, Future}

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
  def fetchUserRecentGames(userId: String): Future[Seq[UserGameInfo]]

  /**
    * Fetch match details from Dota API
    *
    * @param userId  user ID
    * @param matchId match ID
    * @return a future containing UserGameInfo
    */
  def getMatchDetails(userId: String, matchId: String): Future[UserGameInfo]

  /**
    * Fetch most played heroes
    *
    * @param userId user ID
    * @param n number of heroes to return
    * @return a future containing n most played heroes
    */
  def fetchUserMostPlayedHeroes(userId: String, n: Int): Future[Seq[UserHeroPerformance]]

  /**
    * Fetch information about 10 players in a match
    *
    * @param userIds Sequence with 10 userIds
    * @return a future containing sequence of length 10 (one per player), each containing another sequence of
    * up to 20 UserGameInfo's
    */
  def fetchMatchPlayersInfo(userIds: Seq[String])(implicit context: ExecutionContext): Future[Seq[Seq[UserGameInfo]]] = {
    assert(userIds.length == 10)
    Future.sequence(userIds.map(fetchUserRecentGames))
  }

  def analyzeTeam(playerReports: Seq[Seq[UserGameInfo]]): Seq[HeroPerformance] = {
    val heroPerformances = playerReports.flatten.groupBy(_.hero).toList.sortBy(-_._2.length)
    heroPerformances map {
      case (hero, performances) =>
        val totalMatches = performances.length
        val wonMatches = performances.count(_.outcome == Victory)
        val winrate = wonMatches.toDouble / totalMatches.toDouble
        HeroPerformance(hero, totalMatches, winrate)
    }
  }

  def analyzeTeams(playerReports: Seq[Seq[UserGameInfo]]): (Seq[HeroPerformance], Seq[HeroPerformance]) = {
    assert(playerReports.length == 10)
    val radiant = analyzeTeam(playerReports.take(5))
    val dire    = analyzeTeam(playerReports.takeRight(5))
    (radiant, dire)
  }
}
