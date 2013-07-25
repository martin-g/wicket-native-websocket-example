package org.apache.wicket.websocket.jetty.example

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Props, Actor, ActorSystem}
import org.apache.wicket.Application
import org.apache.wicket.protocol.ws.api.IWebSocketConnectionRegistry
import akka.event.Logging
import org.apache.wicket.protocol.ws.IWebSocketSettings
import scala.collection.script.Update
import org.apache.wicket.websocket.jetty.example.EventSystem.{Disconnected, Connected, ClientConnect, UpdateClients, ClientDisconnect}

object EventSystem {

  /**
   * The messages for the main actor
   */
  sealed trait MasterMessage
  case class ClientConnect(applicationName: String, sessionId: String, pageId: Int) extends MasterMessage
  case class ClientDisconnect(applicationName: String, sessionId: String, pageId: Int) extends MasterMessage
  case object UpdateClients extends MasterMessage


  /**
   * The messages for the worker actors.
   * There is one worker actor for each connected client
   */
  sealed trait WorkerMessage
  case class Connected(applicationName: String, sessionId: String, pageId: Int) extends WorkerMessage
  case object Disconnected extends WorkerMessage
  case object Update extends WorkerMessage

}

/**
 * A simple event system based on Akka that tracks the currently connected web socket clients
 * and artificially generates server side events and notifies the clients.
 *
 * This event system is just a demo of a global registry of connected clients.
 *
 * @since 6.0
 */
class EventSystem(val application: Application)
{
  /**
   * Starts Akka
   */
  private val akkaSystem = ActorSystem("webSocketDemo")

  /**
   * The main actor that tracks all connected clients. It creates one child actor for each client
   */
  private val master = akkaSystem.actorOf(Props(classOf[MasterActor], application.getName), "master")

  /**
   * Artificial event notification. Just sends a message to all connected clients every 3 seconds
   */
  akkaSystem.scheduler.schedule(1 second , 3 seconds, master, UpdateClients)

  /**
   * Registers a new client
   *
   * @param applicationName
   *      the web application to look in
   * @param sessionId
   *      the web socket client session id
   * @param pageId
   *      the web socket client page id
   */
  def clientConnected(applicationName: String, sessionId: String, pageId: Int) {
    master ! ClientConnect(applicationName, sessionId, pageId)
  }

  /**
   * Unregisters a client
   *
   * @param applicationName
   *      the web application to look in
   * @param sessionId
   *      the web socket client session id
   * @param pageId
   *      the web socket client page id
   */
  def clientDisconnected(applicationName: String, sessionId: String, pageId: Int) {
    master ! ClientDisconnect(applicationName, sessionId, pageId)
  }

  /**
   * Disposes Akka structures
   */
  def shutdown() { akkaSystem.shutdown() }
}


/**
 * The actor that is registered for each WebSocket connection
 */
private class WorkerActor extends Actor
{
  var client: Option[Connected] = None
  val logger = Logging(context.system, this)

  def receive = {
    case c @ Connected(applicationName, sessionId, pageId) => {

      logger.info("Client with session id '{}' and page id '{}' has connected\n", sessionId, pageId)
      client = Some(c)
    }
    case Disconnected => {
      logger.info("Client with session id '{}' and page id '{}' has disconnected\n", client.get.sessionId, client.get.pageId)
      client = None
      context.stop(self)
    }
    case Update => {
      client.foreach(c => {
        val application = Application.get(c.applicationName)
        val settings: IWebSocketSettings = IWebSocketSettings.Holder.get(application)
        val connectionRegistry: IWebSocketConnectionRegistry = settings.getConnectionRegistry
        val webSocketConnection = connectionRegistry.getConnection(application, c.sessionId, c.pageId)
        webSocketConnection.sendMessage("A message pushed asynchronously by Akka directly to the plain WebSocketConnection!")
      })
    }
  }
}

/**
 * The main actor that keeps track of all connected clients
 *
 * @param appName
   *    the web application to look in
 */
private class MasterActor(appName: String) extends Actor
{
  def receive = {

    case ClientConnect(applicationName, sessionId, pageId) => {
      val worker = Props(classOf[WorkerActor])
      val demoActor = context.actorOf(worker, getActorName(sessionId, pageId))
      demoActor ! Connected(applicationName, sessionId, pageId)
    }

    case ClientDisconnect(applicationName, sessionId, pageId) => {
      val workerActor = context.actorSelection(getActorName(sessionId, pageId))
      workerActor ! Disconnected
    }

    case UpdateClients => {
      // notifies all currently registered clients
      context.children.foreach( child =>
        child ! Update
      )
    }
  }

  private def getActorName(sessionId: String,  pageId: Int) = "workerActor." + sessionId + "." + pageId
}
