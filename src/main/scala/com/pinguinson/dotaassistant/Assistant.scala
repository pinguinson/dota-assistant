package com.pinguinson.dotaassistant

import java.io.File

import com.pinguinson.dotaassistant.models._
import com.pinguinson.dotaassistant.models.Players._
import com.pinguinson.dotaassistant.services._
import com.pinguinson.dotaassistant.JavaFXExecutionContext.javaFxExecutionContext
import com.pinguinson.dotaassistant.models.Outcomes.{Loss, Outcome, Victory}

import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.image._
import scalafx.scene.layout._
import scalafx.geometry.Insets
import scalafx.scene.paint.Color

object Assistant extends JFXApp {

  val defaultPadding = Insets(10, 10, 10, 10)

  val logPathTextField = new TextField()
  logPathTextField.setPrefWidth(720)

  val apiKeyTextField = new TextField()
  apiKeyTextField.setPrefWidth(720)

  val buttonParse = new Button("Parse last match")
  buttonParse.setOnAction(handle(processButtonClick()))

  val textFields = new VBox(
    new Label("Path to server_log.txt"),
    logPathTextField,
    new Label("Your Steam API key"),
    apiKeyTextField)
  textFields.setSpacing(5)
  val textFieldAndButton = new VBox(textFields, buttonParse)
  textFieldAndButton.setSpacing(10)

  val radiantBlock = new VBox()
  radiantBlock.setSpacing(5)
  val direBlock = new VBox()
  direBlock.setSpacing(5)
  val iconBlock = new HBox(radiantBlock, direBlock)
  iconBlock.setSpacing(10)

  val suggestions = new Label()

  val mainBlock = new VBox(
    textFieldAndButton,
    iconBlock,
    suggestions
  )

  val mainScene = new Scene {
    content = mainBlock
  }

  mainBlock.setPadding(defaultPadding)
  mainBlock.setSpacing(10)

  stage = new PrimaryStage {
    width = 1280
    height = 720
    scene = mainScene
  }

  stage.getIcons.add(new Image("icons/clockwerk_minimap_icon.png"))
  stage.setTitle("Dota assistant")

  // stop program on window close
  stage.setOnCloseRequest(handle {
    Platform.exit()
    System.exit(1)
  })

  def processButtonClick(): Unit = {
    val path = new File(logPathTextField.getText).toURI.toURL
    val apiKey = apiKeyTextField.getText
    LogParser.getLobbyPlayers(path) match {
      case None =>
        suggestions.setText("Failed to parse logs")
      case Some(players) =>
        new DotaAPI(apiKey).fetchMatchPlayersInfo(players) map { playerReports =>
          val grids = playerReports map buildIconGrid
          val radiantGrid = grids.take(5)
          val direGrid = grids.drop(5)
          radiantBlock.children = radiantGrid
          direBlock.children = direGrid
        }
    }
  }

  def teamMostPickedToString(team: Seq[HeroPerformance]): Label = {
    val text = team take 5 map {
      case HeroPerformance(hero, matches, winrate) =>
        f"$hero picked $matches times with ${winrate * 100}%.1f%% winrate"
    } mkString "\n"
    new Label(text)
  }

  def buildIconGrid(games: Seq[UserGameInfo]): VBox = {

    def buildImageViewForGame(hero: String, outcome: Outcome) = {
      val iconPath = Heroes.getMinimapIcon(hero)
      val view = new ImageView(new Image(iconPath))
      val bgColor = outcome match {
        case Victory => Color.DarkSeaGreen
        case Loss => Color.LightCoral
      }
      val fill = new BackgroundFill(
        bgColor,
        new CornerRadii(0),
        Insets(0, 0, 0, 0)
      )
      val box = new VBox(view)
      box.setBackground(new Background(Array(fill)))
      box
    }

    def buildImageViewsForUnknownPlayer() = {
      (1 to 20) map { _ =>
        buildImageViewForGame("", Loss)
      }
    }

    val label = games.headOption.map(_.player) match {
      case Some(IdentifiedPlayer(id)) =>
        new Label(s"Player #$id")
      case _ =>
        new Label(s"Unknown player")
    }

    val views = games.headOption match {
      case Some(_) => games.map(g => buildImageViewForGame(g.hero, g.outcome))
      case None => buildImageViewsForUnknownPlayer()
    }
    val row1 = new HBox {
      children = views.take(10)
    }
    val row2 = new HBox {
      children = views.drop(10)
    }

    new VBox(label, row1, row2)
  }
}