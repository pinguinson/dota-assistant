package com.pinguinson.dotaassistant.models

import com.pinguinson.dotaassistant.models.Outcomes._
import com.pinguinson.dotaassistant.models.Players._

/**
  * Created by pinguinson on 6/10/2017.
  */
case class UserGameInfo(player: Player, hero: String, outcome: Outcome, kda: String) extends UserReport
