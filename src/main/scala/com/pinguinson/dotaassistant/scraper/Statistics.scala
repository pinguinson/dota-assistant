package com.pinguinson.dotaassistant.scraper

import com.pinguinson.dotaassistant.model.{UserGameInfo, UserHeroPerformance}

/**
  * Created by pinguinson on 6/10/2017.
  */
trait Statistics {
  def fetchUserRecentGames(userId: String): List[UserGameInfo]
  def fetchUserMostPlayedHeroes(userId: String): List[UserHeroPerformance]
}
