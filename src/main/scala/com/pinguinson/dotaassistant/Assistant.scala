package com.pinguinson.dotaassistant

import java.io.File

import cats.implicits._
import com.pinguinson.dotaassistant.models._
import com.pinguinson.dotaassistant.models.UserReports._
import com.pinguinson.dotaassistant.services._
import com.pinguinson.dotaassistant.JavaFXExecutionContext.javaFxExecutionContext
import com.pinguinson.dotaassistant.models.Outcomes._

import scalafx.Includes._
import scalafx.application._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene._
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
        // set placeholder grids
        val grids = players.map(buildIconGrid)
        radiantBlock.children = grids.take(5)
        direBlock.children = grids.drop(5)

        radiantReports.map(_.map { playerMatches =>
          val index = players.indexOf(playerMatches.head.player.id)
          radiantBlock.children.set(index, buildIconGrid(playerMatches))
        })

        direReports.map(_.map { playerMatches =>
          val index = players.indexOf(playerMatches.head.player.id) - 5
          direBlock.children.set(index, buildIconGrid(playerMatches))
        })
    }
  }

  /**
    * Build a `VBox` with a hero icon
    * @param optionalGame optional `UserGameInfo`
    * @return `VBox` with an `ImageView` containing hero icon or a question mark
    * if `UserGameInfo` is not present. Background color is set to green if
    * the game was won, set to red otherwise
    */
  def buildIcon(optionalGame: Option[UserGameInfo]): VBox = {
    val icon = Hero.getMinimapIcon(optionalGame.map(_.hero))
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

  /**
    * Build a labeled icon grid
    * @param labelText label text
    * @param optionalGames list of optional `UserGameInfo`'s
    * @return `VBox` containing `Label` with `labelText` and an
    * icon grid (one icon per optional `UserGameInfo`)
    */
  def buildIconGrid(labelText: String, optionalGames: List[Option[UserGameInfo]]): VBox = {
    val views = optionalGames.map(buildIcon)
    val label = new Label(labelText)
    val row1 = new HBox {
      children = views.take(10)
    }
    val row2 = new HBox {
      children = views.drop(10)
    }
    new VBox(label, row1, row2)
  }

  /**
    * Build a placeholder icon grid for `userId`
    * @param userId user ID
    * @return `VBox` containing label `userId` and 10x2 grid
    * with question marks
    */
  def buildIconGrid(userId: String): VBox = {
    val labelText = s"Player #$userId"
    val optionalGames = List.fill[Option[UserGameInfo]](20)(None)
    buildIconGrid(labelText, optionalGames)
  }

  /**
    * Build an icon grid for the `games`
    * @param games list of user's `UserGameInfo`s
    * @return `VBox` containing label (player's id) and 10x2 grid
    * with icons of the heroes he played
    */
  def buildIconGrid(games: List[UserGameInfo]): VBox = {
    val labelText = s"Player #${games.head.player.id}"
    val optionalGames = games.map(Some(_))
    buildIconGrid(labelText, optionalGames)
  }
}