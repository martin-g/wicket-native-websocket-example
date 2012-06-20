package org.apache.wicket.websocket.jetty.example

import akka.util.duration._
import akka.actor.{Props, Actor, ActorSystem}
import org.apache.wicket.Application
import org.apache.wicket.protocol.ws.api.SimpleWebSocketConnectionRegistry
import akka.event.Logging

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
  private val master = akkaSystem.actorOf(Props(new MasterActor(application.getName)), "master")

  /**
   * Artificial event notification. Just sends a message to all connected clients every 3 seconds
   */
  akkaSystem.scheduler.schedule(1 second , 3 seconds, master, UpdateClients)

  /**
   * The messages for the main actor
   */
  private sealed trait MasterMessage
  private case class ClientConnect(applicationName: String, sessionId: String, pageId: Int) extends MasterMessage
  private case class ClientDisconnect(applicationName: String, sessionId: String, pageId: Int) extends MasterMessage
  private case object UpdateClients extends MasterMessage

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

  /**
   * The main actor that keeps track of all connected clients
   *
   * @param appName
   *    the web application to look in
   */
  private class MasterActor(val appName: String) extends Actor
  {
    protected def receive = {

      case ClientConnect(applicationName, sessionId, pageId) => {
        val worker = Props(new WorkerActor()) // TODO for some reason Props[WorkerActor] fails at instantiation ?!
        val demoActor = context.actorOf(worker, getActorName(sessionId, pageId))
        demoActor ! Connected(applicationName, sessionId, pageId)
      }

      case ClientDisconnect(applicationName, sessionId, pageId) => {
        val workerActor = context.actorFor(getActorName(sessionId, pageId))
        workerActor ! Disconnected
      }

      case UpdateClients => {
        // notifies all currently registered clients
        context.children.foreach( child =>
          child ! Update(appName)
        )
      }
    }
    
    private def getActorName(sessionId: String,  pageId: Int) = "workerActor." + sessionId + "." + pageId
  }

  /**
   * The messages for the worker actors.
   * There is one worker actor for each connected client
   */
  private sealed trait WorkerMessage
  private case class Connected(applicationName: String, sessionId: String, pageId: Int) extends WorkerMessage
  private case object Disconnected extends WorkerMessage
  private case class Update(applicationName: String) extends WorkerMessage

  /**
   * The actor that is registered for each WebSocket connection
   */
  private class WorkerActor extends Actor
  {
    var client: Option[Connected] = None
    val logger = Logging(context.system, this)

    protected def receive = {
      case Connected(applicationName, sessionId, pageId) => {

        logger.info("Client with session id '{}' and page id '{}' has connected\n", sessionId, pageId)
        client = Some(Connected(applicationName, sessionId, pageId))
      }
      case Disconnected => {
        logger.info("Client with session id '{}' and page id '{}' has disconnected\n",
          client.get.sessionId, client.get.pageId)
        client = None
        context.stop(self)
      }
      case Update(applicationName) => {
        client.foreach(c => {
          val application = Application.get(c.applicationName)
          val connectionRegistry = new SimpleWebSocketConnectionRegistry();
          val webSocketConnection = connectionRegistry.getConnection(application, c.sessionId, c.pageId)
          webSocketConnection.sendMessage("A message pushed asynchronously by Akka directly to the plain WebSocketConnection!")
        })
      }
    }
  }
}
