package com.pinguinson.dotaassistant.model

/**
  * Created by pinguinson on 6/10/2017.
  */
case class UserRecentGames(userId: String, games: Seq[UserGameInfo]) extends UserReport
