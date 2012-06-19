package org.apache.wicket.websocket.jetty.example

import org.apache.wicket.protocol.http.WebApplication

/**
 *
 */
class WicketApplication extends WebApplication
{
  var eventSystem : EventSystem = _

  override def getHomePage = classOf[WebSocketDemo]

  override def init() {
    super.init

    eventSystem = new EventSystem(this);
  }

  override def onDestroy() {
    eventSystem.shutdown
    super.onDestroy
  }

  def getEventSystem = eventSystem
}

object WicketApplication
{
  def get = WebApplication.get().asInstanceOf[WicketApplication]
}


