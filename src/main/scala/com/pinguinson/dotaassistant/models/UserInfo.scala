package com.pinguinson.dotaassistant.models

case class UserInfo(id: String, nickname: String, solo: Option[Int], party: Option[Int]) {
  def pretty: String = {
    val mmr = for {
      s <- solo
      p <- party
    } yield s"Solo: $s | Party: $p"

    mmr match {
      case Some(str) => s"$nickname | $str"
      case None => s"$nickname"
    }
  }
}