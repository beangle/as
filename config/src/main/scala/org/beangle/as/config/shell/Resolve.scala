package org.beangle.as.config.shell

import java.io.{ File, FileInputStream, FileOutputStream }
import java.net.URL

import org.beangle.commons.io.IOs
import org.beangle.commons.lang.Strings.{ isEmpty, isNotEmpty, split, substringAfterLast }
import org.beangle.as.config.model.Container
import org.beangle.maven.artifact.Artifact
import org.beangle.maven.artifact.ArtifactDownloader
import org.beangle.maven.artifact.Repo

object Resolve {

  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      println("Usage: Resolve /path/to/server.xml")
      return
    }
    val configFile = new File(args(0))
    val container = Container(scala.xml.XML.load(new FileInputStream(configFile)))
    val asHome = configFile.getParentFile.getParentFile.getCanonicalPath
    resolve(container, asHome)
  }

  def resolve(container: Container, home: String) {
    val loader = container.context.loader
    val remoteRepoUrl = if (null == loader) null else loader.properties.getOrElse("url", null)
    val localBase = if (null == loader) null else loader.properties.getOrElse("base", null)

    val remote = if (null == remoteRepoUrl) new Repo.Remote("remote", Repo.Remote.AliyunURL) else new Repo.Remote("remote", remoteRepoUrl)
    val local = new Repo.Local(localBase)

    container.webapps foreach { webapp =>
      if (isEmpty(webapp.docBase)) {
        if (isNotEmpty(webapp.url)) {
          val fileName = download(webapp.url, home + "/webapps/")
          webapp.docBase = "../../../webapps/" + fileName
        } else if (isNotEmpty(webapp.gav)) {
          val gavinfo = split(webapp.gav, ":")
          if (gavinfo.length < 3) throw new RuntimeException(s"Invalid gav ${webapp.gav},Using groupId:artifactId:version format.")
          val artifact = Artifact(webapp.gav + ":war")
          new ArtifactDownloader(remote, local).download(List(artifact))
          webapp.docBase = local.url(artifact)
        } else {
          throw new RuntimeException(s"Invalid Webapp definition ${webapp.name},one of (docBase,url,gav) properties needed.")
        }
      }
    }
  }
  private def download(url: String, dir: String): String = {
    val fileName = substringAfterLast(url, "/")
    val destFile = new File(dir + fileName)
    if (!destFile.exists) {
      val destOs = new FileOutputStream(destFile)
      val warurl = new URL(url)
      IOs.copy(warurl.openStream(), destOs)
    }
    fileName
  }
}
