import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.{Button, TableColumn, TableView, Tab, TabPane, TextField}
import scalafx.scene.layout.{HBox, VBox}
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.{BooleanProperty, IntegerProperty, StringProperty}
import scalafx.Includes._
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import upickle.default._
import java.nio.file.{Files, Paths}

case class Task(id: Int, description: String, completed: Boolean = false) {
  val idProp = IntegerProperty(id)
  val descProp = StringProperty(description)
  val completedProp = BooleanProperty(completed)
}

object Task {
  implicit val rw: ReadWriter[Task] = macroRW
}

class TaskListPane(tabName: String, lastListFile: String, tabPane: TabPane) {
  val tasks = ObservableBuffer[Task]()

  // Load the last-used task list for this tab on startup
  if (Files.exists(Paths.get(lastListFile))) {
    try {
      val lastFilePath = Files.readString(Paths.get(lastListFile)).trim
      if (lastFilePath.nonEmpty && Files.exists(Paths.get(lastFilePath))) {
        val json = Files.readString(Paths.get(lastFilePath))
        val loadedTasks = read[Seq[Task]](json)
        tasks ++= loadedTasks
      }
    } catch {
      case e: Exception => println(s"Failed to load last task list for $tabName: ${e.getMessage}")
    }
  }

  val taskField = new TextField {
    promptText = s"Enter task description for $tabName"
  }

  val addButton = new Button("Add Task") {
    onAction = _ => {
      val desc = taskField.text.value.trim
      if (desc.nonEmpty) {
        tasks += Task(tasks.length + 1, desc)
        taskField.text = ""
      }
    }
  }

  val table = new TableView[Task](tasks)

  table.columns ++= Seq(
    new TableColumn[Task, Int] {
      text = "ID"
      cellValueFactory = { cellData =>
        cellData.value.idProp.asInstanceOf[scalafx.beans.value.ObservableValue[Int, Int]]
      }
    },
    new TableColumn[Task, String] {
      text = "Description"
      cellValueFactory = { cellData =>
        cellData.value.descProp.asInstanceOf[scalafx.beans.value.ObservableValue[String, String]]
      }
    },
    new TableColumn[Task, Boolean] {
      text = "Completed"
      cellValueFactory = { cellData =>
        cellData.value.completedProp.asInstanceOf[scalafx.beans.value.ObservableValue[Boolean, Boolean]]
      }
      cellFactory = { (col: TableColumn[Task, Boolean]) =>
        new scalafx.scene.control.TableCell[Task, Boolean] {
          item.onChange { (_, _, newValue) =>
            if (table.getItems.size() > index.value && index.value >= 0) {
              graphic = new scalafx.scene.control.CheckBox {
                selected = newValue
                onAction = _ => {
                  val selectedTask = table.selectionModel.value.getSelectedItem
                  if (selectedTask != null) {
                    val index = tasks.indexOf(selectedTask)
                    tasks(index) = selectedTask.copy(completed = selected.value)
                    tasks(index).completedProp.value = selected.value
                  }
                }
              }
            } else {
              graphic = null
            }
          }
        }
      }
    }
  )

  val deleteButton = new Button("Delete Selected") {
    onAction = _ => {
      val selectedTask = table.selectionModel.value.getSelectedItem
      if (selectedTask != null) {
        tasks -= selectedTask
      }
    }
  }

  val saveButton = new Button("Save List") {
    onAction = _ => {
      val fileChooser = new FileChooser {
        title = s"Save Task List for $tabName"
        extensionFilters.add(new ExtensionFilter("JSON Files", "*.json"))
      }
      val selectedFile = fileChooser.showSaveDialog(null)
      if (selectedFile != null) {
        try {
          val json = write(tasks.toSeq)
          Files.writeString(selectedFile.toPath, json)
          Files.writeString(Paths.get(lastListFile), selectedFile.getAbsolutePath)
        } catch {
          case e: Exception => println(s"Failed to save task list for $tabName: ${e.getMessage}")
        }
      }
    }
  }

  val loadButton = new Button("Load List") {
    onAction = _ => {
      val fileChooser = new FileChooser {
        title = s"Load Task List for $tabName"
        extensionFilters.add(new ExtensionFilter("JSON Files", "*.json"))
      }
      val selectedFile = fileChooser.showOpenDialog(null)
      if (selectedFile != null) {
        try {
          val json = Files.readString(selectedFile.toPath)
          val loadedTasks = read[Seq[Task]](json)
          tasks.clear()
          tasks ++= loadedTasks
          Files.writeString(Paths.get(lastListFile), selectedFile.getAbsolutePath)
        } catch {
          case e: Exception => println(s"Failed to load task list for $tabName: ${e.getMessage}")
        }
      }
    }
  }

  val pane = new VBox(10) {
    children = Seq(taskField, addButton, deleteButton, saveButton, loadButton, table)
    padding = scalafx.geometry.Insets(10)
  }

  val tab = new Tab {
    text = tabName
    content = pane
    onClosed = _ => {
      try {
        Files.deleteIfExists(Paths.get(lastListFile))
      } catch {
        case e: Exception => println(s"Failed to delete $lastListFile: ${e.getMessage}")
      }
    }
  }
}

object TaskManagerGUI extends JFXApp3 {
  override def start(): Unit = {
    val tabPane = new TabPane()
    val tabListFile = "tab_list.txt"
    var tabCounter = 1

    // Load the list of tabs from the previous session
    val initialTabs = if (Files.exists(Paths.get(tabListFile))) {
      try {
        val tabNames = Files.readString(Paths.get(tabListFile)).trim.split("\n").toList
        tabNames.map(name => (name, s"last_list_$name.txt"))
      } catch {
        case e: Exception => println(s"Failed to load tab list: ${e.getMessage}"); List(("List 1", "last_list_tab1.txt"))
      }
    } else {
      List(("List 1", "last_list_tab1.txt"))
    }

    // Create initial tabs
    initialTabs.foreach { case (name, lastListFile) =>
      val taskListPane = new TaskListPane(name, lastListFile, tabPane)
      tabPane.tabs.add(taskListPane.tab)
      tabCounter = name.replace("List ", "").toInt + 1
    }

    // Add a "+" button to create new tabs
    val addTabButton = new Button("+") {
      onAction = _ => {
        val newTabName = s"List $tabCounter"
        val newLastListFile = s"last_list_tab$tabCounter.txt"
        val newTaskListPane = new TaskListPane(newTabName, newLastListFile, tabPane)
        tabPane.tabs.add(newTaskListPane.tab)
        tabPane.selectionModel.value.select(newTaskListPane.tab)
        tabCounter += 1
      }
    }

    // Layout with TabPane and "+" button
    val tabBar = new HBox(5) {
      children = Seq(tabPane, addTabButton)
    }

    val layout = new VBox(10) {
      children = Seq(tabBar)
      padding = scalafx.geometry.Insets(10)
    }

    stage = new PrimaryStage {
      title = "Task Manager"
      scene = new Scene(layout, 400, 400)
    }

    // Save the list of tabs on close
    stage.onCloseRequest = _ => {
      try {
        val tabNames = tabPane.tabs.toList.map(_.text.value).mkString("\n")
        Files.writeString(Paths.get(tabListFile), tabNames)
      } catch {
        case e: Exception => println(s"Failed to save tab list: ${e.getMessage}")
      }
    }
  }
}
