/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2014, Beangle Software.
 *
 * Beangle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Beangle is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Beangle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.as.config.model

import org.beangle.commons.lang.Numbers.toInt
import org.beangle.commons.lang.Strings

object Container {
  def apply(xml: scala.xml.Elem): Container = {
    val conf = new Container

    (xml \ "Repository") foreach { repoElem =>
      val local = (repoElem \ "@local").text
      val remote = (repoElem \ "@remote").text
      conf.repository = new Repository(if (Strings.isEmpty(local)) null else local, if (Strings.isEmpty(remote)) null else remote)
    }

    (xml \ "Engines" \ "Engine") foreach { engineElem =>
      val name = (engineElem \ "@name").text
      val version = (engineElem \ "@version").text
      val typ = (engineElem \ "@type").text
      val engine = new Engine(name, typ, version)

      (engineElem \ "Listener").foreach { lsnElem =>
        val listener = new Listener((lsnElem \ "@className").text)
        for ((k, v) <- (lsnElem.attributes.asAttrMap -- Set("className"))) {
          listener.properties.put(k, v)
        }
        engine.listeners += listener
      }

      (engineElem \ "Context").foreach { ctxElem =>
        val context = new Context
        (ctxElem \ "Loader").foreach { ldElem =>
          val loader = new Loader((ldElem \ "@className").text)
          for ((k, v) <- (ldElem.attributes.asAttrMap -- Set("className"))) {
            loader.properties.put(k, v)
          }
          context.loader = loader
        }
        (ctxElem \ "JarScanner").foreach { scanElem =>
          val jarScanner = new JarScanner()
          for ((k, v) <- (scanElem.attributes.asAttrMap -- Set("className"))) {
            jarScanner.properties.put(k, v)
          }
          context.jarScanner = jarScanner
        }
        engine.context = context
      }
      conf.engines += engine
    }

    (xml \ "Hosts" \ "Host") foreach { hostElem =>
      val name = (hostElem \ "@name").text
      val ip = (hostElem \ "@ip").text
      val comment = (hostElem \ "@comment").text
      val host = new Host(name, ip)
      if (Strings.isNotBlank(comment)) host.comment = Some(comment)
      conf.hosts += host
    }

    (xml \ "Farms" \ "Farm").foreach { farmElem =>
      val engine = conf.engine((farmElem \ "@engine").text)
      if (engine.isEmpty) throw new RuntimeException("Cannot find engine for" + (farmElem \ "@engine").text)

      val farm = new Farm((farmElem \ "@name").text, engine.get)
      val jvmopts = (farmElem \ "JvmArgs" \ "@opts").text
      farm.jvmopts = if (Strings.isEmpty(jvmopts)) None else Some(jvmopts)

      (farmElem \ "HttpConnector") foreach { httpElem =>
        val http = new HttpConnector
        readConnector(httpElem, http)
        if (!(httpElem \ "@disableUploadTimeout").isEmpty) http.disableUploadTimeout = (httpElem \ "@disableUploadTimeout").text == "true"
        if (!(httpElem \ "@connectionTimeout").isEmpty) http.connectionTimeout = toInt((httpElem \ "@connectionTimeout").text)
        if (!(httpElem \ "@compression").isEmpty) http.compression = (httpElem \ "@compression").text
        if (!(httpElem \ "@compressionMinSize").isEmpty) http.compressionMinSize = toInt((httpElem \ "@compressionMinSize").text)
        if (!(httpElem \ "@compressionMimeType").isEmpty) http.compressionMimeType = (httpElem \ "@compressionMimeType").text
        farm.http = http
      }

      (farmElem \ "Server") foreach { serverElem =>
        val server = new Server(farm, (serverElem \ "@name").text)
        server.http = toInt((serverElem \ "@http").text)
        val host = (serverElem \ "@host").text
        if (Strings.isEmpty(host)) server.host = host
        farm.servers += server
      }
      conf.farms += farm
    }

    (xml \ "Resources" \ "Resource") foreach { resourceElem =>
      val ds = new Resource((resourceElem \ "@name").text)
      for ((k, v) <- (resourceElem.attributes.asAttrMap -- Set("name"))) {
        ds.properties.put(k, v)
      }
      conf.resources.put(ds.name, ds)
    }

    (xml \ "Webapps" \ "Webapp").foreach { webappElem =>
      val context = new Webapp((webappElem \ "@name").text)
      if (!(webappElem \ "@reloadable").isEmpty) context.reloadable = (webappElem \ "@reloadable").text == "true"
      if (!(webappElem \ "@docBase").isEmpty) context.docBase = (webappElem \ "@docBase").text
      if (!(webappElem \ "@url").isEmpty) context.url = (webappElem \ "@url").text
      if (!(webappElem \ "@gav").isEmpty) context.gav = (webappElem \ "@gav").text

      for ((k, v) <- (webappElem.attributes.asAttrMap -- Set("name", "docBase", "reloadable", "url", "gav"))) {
        context.properties.put(k, v)
      }

      (webappElem \ "ResourceRef").foreach { dsElem =>
        context.resources += conf.resources((dsElem \ "@ref").text)
      }
      (webappElem \ "Realm").foreach { realmElem =>
        context.realms = realmElem.toString()
      }
      conf.webapps += context
    }

    (xml \ "Deployments" \ "Deployment") foreach { deployElem =>
      conf.deployments += new Deployment((deployElem \ "@webapp").text, (deployElem \ "@on").text, (deployElem \ "@path").text)
    }
    conf
  }

