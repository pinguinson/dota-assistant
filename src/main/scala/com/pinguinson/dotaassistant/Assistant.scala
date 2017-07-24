package com.pinguinson.dotaassistant

import java.io.File

import cats.implicits._
import com.pinguinson.dotaassistant.models._
import com.pinguinson.dotaassistant.models.UserReports._
import com.pinguinson.dotaassistant.services._
import com.pinguinson.dotaassistant.JavaFXExecutionContext.javaFxExecutionContext
import com.pinguinson.dotaassistant.config.AssistantConfig.{saveConfig, config => assistantConfig}
import com.pinguinson.dotaassistant.models.Outcomes._

import javafx.scene.control.{TextField => JTextField}
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

  val logPathTextField = buildTextField(assistantConfig.logPath)
  logPathTextField.setPrefWidth(650)

  val apiKeyTextField = buildTextField(assistantConfig.apiKey)
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
    height = 650
    scene = mainScene
  }

  stage.getIcons.add(new Image("icons/clockwerk.png"))
  stage.setTitle("Dota assistant")

  // stop program on window close
  stage.setOnCloseRequest(handle {
    Platform.exit()
    System.exit(1)
  })

  def processButtonClick(): Unit = {
    updateConfig()
    val path = new File(logPathTextField.getText).toURI.toURL
    val apiKey = apiKeyTextField.getText
    LogParser.getLobbyPlayers(path) match {
      case None =>
        suggestions.setText("Failed to parse logs")
      case Some(players) =>
        val api = new DotaAPI(apiKey)
        val (radiantReports, direReports) = api.fetchMatchPlayersInfo(players).splitAt(5)
        val (radiantPlayers, direPlayers) = api.fetchUsersInfo(players).splitAt(5)
        // set placeholder grids
        val grids = players.map(buildIconGrid)
        val labels = players.map(buildLabel)
        val blocks = labels zip grids flatMap {
          case (label, grid) => List(label, grid)
        }
        radiantBlock.children = blocks.take(10)
        direBlock.children = blocks.drop(10)

        radiantPlayers.map(_.map { player =>
          val index = players.indexOf(player.id) * 2
          radiantBlock.children.set(index, buildLabel(player))
        })

        radiantReports.map(_.map { playerMatches =>
          val index = players.indexOf(playerMatches.head.player.id) * 2 + 1
          radiantBlock.children.set(index, buildIconGrid(playerMatches))
        })

        direPlayers.map(_.map { player =>
          val index = (players.indexOf(player.id) - 5) * 2
          direBlock.children.set(index, buildLabel(player))
        })

        direReports.map(_.map { playerMatches =>
          val index = (players.indexOf(playerMatches.head.player.id) - 5) * 2 + 1
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
    * Build an icon grid
    * @param optionalGames list of optional `UserGameInfo`'s
    * @return `VBox` containing an icon grid (one icon per optional `UserGameInfo`)
    */
  def buildIconGridAux(optionalGames: List[Option[UserGameInfo]]): VBox = {
    val views = optionalGames.map(buildIcon)
    val row1 = new HBox {
      children = views.take(10)
    }
    val row2 = new HBox {
      children = views.drop(10)
    }
    new VBox(row1, row2)
  }

  /**
    * Build a placeholder icon grid for `userId`
    * @param userId user ID
    * @return `VBox` containing 10x2 grid with question marks
    */
  def buildIconGrid(userId: String): VBox = {
    val optionalGames = List.fill[Option[UserGameInfo]](20)(None)
    buildIconGridAux(optionalGames)
  }

  /**
    * Build an icon grid for the `games`
    * @param games list of user's `UserGameInfo`s
    * @return `VBox` containing 10x2 grid with icons of the heroes user played
    */
  def buildIconGrid(games: List[UserGameInfo]): VBox = {
    val optionalGames = games.map(Some(_))
    buildIconGridAux(optionalGames)
  }
  /**
    * Build a `Label` for a user
    * @param info `UserInfo`
    * @return `Label` with user's nickname, solo MMR, and party MMR
    */
  def buildLabel(info: UserInfo): Label = {
    Label(info.pretty)
  }

  /**
    * Build a `Label` for a user
    * @param userId user ID
    * @return `Label` with user ID
    */
  def buildLabel(userId: String): Label = {
    Label(s"Player #$userId")
  }

  /**
    * Build a `TextField` with an optional default text
    * @param defaultText default text or `None`
    * @return a `TextField`
    */
  def buildTextField(defaultText: Option[String]): TextField = defaultText match {
    case Some(text) =>
      new TextField(new JTextField(text))
    case None =>
      new TextField()
  }

  /**
    * Save log path and API key in text fields to the config file
    */
  def updateConfig(): Unit = {
    val path = logPathTextField.getText
    val apiKey = apiKeyTextField.getText
    if (!path.isEmpty && !apiKey.isEmpty)
      saveConfig(path, apiKey)
  }
}