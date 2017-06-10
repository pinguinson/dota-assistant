package com.pinguinson.dotaassistant.scraper

import com.pinguinson.dotaassistant.model.{UserHeroPerformance, UserRecentGames}

/**
  * Created by pinguinson on 6/10/2017.
  */
trait Statistics {
  def fetchUserRecentGames(userId: String): UserRecentGames
  def fetchUserMostPlayedHeroes(userId: String): List[UserHeroPerformance]
}
