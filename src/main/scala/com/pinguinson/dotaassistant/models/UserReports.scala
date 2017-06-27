package com.pinguinson.dotaassistant.models

import com.pinguinson.dotaassistant.models.Outcomes.Outcome
import com.pinguinson.dotaassistant.models.Players._

/**
  * Created by pinguinson on 6/26/2017.
  */
object UserReports {

  sealed trait UserReport {
    def player: Player
  }

  case class UserRecentGames(player: Player, matches: Seq[UserGameInfo]) extends UserReport
  case class UserHeroPerformance(player: Player, hero: String, matches: Int, winrate: Double) extends UserReport
  case class UserGameInfo(player: IdentifiedPlayer, hero: String, outcome: Outcome, kda: String) extends UserReport
}
