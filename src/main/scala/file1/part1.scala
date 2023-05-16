package file1


import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}

import java.io.{BufferedWriter, FileWriter}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.DurationInt

object LoggerActor {
  def props(): Props = Props(new LoggerActor)

  case class LogWarn(message: String)
  case class LogInfo(message: String)
  case class RenameLogFile(newName: String)
}

class LoggerActor extends Actor with ActorLogging {
  import LoggerActor._

  private val timestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
  private var logFile: Option[BufferedWriter] = openLogFile("log") // Initialize logFile with default log file name

  override def preStart(): Unit = {

    log.info("Initiation is started")
  }

  // close the stuff on termination
  override def postStop(): Unit = {

    log.info("Stopping the process")
  }

  var supervision = OneForOneStrategy() {
    case _: Exception => Restart

  }
  def receive: Receive = {
    case LogWarn(message) =>

      val timestamp = LocalDateTime.now().format(timestampFormat)
      writeLog(s"[WARN][$timestamp] $message")
      log.info(s"[WARN][$timestamp] $message")

    case LogInfo(message) =>

      val timestamp = LocalDateTime.now().format(timestampFormat)
      writeLog(s"[INFO][$timestamp] $message")
      log.info(s"[INFO][$timestamp] $message")
    case RenameLogFile(newName) =>
      closeLogFile(logFile)
      logFile = openLogFile(newName)
      log.info(s"FileRenamed successfully by new name: $newName")
    case number:Int => throw new Exception("9")
  }

  private def openLogFile(name: String): Option[BufferedWriter] = {
    try {
      val fileWriter = new FileWriter(s"$name.log", true)
      val bufferedWriter = new BufferedWriter(fileWriter)
      Some(bufferedWriter)
    } catch {
      case e: Exception =>
        log.error("Failed to open log file", e)
        None
    }
  }

  private def writeLog(logMessage: String): Unit = {
    logFile match {
      case Some(file) =>
        try {
          file.write(logMessage)
          file.newLine()
          file.flush()
        } catch {
          case e: Exception =>
            log.error("Failed to write log message", e)
        }

      case None =>
        log.error("Log file is not open")
    }
  }

  private def closeLogFile(file: Option[BufferedWriter]): Unit = {
    file.foreach { f =>
      try {
        f.close()
      } catch {
        case e: Exception =>
          log.error("Failed to close log file", e)
      }
    }
  }
}

import akka.actor.ActorSystem
object Part1 extends App {
  import LoggerActor._

  // Create the ActorSystem
  val system = ActorSystem("LoggerSystem")

  // Create an instance of the LoggerActor
  val loggerActor = system.actorOf(LoggerActor.props(), "loggerActor")

  // Send log messages to the LoggerActor
  loggerActor ! LogWarn("This is a warning message.")
  loggerActor ! LogInfo("This is an info message.")

  // Rename the log file
  loggerActor ! RenameLogFile("newlog")


  // Supervision
  loggerActor ! 667

  // Stop the ActorSystem

  import system.dispatcher

  system.terminate()
  system.scheduler.scheduleOnce(15.seconds) {
    system.terminate()
  }
}
