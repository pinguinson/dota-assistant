package com.pinguinson.dotaassistant.models

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
}
