package org.apache.wicket

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.webapp.WebAppContext

object Start extends App {

	val server = new Server()
	val connector = new SelectChannelConnector()

	// Set some timeout options to make debugging easier.
	connector.setMaxIdleTime(1000 * 60 * 60)
	connector.setSoLingerTime(-1)
	connector.setPort(8080)
	server.setConnectors(Array(connector))

	val bb = new WebAppContext()
	bb.setServer(server)
	bb.setContextPath("/")
	bb.setWar("src/main/webapp")

	// START JMX SERVER
	// MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	// MBeanContainer mBeanContainer = new MBeanContainer(mBeanServer);
	// server.getContainer().addEventListener(mBeanContainer);
	// mBeanContainer.start();

	server.setHandler(bb)

	try {
		System.out.println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP")
		server.start()
		System.in.read()
		System.out.println(">>> STOPPING EMBEDDED JETTY SERVER")
		// while (System.in.available() == 0) {
		//   Thread.sleep(5000);
		// }
		server.stop()
		server.join()
	} catch {
		case e: Exception => {
			e.printStackTrace()
			System.exit(100)
		}
	}
}
