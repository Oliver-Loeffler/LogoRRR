package app.logorrr.model

import app.logorrr.views.block.BlockView
import app.logorrr.views.{Filter, Fltr}
import javafx.geometry.Insets
import javafx.scene.layout.{Background, BackgroundFill, CornerRadii}
import javafx.scene.paint.Color

import java.time.Instant
import scala.language.postfixOps

/**
 * represents one line in a log file
 *
 * @param lineNumber line number of this log entry
 * @param value contens of line in plaintext
 * @param someInstant a timestamp if there is any
 * */
case class LogEntry(lineNumber: Int
                    , color: Color
                    , value: String
                    , someInstant: Option[Instant])
  extends BlockView.E {

  val index: Int = lineNumber

  lazy val background = new Background(new BackgroundFill(color, new CornerRadii(1.0), new Insets(0.0)))


}
