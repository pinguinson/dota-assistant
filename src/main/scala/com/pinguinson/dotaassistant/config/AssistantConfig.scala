package com.pinguinson.dotaassistant.config

import java.nio.file.{Path, Paths}

import pureconfig._

/**
  * Created by pinguinson on 24.07.17.
  */
object AssistantConfig {

  private sealed trait OS
  private case object Windows extends OS
  private case object Linux extends OS
  private case object Mac extends OS
  private case object Unknown extends OS

  case class AssistantConfig(logPath: Option[String], apiKey: Option[String])
  private case class Assistant(assistant: AssistantConfig)
  private val defaultConfiguration = AssistantConfig(None, None)

  lazy val config: AssistantConfig = {
    loadConfig[Assistant](configPath) match {
      case Left(err) =>
        println(s"Failed to load assistant config: $err")
        defaultConfiguration
      case Right(c) =>
        c.assistant
    }
  }

  def saveConfig(logPath: String, apiKey: String): Unit = {
    val newConfig = Assistant(AssistantConfig(Some(logPath), Some(apiKey)))
    saveConfigAsPropertyFile(newConfig, configPath, overrideOutputPath = true)
  }

  private lazy val configPath: Path = {
    val homePath = System.getProperty("user.home")
    val fileName = "assistant.conf"
    val configSubpath = detectOS match {
      case Windows =>
        "/AppData/Local/DotaAssistant/"
      case Linux =>
        "/.config/dota-assistant/"
      case _ =>
        ""
    }
    val configPath = Paths.get(homePath + configSubpath + fileName)
    // Create config folder with all its necessary non-existent parents
    configPath.getParent.toFile.mkdirs()
    configPath
  }

  private def detectOS: OS = System.getProperty("os.name").toLowerCase match {
    case name if name contains "windows" =>
      Windows
    case name if name contains "linux" =>
      Linux
    case name if name contains "mac" =>
      // Not sure about this one
      Mac
    case _ =>
      Unknown
  }
}
