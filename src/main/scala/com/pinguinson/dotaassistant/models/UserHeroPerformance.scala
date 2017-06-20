package com.pinguinson.dotaassistant.models

import com.pinguinson.dotaassistant.models.Players._

/**
  * Created by pinguinson on 6/10/2017.
  */
case class UserHeroPerformance(player: Player, hero: String, matches: Int, winrate: Double) extends UserReport
