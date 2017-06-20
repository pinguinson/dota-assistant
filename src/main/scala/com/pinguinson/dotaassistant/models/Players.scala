package com.pinguinson.dotaassistant.models

/**
  * Created by pinguinson on 6/21/2017.
  */
object Players {
  sealed trait Player
  case object UnknownPlayer extends Player
  case class IdentifiedPlayer(id: String) extends Player
}
