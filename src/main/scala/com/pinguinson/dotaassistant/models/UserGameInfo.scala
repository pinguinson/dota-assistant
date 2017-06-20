package com.pinguinson.dotaassistant.models

import com.pinguinson.dotaassistant.models.Results._

/**
  * Created by pinguinson on 6/10/2017.
  */
case class UserGameInfo(userId: String, hero: String, result: Result, kda: String) extends UserReport
