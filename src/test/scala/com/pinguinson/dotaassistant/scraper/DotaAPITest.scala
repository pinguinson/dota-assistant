package com.pinguinson.dotaassistant.scraper

import org.scalatest.FunSuite

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by pinguinson on 6/12/2017.
  */
class DotaAPITest extends FunSuite {

  val dotaApi = new DotaAPI()
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
    val result = dotaApi.fetchUserRecentGames(validId)
  }

  test("10 players") {
    val results = tenValidIds.map(dotaApi.fetchUserRecentGames)
    val r = results.foreach { res =>
      res.foreach(_.onComplete {
        case Success(result) => println(result)
        case Failure(ex) => println(ex)
      })
    }
    assert(results.length == 10)
  }

}
