package app.logorrr.views

import app.logorrr.model.LogEntry
import app.logorrr.util.JfxUtils
import javafx.beans.property.SimpleListProperty
import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.ListChangeListener
import javafx.collections.transformation.FilteredList
import javafx.scene.control.{Button, ToggleButton, ToolBar}
import javafx.scene.shape.Rectangle

import java.text.DecimalFormat
import scala.jdk.CollectionConverters._

/** A toolbar with buttons which filter log events */
object FiltersToolBar {

  val percentFormatter = new DecimalFormat("#.##")

  def percentAsString(value: Int, totalSize: Int): String = {
    percentFormatter.format((100 * value.toDouble) / totalSize.toDouble) + "%"
  }

  class RemoveButton(filter: Filter, removeFilter: Filter => Unit) extends Button("x") {
    setDisable(filter.isInstanceOf[UnclassifiedFilter])
    setOnAction(_ => removeFilter(filter))
  }

}


/**
 * Depending on buttons pressed, filteredList will be mutated to show only selected items.
 *
 * @param filteredList list of entries which are displayed (can be filtered via buttons)
 * @param totalSize    number of all entries
 */
class FiltersToolBar(filteredList: FilteredList[LogEntry]
                     , totalSize: Int
                     , removeFilter: Filter => Unit) extends ToolBar {

  val filtersProperty = new SimpleListProperty[Filter]()

  filtersProperty.addListener(JfxUtils.mkListChangeListener[Filter](processFiltersChange))

  /** if list is changed in any way, react to this event and either add or remove filter from UI */
  private def processFiltersChange(change: ListChangeListener.Change[_ <: Filter]): Unit = {
    while (change.next()) {
      if (change.wasAdded()) {
        change.getAddedSubList.asScala.foreach(addSearchTag)
        updateUnclassified()
      } else if (change.wasRemoved()) {
        change.getRemoved.asScala.foreach(removeSearchTag)
        updateUnclassified()
      }
    }
  }

  var filterButtons: Map[Filter, SearchTag] = Map[Filter, SearchTag]()

  var someUnclassifiedFilter: Option[(Filter, SearchTag)] = None

  var occurrences: Map[Filter, Int] = Map().withDefaultValue(0)

  def allFilters: Set[Filter] = filterButtons.keySet ++ someUnclassifiedFilter.map(x => Set(x._1)).getOrElse(Set())

  private def updateOccurrences(sf: Filter): Unit = {
    occurrences = occurrences + (sf -> filteredList.getSource.asScala.count(e => sf.matcher.applyMatch(e.value)))
  }

  private def updateUnclassified(): Unit = {
    val unclassified = new UnclassifiedFilter(filterButtons.keySet)
    updateOccurrences(unclassified)
    val searchTag = SearchTag(unclassified, occurrences, totalSize, updateActiveFilter, removeFilter)
    someUnclassifiedFilter.foreach(ftb => getItems.remove(ftb._2))
    getItems.add(0, searchTag)
    someUnclassifiedFilter = Option((unclassified, searchTag))
    updateActiveFilter()
  }

  /**
   * Filters are only active if selected.
   *
   * UnclassifiedFilter gets an extra handling since it depends on other filters
   *
   * @return
   */
  def computeCurrentFilter(): Filter = {
    new AnyFilter(someUnclassifiedFilter.map(fst => if (fst._2.toggleButton.isSelected) Set(fst._1) else Set()).getOrElse(Set()) ++
      filterButtons.filter(fst => fst._2.toggleButton.isSelected).keySet)
  }

  private def addSearchTag(filter: Filter): Unit = {
    updateOccurrences(filter)
    val searchTag = SearchTag(filter, occurrences, totalSize, updateActiveFilter, removeFilter)
    getItems.add(searchTag)
    filterButtons = filterButtons + (filter -> searchTag)
  }

  private def removeSearchTag(filter: Filter): Unit = {
    getItems.remove(filterButtons(filter))
    filterButtons = filterButtons - filter
  }

  def updateActiveFilter(): Unit = {
    val filter = computeCurrentFilter()
    filteredList.setPredicate((entry: LogEntry) => filter.matcher.applyMatch(entry.value))
  }

}

