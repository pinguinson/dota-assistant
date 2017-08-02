package com.pinguinson.dotaassistant.models

case class UserInfo(id: String, nickname: String, solo: Option[Int], party: Option[Int]) {

  /**
    * String representation of player's MMR
    * @return optional string representation if any MMR value is known
    */
  def prettyMMR: Option[String] = {
    (solo, party) match {
      case (Some(s), Some(p)) =>
        Some(s"Solo: $s | Party: $p")
      case (Some(s), None) =>
        Some(s"Solo: $s")
      case (None, Some(p)) =>
        Some(s"Party: $p")
      case (None, None) =>
        None
    }
  }
}