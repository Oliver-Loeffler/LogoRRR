package app.logorrr.views

import app.logorrr.conf.LogoRRRGlobals
import app.logorrr.views.block.{HasBlockSizeProperty, RectButton}
import app.logorrr.views.search.{FiltersToolBar, SearchToolBar}
import app.logorrr.views.text.TextSizeButton
import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.{Orientation, Pos}
import javafx.scene.control.{Control, Separator, ToolBar, Tooltip}
import javafx.scene.layout.{BorderPane, HBox}
import javafx.scene.paint.Color

/**
 * Container to horizontally align search, filters and settings
 */
class OpsBorderPane(pathAsString: String
                    , searchToolBar: SearchToolBar
                    , filtersToolBar: FiltersToolBar
                    , settingsToolBar: SettingsToolBar)
  extends HBox
    with HasBlockSizeProperty {

  def setFontSize(fontSize: Int): Unit =
    LogoRRRGlobals.getLogFileSettings(pathAsString).setFontSize(fontSize)

  def getFontSize(): Int = LogoRRRGlobals.getLogFileSettings(pathAsString).getFontSize()

  /** increment/decrement block size */
  val blockSizeStep = 4

  /** increment / decrement font size */
  val fontSizeStep = 2

  // bound to global var
  override val blockSizeProperty: SimpleIntegerProperty = new SimpleIntegerProperty()

  val fontSizeProperty: SimpleIntegerProperty = new SimpleIntegerProperty()

  val items: Seq[Control] = {
    val smallerRectBtn =
      new RectButton(2 * blockSizeStep, 2 * blockSizeStep, Color.GRAY,
        _ => {
          if (getBlockSize() - blockSizeStep > 0) {
            setBlockSize(getBlockSize() - blockSizeStep)
          }
        })
    smallerRectBtn.setTooltip(new Tooltip("decrease block size"))
    val biggerRectBtn =
      new RectButton(3 * blockSizeStep, 3 * blockSizeStep, Color.GRAY
        , _ => {
          if (getBlockSize() + blockSizeStep < 10 * blockSizeStep) {
            setBlockSize(getBlockSize() + blockSizeStep)
          }
        })
    biggerRectBtn.setTooltip(new Tooltip("increase block size"))
    val smallerTextBtn =
      new TextSizeButton(6,
        _ => {
          if (getFontSize() - fontSizeStep > 0) {
            setFontSize(getFontSize() - fontSizeStep)
          }
        })
    smallerTextBtn.setTooltip(new Tooltip("decrease text size"))
    val biggerTextBtn =
      new TextSizeButton(9,
        _ => {
          if (getFontSize() + fontSizeStep < 10 * fontSizeStep) {
            setFontSize(getFontSize() + fontSizeStep)
          }
        })
    biggerTextBtn.setTooltip(new Tooltip("increase text size"))
    Seq(smallerRectBtn, biggerRectBtn, smallerTextBtn, biggerTextBtn)
  }

  BorderPane.setAlignment(searchToolBar, Pos.CENTER_LEFT)
  searchToolBar.setMaxHeight(Double.PositiveInfinity)

  // setLeft(searchToolBar)

  private val box = new ToolBar(items: _*)
  box.setMinWidth(150)
   box.setMaxHeight(Double.PositiveInfinity)
  //  BorderPane.setAlignment(box, Pos.CENTER_LEFT)
  //  setCenter(box)

  filtersToolBar.setMaxHeight(Double.PositiveInfinity)
  //  BorderPane.setAlignment(filtersToolBar, Pos.CENTER_LEFT)
  //  setRight(filtersToolBar)
  val is = Seq(searchToolBar, box,  filtersToolBar)
  setAlignment(Pos.CENTER_LEFT)
  getChildren.addAll(is: _*)


}

