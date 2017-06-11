package com.pinguinson.dotaassistant.scraper

import com.pinguinson.dotaassistant.model._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

/**
  * Scraper for dotabuff.com
  */
class DotabuffScraper extends Statistics {

  private[this] val browser = JsoupBrowser()

  /**
    * Fetch recent games (at most 50, might be less if user played custom games)
    *
    * @param userId user ID
    * @return a list which contains recent games
    */
  def fetchUserRecentGames(userId: String): List[UserGameInfo] = {
    val doc = browser.get(s"https://www.dotabuff.com/players/$userId/matches")
    val entries = doc >> elementList("section > article > table > tbody > tr") >> elementList("td")
    val games = entries.map { columns =>
      val hero = columns(1) >> text("a")
      val result = columns(3) >> text("a") match {
        case "Lost Match" => Results.Loss
        case "Won Match"  => Results.Victory
      }
      val optionalKda = (columns(6) >?> element(".kda-record") >> texts(".value")).map(_.mkString("/"))
      optionalKda.map(kda => UserGameInfo(userId, hero, result, kda))
    }
    games.flatten
  }

  /**
    * Fetch most played heroes
    *
    * @param userId user ID
    * @return a list which contains most played heroes
    */
  def fetchUserMostPlayedHeroes(userId: String): List[UserHeroPerformance] = List.empty
}
