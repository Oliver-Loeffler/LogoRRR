package net.ladstatt.logboard.views

import javafx.beans.property.{SimpleIntegerProperty, SimpleObjectProperty}
import javafx.event.EventHandler
import javafx.scene.control.ScrollPane
import javafx.scene.image.{ImageView, PixelWriter, WritableImage}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import net.ladstatt.logboard.{LogEntry, LogReport}
import net.ladstatt.util.CanLog

import scala.jdk.CollectionConverters._

class LogVisualView(entries: java.util.List[LogEntry]
                    , squareWidth: Int
                    , canvasWidth: Int) extends BorderPane with CanLog {

  val currentCanvasWidthProperty = new SimpleIntegerProperty(canvasWidth)

  def getCanvasWidth(): Int = currentCanvasWidthProperty.get()

  def setCanvasWidth(canvasWidth: Int): Unit = currentCanvasWidthProperty.set(canvasWidth)

  val selectedEntryProperty = new SimpleObjectProperty[LogEntry]()

  def setSelectedEntry(e: LogEntry): Unit = selectedEntryProperty.set(e)

  /** will be set by mouse clicks / movements */
  val selectedIndexProperty = new SimpleIntegerProperty()

  def setSelectedIndex(i: Int): Unit = selectedIndexProperty.set(i)

  /** responsible for determining current logevent */
  val mouseEventHandler = new EventHandler[MouseEvent]() {
    override def handle(me: MouseEvent): Unit = {
      val index = LogReport.indexOf(me.getX.toInt, me.getY.toInt, squareWidth, getCanvasWidth())
      val entry = entries.get(index)
      setSelectedIndex(index)
      setSelectedEntry(entry)
    }
  }

  val view = {
    val i = mkBareImage(entries, squareWidth, canvasWidth)
    val iv = new ImageView(i)
    iv.setOnMouseMoved(mouseEventHandler)
    iv
  }

  setCenter(new ScrollPane(view))

  def doRepaint(sWidth: Int, cWidth: Int): Unit = {
    setCanvasWidth(cWidth)
    val i = timeR(paint(entries, sWidth, cWidth), "repainting")
    view.setImage(i)
  }

  def mkBareImage(entries: java.util.List[LogEntry], squareWidth: Int, canvasWidth: Int): WritableImage = {
    val numberCols = canvasWidth / squareWidth
    val numRows = entries.size() / numberCols
    val height = squareWidth * numRows
    new WritableImage(canvasWidth + squareWidth, height + squareWidth)
  }

  def paint(entries: java.util.List[LogEntry], squareWidth: Int, canvasWidth: Int): WritableImage = {
    val wi = mkBareImage(entries, squareWidth, canvasWidth)

    val numberCols = canvasWidth / squareWidth
    val pw = wi.getPixelWriter
    for ((e, i) <- entries.asScala.zipWithIndex) {
      paintSquare(pw, (i % numberCols) * squareWidth, (i / numberCols) * squareWidth, squareWidth.toInt, e.severity.color)
    }
    wi
  }

  def paintSquare(pw: PixelWriter, u: Int, v: Int, length: Int, c: Color): PixelWriter = {
    for {x <- u until (u + length - 1)
         y <- v until (v + length - 1)} {
      pw.setColor(x, y, c)
    }
    pw
  }

}