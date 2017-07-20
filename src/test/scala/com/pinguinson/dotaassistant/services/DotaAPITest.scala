package com.pinguinson.dotaassistant.services

import com.pinguinson.dotaassistant.config.DotaApiConfig.config
import com.pinguinson.dotaassistant.models.Exceptions._
import org.scalatest._

/**
  * Created by pinguinson on 6/12/2017.
  */
class DotaAPITest extends AsyncFunSuite
  with Matchers
  with EitherValues
  with OptionValues {

  val validId = "61242572"
  val privateId = "61242573"
  val invalidCharactersId = "abc123"
  val nonexistentId = "99999999999999"

  val api = new DotaAPI(config.apiKey)

  val tenValidIds = List(
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
    api.fetchUserRecentGames(validId).value.map { either =>
      either.right.value.length should be <= config.maxRecentGames
    }
  }

  test("fetchUserMostPlayedHeroes with a valid ID should return proper result") {
    api.fetchUserMostPlayedHeroes(validId, 10).value.map { either =>
      val heroes = either.right.value

      heroes should have length 10

      // I'm fairly certain this is not going to change (almost 300 games ahead of a second place)
      val mostPlayed = heroes.head
      mostPlayed.hero.getName shouldBe "Crystal Maiden"
      mostPlayed.matches should be >= 570

      // As of 6/20/2017 my 10th most played hero was picked 97 times
      val leastPlayed = heroes.last
      leastPlayed.matches should be >= 97

      atLeast(8, heroes.map(_.matches)) should be >= 100
    }
  }

  test("fetchUserMostPlayedHeroes with a private ID should return empty list") {
    api.fetchUserMostPlayedHeroes(privateId, 10).value.map { either =>
      either.left.value shouldBe a [UnknownException]
    }
  }

  test("fetchUserMostPlayedHeroes with an invalid ID should return empty list") {
    api.fetchUserMostPlayedHeroes(invalidCharactersId, 10).value.map { either =>
      either.left.value shouldBe a [UnknownException]
    }
  }

  test("parseUserInfo with a valid ID") {
    api.fetchUserInfo(validId).value.map { either =>
      val userInfo = either.right.value

      userInfo.id.toString shouldBe validId
      userInfo.nickname shouldBe "pinguinson"
      userInfo.solo.value should be > 5000
      userInfo.party.value should be < 3500
    }
  }
}
