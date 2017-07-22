package com.pinguinson.dotaassistant.models

case class UserInfo(id: String, nickname: String, solo: Option[Int], party: Option[Int]) {

  def pretty: String = {

    def mmrToString(mmr: Option[Int]): String = {
      mmr.map(_.toString).getOrElse("Unknown")
    }

    s"$nickname | Solo: ${mmrToString(solo)} | Party: ${mmrToString(party)}"
  }
}