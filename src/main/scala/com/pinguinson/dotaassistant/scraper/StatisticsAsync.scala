package com.pinguinson.dotaassistant.scraper

import com.pinguinson.dotaassistant.model.{UserGameInfo, UserHeroPerformance}

import scala.concurrent.{ExecutionContext, Future}

/**
  * A trait which all asynchronous scrapers should implement
  */
trait StatisticsAsync {

  /**
    * Fetch recent games (at most 50, might be less if user played custom games)
    *
    * @param userId user ID
    * @return a future containing recent games
    */
  def fetchUserRecentGames(userId: String): Future[Seq[UserGameInfo]]

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
    Future.sequence(userIds.map(fetchUserRecentGames))
  }
}
