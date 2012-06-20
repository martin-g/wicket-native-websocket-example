package org.apache.wicket.websocket.jetty.example

import org.apache.wicket.markup.head.{IHeaderResponse, JavaScriptHeaderItem}
import org.apache.wicket.markup.html.WebPage
import org.apache.wicket.request.resource.PackageResourceReference
import org.apache.wicket.ajax.markup.html.AjaxLink
import org.apache.wicket.ajax.{AjaxRequestTarget, WebSocketRequestHandler}
import org.apache.wicket.markup.html.panel.FeedbackPanel
import org.apache.wicket.protocol.ws.api.{SimpleWebSocketConnectionRegistry, WebSocketBehavior}
import org.apache.wicket.protocol.ws.api.message.{ClosedMessage, ConnectedMessage, TextMessage}

/**
 * A (ugly) demo page for WebSocketBehavior
 */
class WebSocketDemo extends WebPage {

  val feedback = new FeedbackPanel("feedback");
  feedback.setOutputMarkupId(true)
  add(feedback)

  add(new WebSocketBehavior()
  {
    protected override def onConnect(message: ConnectedMessage)
    {
      val application = message.getApplication
      val sessionId = message.getSessionId
      val pageId = message.getPageId
      // register in the global registry. Optional
      NativeWebSocketExampleApplication.get.getEventSystem.clientConnected(application.getName, sessionId, pageId)
    }

    protected override def onClose(message: ClosedMessage)
    {
      val application = message.getApplication
      val sessionId = message.getSessionId
      val pageId = message.getPageId
      // unregister in the global registry. Optional
      NativeWebSocketExampleApplication.get.getEventSystem.clientDisconnected(application.getName, sessionId, pageId)
    }

    /**
     * A callback called when a text based message is sent by the web socket client
     *
     * @param handler
     *    the web socket handler. Similar to AjaxRequestTarget but can 'push' directly in the
     *    web socket connection too
     * @param data
     *    the text sent by the client
     */
    override protected def onMessage(handler: WebSocketRequestHandler, data: TextMessage)
    {
      getSession.info("You typed: " + data.getMessage)
      handler.add(feedback)
      handler.push("A message pushed by the server via WebSocketRequestHandler!")
    }
  })

  add(new AjaxLink[Unit]("link") {
    def onClick(target: AjaxRequestTarget)
    {
      // shows how to push into existing WebSocket connection from normal Ajax request
      val sessionId = getSession.getId
      val pageId = getPage.getPageId
      val registry = new SimpleWebSocketConnectionRegistry()
      val connection = registry.getConnection(getApplication, sessionId, pageId)
      if (connection != null)
      {
        val webSocketHandler = new WebSocketRequestHandler(this, connection)
        webSocketHandler.push("A message pushed by clicking a normal AjaxLink")
      }
      getSession.info("AjaxLink clicked")
      target.add(feedback)
    }
  })

  override def renderHead(response: IHeaderResponse)
  {
    super.renderHead(response)

    response.render(JavaScriptHeaderItem.forReference(
      new PackageResourceReference(classOf[WebSocketDemo], "client.js")))
  }
}
