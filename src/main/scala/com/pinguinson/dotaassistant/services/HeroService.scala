package com.pinguinson.dotaassistant.services

import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source

/**
  * Created by pinguinson on 6/18/2017.
  */
object HeroService {

  case class Hero(id: Int, name: String)
  case class HeroesMap(heroes: List[Hero])

  private lazy val heroesMap: Map[Int, String] = decode[HeroesMap](Source.fromResource("heroes.json").getLines.toList.mkString("")) match {
    case Right(map) =>
      map.heroes.map(h => (h.id, h.name)).toMap.withDefaultValue("<unknown>")
    case Left(error) =>
      println(s"Failed to parse heroes.json: $error")
      Map.empty[Int, String].withDefaultValue("<unknown>")
  }

  /**
    * Get hero name by hero ID
    * @param id hero ID
    * @return hero name
    */
  def getName(id: Int): String = heroesMap(id)

  /**
    * Get path to the hero icon
    * @param optionalHero optional hero name
    * @return path to the hero icon or question mark if hero is None
    */
  def getMinimapIcon(optionalHero: Option[String]): String = {

    val fileName = optionalHero
      .map(_.toLowerCase.replace(' ', '_') + "_minimap_icon.png")
      .getOrElse("unknown_hero_minimap_icon.png")

    s"icons/$fileName"
  }
}
