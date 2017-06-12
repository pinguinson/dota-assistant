package com.pinguinson.dotaassistant.scraper

import com.pinguinson.dotaassistant.model.{UserGameInfo, UserHeroPerformance}

import scala.concurrent.Future

/**
  * A trait which all scrapers should implement
  */
trait StatisticsAsync {

  /**
    * Fetch recent games (at most 50, might be less if user played custom games)
    *
    * @param userId user ID
    * @return a list of futures which contains recent games
    */
  def fetchUserRecentGames(userId: String): List[Future[UserGameInfo]]

  /**
    * Fetch most played heroes
    *
    * @param userId user ID
    * @param n number of heroes to return
    * @return a list of futures which contains n most played heroes
    */
  def fetchUserMostPlayedHeroes(userId: String, n: Int): List[Future[UserHeroPerformance]]
}
