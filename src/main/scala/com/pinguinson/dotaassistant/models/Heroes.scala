package com.pinguinson.dotaassistant.models

import java.net.URL

import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source

/**
  * Created by pinguinson on 6/18/2017.
  */
object Heroes {

  case class Hero(id: Int, name: String)
  case class HeroesMap(heroes: List[Hero])

  private lazy val heroesMap: Map[Int, String] = decode[HeroesMap](Source.fromResource("heroes.json").getLines.toList.mkString("")) match {
    case Right(map) =>
      map.heroes.map(h => (h.id, h.name)).toMap.withDefaultValue("<unknown>")
    case Left(error) =>
      println(s"Failed to parse heroes.json: $error")
      Map.empty[Int, String].withDefaultValue("<unknown>")
  }

  def apply(id: Int): String = heroesMap(id)
  def apply(id: String): String = heroesMap(id.toInt)
  def getIcon(id: Int): String = "generic_icon.png"
  def getIcon(heroName: String): String = "generic_icon.png"
  def getMinimapIcon(id: Int): String = "generic_minimap_icon.png"
  def getMinimapIcon(heroName: String): String = {
    val default = "unknown_hero_minimap_icon.png"
    val fileName = heroName.headOption match {
      case Some(_) =>
        heroName.toLowerCase.replace(' ', '_') + "_minimap_icon.png"
      case _ =>
        default
    }
    s"icons/$fileName"
  }
}
