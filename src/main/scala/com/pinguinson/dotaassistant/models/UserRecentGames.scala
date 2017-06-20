package com.pinguinson.dotaassistant.models

import com.pinguinson.dotaassistant.models.Players.Player

/**
  * Created by pinguinson on 6/10/2017.
  */
case class UserRecentGames(player: Player, matches: Seq[UserGameInfo]) extends UserReport
