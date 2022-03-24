package app.logorrr.views

import app.logorrr.conf.SettingsIO
import app.logorrr.model.{LogEntries, LogEntry, LogEntryInstantFormat, LogFileSettings}
import app.logorrr.util.{CanLog, CollectionUtils, JfxUtils, LogEntryListener, LogoRRRFonts}
import app.logorrr.views.main.LogoRRRGlobals
import app.logorrr.views.visual.LogVisualView
import javafx.application.HostServices
import javafx.beans.binding.{Bindings, StringExpression}
import javafx.beans.property.{SimpleBooleanProperty, SimpleIntegerProperty, SimpleListProperty, SimpleObjectProperty}
import javafx.beans.value.ChangeListener
import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.ListChangeListener
import javafx.collections.transformation.FilteredList
import javafx.geometry.Pos
import javafx.scene.control._
import javafx.scene.layout._
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import org.apache.commons.io.input.Tailer

import scala.jdk.CollectionConverters._

object LogFileTab {

  def apply(hostServices: HostServices
            , logViewTabPane: LogViewTabPane
            , logFile: LogEntries
            , logFileDefinition: LogFileSettings
            , initFileMenu: => Unit): LogFileTab = {
    val logFileTab = new LogFileTab(
      hostServices
      , logFile
      , logViewTabPane.sceneWidthProperty.get()
      , logFileDefinition
      , initFileMenu)


    /** activate invalidation listener on filtered list */
    logFileTab.init()
    logFileTab.sceneWidthProperty.bind(logViewTabPane.sceneWidthProperty)
    logFileTab
  }

}


/**
 * Represents a single 'document' UI approach for a log file.
 *
 * One can view / interact with more than one log file at a time, using tabs here feels quite natural.
 *
 * @param logEntries report instance holding information of log file to be analyzed
 * */
