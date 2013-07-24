package org.apache.wicket

import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.webapp.WebAppContext

object Start {
  def main(args: Array[String]) {
    val http_config: HttpConfiguration = new HttpConfiguration
    http_config.setSecureScheme("https")
    http_config.setSecurePort(8443)
    http_config.setOutputBufferSize(32768)
    http_config.setRequestHeaderSize(8192)
    http_config.setResponseHeaderSize(8192)
    http_config.setSendServerVersion(true)
    http_config.setSendDateHeader(false)
    val server: Server = new Server
    val connector: ServerConnector = new ServerConnector(server, new HttpConnectionFactory(http_config))
    connector.setSoLingerTime(-1)
    connector.setPort(8080)
    server.addConnector(connector)
    val keystore: Resource = Resource.newClassPathResource("/keystore")
    if (keystore != null && keystore.exists) {
      val factory: SslContextFactory = new SslContextFactory
      factory.setKeyStoreResource(keystore)
      factory.setKeyStorePassword("wicket")
      factory.setTrustStoreResource(keystore)
      factory.setKeyManagerPassword("wicket")
      val https_config: HttpConfiguration = new HttpConfiguration(http_config)
      https_config.addCustomizer(new SecureRequestCustomizer)
      val sslConnector: ServerConnector = new ServerConnector(server, new SslConnectionFactory(factory, "http/1.1"), new HttpConnectionFactory(https_config))
      sslConnector.setPort(8443)
      server.addConnector(sslConnector)
      System.out.println("SSL access to the quickstart has been enabled on port 8443")
      System.out.println("You can access the application using SSL on https://localhost:8443")
      System.out.println()
    }
    val bb: WebAppContext = new WebAppContext
    bb.setServer(server)
    bb.setContextPath("/")
    bb.setWar("src/main/webapp")
    server.setHandler(bb)
    try {
      System.out.println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP")
      server.start()
      System.in.read
      System.out.println(">>> STOPPING EMBEDDED JETTY SERVER")
      server.stop()
      server.join()
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        System.exit(1)
      }
    }
  }
}
