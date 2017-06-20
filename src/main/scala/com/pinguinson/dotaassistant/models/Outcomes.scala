package com.pinguinson.dotaassistant.models

/**
  * Created by pinguinson on 6/10/2017.
  */
object Outcomes {
  sealed trait Outcome
  case object Victory extends Outcome
  case object Loss extends Outcome
}
