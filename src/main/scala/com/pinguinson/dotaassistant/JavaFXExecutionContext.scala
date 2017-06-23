package com.pinguinson.dotaassistant

import javafx.application.Platform

import scala.concurrent.ExecutionContext


/**
  * Created by pinguinson on 6/22/2017.
  */
object JavaFXExecutionContext {
  implicit val javaFxExecutionContext: ExecutionContext = ExecutionContext.fromExecutor((command: Runnable) => Platform.runLater(command))
}
