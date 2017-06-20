package com.pinguinson.dotaassistant.services

import com.pinguinson.dotaassistant.config.DotaApiConfig.config
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by pinguinson on 6/12/2017.
  */
class DotaAPITest extends FunSuite with Matchers {

  val validId = "61242572"
  val privateId = "61242573"
  val invalidCharactersId = "abc123"
  val nonexistentId = "99999999999999"

  val tenValidIds = Seq(
    "169672678",
    "116889906",
    "61242572",
    "205103394",
    "115432332",
    "164109096",
    "148019944",
    "86714798",
    "35038268",
    "60775374"
  )

  test("testFetchUserRecentGames") {
    val futureResult = DotaAPI.fetchUserRecentGames(validId)
    val result = Await.result(futureResult, 10 minutes)

    result.length should be <= config.maxRecentGames
  }

  test("10 players") {
    val futureResult = DotaAPI.fetchMatchPlayersInfo(tenValidIds)
    val result = Await.result(futureResult, 10 minutes)

    result should have length 10
  }

  test("fetchUserMostPlayedHeroes with a valid ID should return proper result") {
    val futureResult = DotaAPI.fetchUserMostPlayedHeroes(validId, 10)
    val heroes = Await.result(futureResult, 5 seconds)

    heroes should have length 10

    // I'm fairly certain this is not going to change (almost 300 games ahead of a second place)
    val mostPlayed = heroes.head
    mostPlayed.hero shouldBe "Crystal Maiden"
    mostPlayed.matches should be >= 570

    // As of 6/20/2017 my 10th most played hero was picked 97 times
    val leastPlayed = heroes.last
    leastPlayed.matches should be >= 97

    atLeast(8, heroes.map(_.matches)) should be >= 100
  }

  test("fetchUserMostPlayedHeroes with a private ID should return empty list") {
    val futureResult = DotaAPI.fetchUserMostPlayedHeroes(privateId, 10)
    val result = Await.result(futureResult, 5 seconds)
    result shouldBe empty
  }

  test("fetchUserMostPlayedHeroes with an invalid ID should return empty list") {
    val futureResult = DotaAPI.fetchUserMostPlayedHeroes(invalidCharactersId, 10)
    val result = Await.result(futureResult, 5 seconds)
    result shouldBe empty
  }
}