  private def readConnector(xml: scala.xml.Node, connector: Connector) {
    if (!(xml \ "@protocol").isEmpty) connector.protocol = (xml \ "@protocol").text
    if (!(xml \ "@URIEncoding").isEmpty) connector.URIEncoding = (xml \ "@URIEncoding").text
    if (!(xml \ "@enableLookups").isEmpty) connector.enableLookups = (xml \ "@enableLookups").text == "true"
    if (!(xml \ "@acceptCount").isEmpty) connector.acceptCount = toInt((xml \ "@acceptCount").text)
    if (!(xml \ "@maxThreads").isEmpty) connector.maxThreads = toInt((xml \ "@maxThreads").text)
    if (!(xml \ "@maxConnections").isEmpty) connector.maxConnections = Some(toInt((xml \ "@maxConnections").text))
    if (!(xml \ "@minSpareThreads").isEmpty) connector.minSpareThreads = toInt((xml \ "@minSpareThreads").text)
  }

}
class Container {

  var repository: Repository = _

  val engines = new collection.mutable.ListBuffer[Engine]

  val hosts = new collection.mutable.ListBuffer[Host]

  val farms = new collection.mutable.ListBuffer[Farm]

  val webapps = new collection.mutable.ListBuffer[Webapp]

  val resources = new collection.mutable.HashMap[String, Resource]

  val deployments = new collection.mutable.ListBuffer[Deployment]

  def webappNames: Set[String] = {
    webapps.map(c => c.name).toSet
  }

  def resourceNames: Set[String] = {
    resources.keySet.toSet
  }

  def engine(name: String): Option[Engine] = {
    engines.find(e => e.name == name)
  }

  def farmResourceNames(farm: Farm): Set[String] = {
    val names = new collection.mutable.HashSet[String]
    deployments foreach { d =>
      if (d.on == farm.name || d.on.startsWith(farm.name + ".")) {
        webapps find (w => w.name == d.webapp) foreach { w => names ++= w.resourceNames }
      }
    }
    names.toSet
  }

  def farmNames: Set[String] = farms.map(f => f.name).toSet

  def serverNames: Seq[String] = farms.map(f => f.servers).flatten.map(s => s.farm.name + "." + s.name)

  def ports: List[Int] = {
    val ports = new collection.mutable.HashSet[Int]
    for (farm <- farms; server <- farm.servers) {
      if (server.http > 0) ports += server.http
    }
    ports.toList.sorted
  }

}
