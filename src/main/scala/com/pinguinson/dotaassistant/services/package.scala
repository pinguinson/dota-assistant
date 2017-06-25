package com.pinguinson.dotaassistant

import cats.data.EitherT
import com.pinguinson.dotaassistant.models.Exceptions.DotaApiException

import scala.concurrent.Future

/**
  * Created by pinguinson on 6/25/2017.
  */
package object services {
  type ApiError = DotaApiException
  type FutureEither[A] = EitherT[Future, ApiError, A]
}
