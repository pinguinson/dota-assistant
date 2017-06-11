package com.pinguinson.dotaassistant.scraper

import com.pinguinson.dotaassistant.model.{UserGameInfo, UserHeroPerformance}

/**
  * A trait which all scrapers should implement
  */
trait Statistics {

  /**
    * Fetch recent games (at most 50, might be less if user played custom games)
    *
    * @param userId user ID
    * @return a list which contains recent games
    */
  def fetchUserRecentGames(userId: String): List[UserGameInfo]

  /**
    * Fetch most played heroes
    *
    * @param userId user ID
    * @param n number of heroes to return
    * @return a list which contains n most played heroes
    */
  def fetchUserMostPlayedHeroes(userId: String, n: Int): List[UserHeroPerformance]
}
