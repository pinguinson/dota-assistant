package com.pinguinson.dotaassistant.model

import com.pinguinson.dotaassistant.model.Results._

/**
  * Created by pinguinson on 6/10/2017.
  */
case class UserGameInfo(userId: String, hero: String, result: Result, kda: String) extends UserReport