class LogFileTab(hostServices: HostServices
                 , val logEntries: LogEntries
                 , val initialSceneWidth: Int
                 , val initialLogFileDefinition: LogFileSettings
                 , initFileMenu: => Unit)
  extends Tab
    with CanLog {

  val tailer = new Tailer(initialLogFileDefinition.path.toFile, new LogEntryListener(logEntries.values), 1000, true)

  /** start observing log file for changes */
  def startTailer(): Unit = new Thread(tailer).start()

  /** stop observing changes */
  def stopTailer(): Unit = tailer.stop()

  /** is set to false if logview was painted at least once (see repaint) */
  val neverPaintedProperty = new SimpleBooleanProperty(true)

  /** repaint if entries or filters change */
  val repaintInvalidationListener: InvalidationListener = (_: Observable) => repaint()

  /** list of search filters to be applied to a Log Report */
  val filtersListProperty = new SimpleListProperty[Filter](CollectionUtils.mkEmptyObservableList())

  /** bound to sceneWidthProperty of parent LogViewTabPane */
  val sceneWidthProperty = new SimpleIntegerProperty(initialSceneWidth)


  /** split visual view and text view */
  val splitPane = new SplitPane()

  /** list which holds all entries, default to display all (can be changed via buttons) */
  val filteredList = new FilteredList[LogEntry](logEntries.values)

  private val searchToolBar = new SearchToolBar(addFilter)

  private val filtersToolBar = {
    val fbtb = new FiltersToolBar(filteredList, logEntries.values.size, removeFilter)
    fbtb.filtersProperty.bind(filtersListProperty)
    fbtb
  }

  val settingsToolBar = new SettingsToolBar(hostServices, initialLogFileDefinition)

  val opsBorderPane: BorderPane = new OpsBorderPane(searchToolBar, filtersToolBar, settingsToolBar)

  val initialWidth = (sceneWidth * initialLogFileDefinition.dividerPosition).toInt

  private lazy val logVisualView = {
    val lvv = new LogVisualView(filteredList.asScala
      , initialWidth)
    lvv.sisp.filtersListProperty.bind(filtersListProperty)
    lvv
  }

  private val logTextView = new LogTextView(filteredList, logEntries.timings)

  val entryLabel = {
    val l = new Label("")
    l.prefWidthProperty.bind(sceneWidthProperty)
    l.setStyle(LogoRRRFonts.jetBrainsMono(20))
    l
  }


  /** to share state between visual view and text view. index can be selected by navigation in visual view */
  val selectedIndexProperty = new SimpleIntegerProperty()

  val selectedEntryProperty = new SimpleObjectProperty[LogEntry]()

  private val logEntryChangeListener: ChangeListener[LogEntry] = JfxUtils.onNew[LogEntry](updateEntryLabel)


  /** if a change event for filtersList Property occurs, save it to disc */
  def initFiltersPropertyListChangeListener(): Unit = {
    filtersListProperty.addListener(JfxUtils.mkListChangeListener(handleFilterChange))
  }

  private def handleFilterChange(change: ListChangeListener.Change[_ <: Filter]): Unit = {
    while (change.next()) {
      val updatedDefinition = initialLogFileDefinition.copy(filters = filtersListProperty.asScala.toSeq)
      SettingsIO.updateRecentFileSettings(rf => rf.update(updatedDefinition))
    }
  }

  def init(): Unit = {
    initialLogFileDefinition.filters.foreach(addFilter)

    /** top component for log view */
    val borderPane = new BorderPane()
    borderPane.setTop(opsBorderPane)
    borderPane.setCenter(splitPane)
    borderPane.setBottom(entryLabel)

    setContent(borderPane)

    /** don't monitor file anymore if tab is closed, free invalidation listeners */
    setOnClosed(_ => closeTab())


    textProperty.bind(computeTabTitle)
    // textProperty.bind(logFile.titleProperty)
    selectedIndexProperty.bind(logVisualView.selectedIndexProperty)

    selectedIndexProperty.addListener(JfxUtils.onNew[Number](selectEntry))

    selectedEntryProperty.bindBidirectional(logVisualView.selectedEntryProperty)

    selectedEntryProperty.addListener(logEntryChangeListener)

    // if user changes selected item in listview, change footer as well
    logTextView.listView.getSelectionModel.selectedItemProperty.addListener(logEntryChangeListener)

    splitPane.getItems.addAll(logVisualView, logTextView)


    /**
     * we are interested just in the first divider. If it changes its position (which means the user interacts) then
     * update logVisualView
     * */
    splitPane.getDividers.get(0).positionProperty().addListener(JfxUtils.onNew {
      t1: Number =>
        val width = t1.doubleValue() * splitPane.getWidth
        SettingsIO.updateDividerPosition(initialLogFileDefinition.path, t1.doubleValue())
        repaint(width)
    })

    startTailer()

    setDivider(initialLogFileDefinition.dividerPosition)
    initFiltersPropertyListChangeListener()
    installInvalidationListener()
  }

  /** compute title of tab */
  private def computeTabTitle: StringExpression = {
    Bindings.concat(initialLogFileDefinition.pathAsString, " (", Bindings.size(logEntries.values).asString, " lines)")
  }

  /**
   * Actions to perform if tab is closed:
   *
   * - end monitoring of file
   * - update config file
   * - update file menu
   *
   */
  def closeTab(): Unit = {
    SettingsIO.updateRecentFileSettings(rf => rf.remove(initialLogFileDefinition.path.toAbsolutePath.toString))
    initFileMenu
    shutdown()
  }

  def shutdown(): Unit = {
    logInfo(s"Closing file ${initialLogFileDefinition.path.toAbsolutePath} ...")
    uninstallInvalidationListener()
    stopTailer()
  }

  def sceneWidth = sceneWidthProperty.get()


  def installInvalidationListener(): Unit = {
    // to detect when we apply a new filter via filter buttons (see FilterButtonsToolbar)
    filteredList.predicateProperty().addListener(repaintInvalidationListener)
    logEntries.values.addListener(repaintInvalidationListener)
    // if application changes width this will trigger repaint (See Issue #9)
    splitPane.widthProperty().addListener(repaintInvalidationListener)
  }

  def uninstallInvalidationListener(): Unit = {
    filteredList.predicateProperty().removeListener(repaintInvalidationListener)
    logEntries.values.removeListener(repaintInvalidationListener)
  }

  def selectEntry(number: Number): Unit = logTextView.selectEntryByIndex(number.intValue)

  def updateEntryLabel(logEntry: LogEntry): Unit = {
    Option(logEntry) match {
      case Some(entry) =>
        val background: Background = entry.background(filtersToolBar.filterButtons.keys.toSeq)
        entryLabel.setBackground(background)
        entryLabel.setTextFill(entry.calcColor(filtersToolBar.filterButtons.keys.toSeq).invert())
        entryLabel.setText(entry.value)
      case None =>
        entryLabel.setBackground(null)
        entryLabel.setText("")
    }
  }

  def setDivider(pos: Double): Unit = splitPane.getDividers.get(0).setPosition(pos)

  def addFilter(filter: Filter): Unit = filtersListProperty.add(filter)

  def removeFilter(filter: Filter): Unit = {
    filtersListProperty.remove(filter)
  }

  def getVisualViewWidth(): Double = {
    val w = splitPane.getDividers.get(0).getPosition * splitPane.getWidth
    if (w != 0.0) {
      w
    } else {
      initialWidth
    }
  }

  /** width can be negative as well, we have to guard about that. also we repaint only if view is visible. */
  def repaint(width: Double = getVisualViewWidth()): Unit = {
    val squareWidth = LogoRRRGlobals.settings.squareImageSettings.widthProperty.get
    if (isSelected && (neverPaintedProperty.get() || isSelected && width > 0 && width > squareWidth * 4)) { // at minimum we want to have 4 squares left (an arbitrary choice)
      neverPaintedProperty.set(false)
      logVisualView.repaint(width.toInt)
    } else {
      logTrace(s"Not painting since neverPainted: ${neverPaintedProperty.get} isSelected: $isSelected && $width > 0 && $width > ${squareWidth * 4}")
    }
  }


}