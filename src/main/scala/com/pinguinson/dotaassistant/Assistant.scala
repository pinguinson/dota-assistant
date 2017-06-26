package com.pinguinson.dotaassistant

import java.io.File

import cats.implicits._
import com.pinguinson.dotaassistant.models._
import com.pinguinson.dotaassistant.models.UserReports._
import com.pinguinson.dotaassistant.models.Players._
import com.pinguinson.dotaassistant.services._
import com.pinguinson.dotaassistant.JavaFXExecutionContext.javaFxExecutionContext
import com.pinguinson.dotaassistant.models.Outcomes.{Loss, Outcome, Victory}

import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.{Scene, Node}
import scalafx.scene.control._
import scalafx.scene.image._
import scalafx.scene.layout._
import scalafx.geometry.Insets
import scalafx.scene.paint.Color

object Assistant extends JFXApp {

  val defaultPadding = Insets(10, 10, 10, 10)

  val logPathTextField = new TextField()
  logPathTextField.setPrefWidth(650)

  val apiKeyTextField = new TextField()
  apiKeyTextField.setPrefWidth(650)

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
    width = 690
    height = 630
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
        val (radiantReports, direReports) = new DotaAPI(apiKey).fetchMatchPlayersInfo(players).splitAt(5)
        // remove previous children
        radiantBlock.children = List.empty[Node]
        direBlock.children = List.empty[Node]

        radiantReports.map(_.bimap(
          { error =>
            radiantBlock.children.add(buildIconGrid(Either.left(error)))
          },
          { playerMatches =>
            radiantBlock.children.add(buildIconGrid(Either.right(playerMatches)))
          }
        ))
        direReports.map(_.bimap(
          { error =>
            direBlock.children.add(buildIconGrid(Either.left(error)))
          },
          { playerMatches =>
            direBlock.children.add(buildIconGrid(Either.right(playerMatches)))
          }
        ))
    }
  }

  def teamMostPickedToString(team: Seq[HeroPerformance]): Label = {
    val text = team take 5 map {
      case HeroPerformance(hero, matches, winrate) =>
        f"$hero picked $matches times with ${winrate * 100}%.1f%% winrate"
    } mkString "\n"
    new Label(text)
  }

  def buildIconGrid(e: Either[ApiError, List[UserGameInfo]]): VBox = {

    def buildImageViewForGame(optionalGame: Option[UserGameInfo]) = {
      val icon = HeroService.getMinimapIcon(optionalGame.map(_.hero))
      val view = new ImageView(new Image(icon))
      val bgColor = optionalGame.map(_.outcome) match {
        case Some(Victory) => Color.DarkSeaGreen
        case _ => Color.LightCoral
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

    val labelText = e.map(_.head.player) match {
      case Left(error) =>
        error.toString
      case Right(IdentifiedPlayer(id)) =>
        s"Player #$id"
      case Right(UnknownPlayer) =>
        s"Unknown player"
    }
    val views = e match {
      case Left(error) =>
        (1 to 20).map(_ => None).map(buildImageViewForGame)
      case Right(userGameInfos) =>
        userGameInfos.map(Some(_)).map(buildImageViewForGame)
    }
    val label = new Label(labelText)
    val row1 = new HBox {
      children = views.take(10)
    }
    val row2 = new HBox {
      children = views.drop(10)
    }

    new VBox(label, row1, row2)
  }
}