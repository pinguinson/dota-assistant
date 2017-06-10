package com.pinguinson.dotaassistant.scraper

import com.pinguinson.dotaassistant.model._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

/**
  * Created by pinguinson on 6/10/2017.
  */
class DotabuffScraper extends Statistics {

  val browser = JsoupBrowser()

  def fetchUserRecentGames(userId: String): UserRecentGames = {
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
    UserRecentGames(userId, games = games.flatten)
  }

  def fetchUserMostPlayedHeroes(userId: String): List[UserHeroPerformance] = List.empty
}
