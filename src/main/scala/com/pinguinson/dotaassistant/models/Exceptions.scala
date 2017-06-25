package com.pinguinson.dotaassistant.models

/**
  * Created by pinguinson on 6/25/2017.
  */
object Exceptions {
  sealed trait DotaApiException
  case object AccessForbiddenException extends DotaApiException
  case object TooManyRequestsException extends DotaApiException
  case object PrivateProfileException extends DotaApiException
  case class UnknownException(responseString: String) extends DotaApiException
  case object MatchNotFound extends DotaApiException
}
