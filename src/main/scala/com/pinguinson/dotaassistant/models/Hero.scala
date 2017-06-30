package com.pinguinson.dotaassistant.models

import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source

/**
  * Created by pinguinson on 6/28/2017.
  */
case class Hero(id: Int) extends AnyVal {

  def getName: String = Hero.getName(id)

  def getIconPath: String = Hero.getMinimapIcon(Some(this))

}

object Hero {

  private case class HeroEntry(id: Int, name: String)
  private case class HeroesMap(heroes: List[HeroEntry])

  private lazy val heroIdToName: Map[Int, String] = decode[HeroesMap](Source.fromResource("heroes.json").getLines.toList.mkString("")) match {
    case Right(map) =>
      map.heroes.map(h => (h.id, h.name)).toMap
    case Left(error) =>
      println(s"Failed to parse heroes.json: $error")
      Map.empty[Int, String]
  }

  private lazy val heroNameToId: Map[String, Int] = heroIdToName.map(_.swap)

  /**
    * Get hero name by hero ID
    * @param id hero ID
    * @return hero name
    */
  def getName(id: Int): String = heroIdToName.getOrElse(id, "Unknown hero")

  /**
    * Get path to the hero icon
    * @param optionalHero optional `Hero`
    * @return path to the hero icon or question mark if hero is None
    */
  def getMinimapIcon(optionalHero: Option[Hero]): String = {

    val fileName = optionalHero
      .map(_.getName.toLowerCase.replace(' ', '_') + ".png")
      .getOrElse("unknown_hero.png")

    s"icons/$fileName"
  }

  /**
    * Get `Hero` by hero name
    * @param name hero name
    * @return optional `Hero` if name is valid, `None` otherwise
    */
  def getHeroByName(name: String): Option[Hero] = {
    for {
      id <- heroNameToId.get(name)
    } yield Hero(id)
  }

}