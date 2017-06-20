package com.pinguinson.dotaassistant.models

/**
  * Created by pinguinson on 6/10/2017.
  */
object Results {
  sealed trait Result
  case object Victory extends Result
  case object Loss    extends Result
}
