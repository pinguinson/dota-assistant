package com.pinguinson.dotaassistant.config

import pureconfig.loadConfig

/**
  * Created by pinguinson on 6/13/2017.
  */
object DotaApiConfig {

  case class Endpoints(matchHistory: String, matchDetails: String)
  case class DotaApiConfig(endpoints: Endpoints, validLobbyTypes: Seq[Int], maxRecentGames: Int, maxRetries: Int, apiKey: String)
  private case class DotaApi(dotaApi: DotaApiConfig)

  private val defaultConfig = DotaApiConfig(Endpoints("missing", "missing"), List.empty, 1, 1, "missing")

  lazy val config: DotaApiConfig = loadConfig[DotaApi] match {
    case Left(ex) =>
      println(s"No config found: $ex")
      // Maybe should handle this case differently?
      defaultConfig
    case Right(c) => c.dotaApi
  }
}
