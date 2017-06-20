package com.pinguinson.dotaassistant.services

import com.pinguinson.dotaassistant.services.LogParser.getLobbyPlayers
import org.scalatest._

/**
  * Created by pinguinson on 6/14/2017.
  */
class LogParserTest extends FunSuite with Matchers {

  test("getLobbyPlayers with valid log file returns correct list of IDs") {
    val path = getClass.getResource("/logs/log_valid.txt")
    getLobbyPlayers(path) shouldBe Some(List("295023734", "149929786", "208470653", "88989059", "209875610", "210021266", "61242572", "50862597", "136049358", "86732687"))
  }

  test("getLobbyPlayers with invalid log file returns None") {
    val path = getClass.getResource("/logs/log_invalid.txt")
    getLobbyPlayers(path) shouldBe None
  }

}
