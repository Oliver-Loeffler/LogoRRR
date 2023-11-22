package app.logorrr.views.text

import app.logorrr.views.ops.TextSizeButton
import app.logorrr.views.search.OpsToolBar

/**
 * UI element for changing text size
 *
 * @param pathAsString to resolve current log file
 */
class IncreaseTextSizeButton(val pathAsString: String) extends TextSizeButton(16, "increase text size") {

  setOnAction(_ => {
    if (getFontSize + OpsToolBar.fontSizeStep < 50 * OpsToolBar.fontSizeStep) {
      setFontSize(getFontSize + OpsToolBar.fontSizeStep)
    }
  })
}



