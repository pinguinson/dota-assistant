package com.pinguinson.dotaassistant.scraper

import org.scalatest.{BeforeAndAfterEach, FunSuite}

/**
  * Created by pinguinson on 6/11/2017.
  */
class DotabuffScraperTest extends FunSuite with BeforeAndAfterEach {

  val scraper = new DotabuffScraper()
  val validId = "61242572"
  val privateId = "61242573"
  val invalidCharactersId = "abc123"
  val nonexistentId = "99999999999999"

  test("fetchUserRecentGames with valid ID returns nonempty list") {
    val result = scraper.fetchUserRecentGames(validId)
    assert(result.nonEmpty)
  }

  test("fetchUserRecentGames with private profile ID returns empty list") {
    val result = scraper.fetchUserRecentGames(privateId)
    assert(result.isEmpty)
  }

  test("fetchUserRecentGames returns empty list if user ID is not a number") {
    val result = scraper.fetchUserRecentGames(invalidCharactersId)
    assert(result.isEmpty)
  }

  test("fetchUserRecentGames returns empty list if ID is valid but doesn't exist") {
    val result = scraper.fetchUserRecentGames(nonexistentId)
    assert(result.isEmpty)
  }

  test("fetchUserMostPlayedHeroes with valid ID returns nonempty list") {
    val result = scraper.fetchUserMostPlayedHeroes(validId, 10)
    assert(result.nonEmpty)
  }

  test("fetchUserMostPlayedHeroes with private profile ID returns empty list") {
    val result = scraper.fetchUserMostPlayedHeroes(privateId, 10)
    assert(result.isEmpty)
  }

  test("fetchUserMostPlayedHeroes returns empty list if user ID is not a number") {
    val result = scraper.fetchUserMostPlayedHeroes(invalidCharactersId, 10)
    assert(result.isEmpty)
  }

  test("fetchUserMostPlayedHeroes returns empty list if ID is valid but doesn't exist") {
    val result = scraper.fetchUserMostPlayedHeroes(nonexistentId, 10)
    assert(result.isEmpty)
  }

}
