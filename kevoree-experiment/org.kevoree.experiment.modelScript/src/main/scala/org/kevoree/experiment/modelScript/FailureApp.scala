package org.kevoree.experiment.modelScript

import java.lang.Character
import util.control.Breaks
import java.io.{InputStreamReader, BufferedReader, BufferedInputStream, InputStream}

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 28/05/11
 * Time: 15:00
 */
object FailureApp extends Application {

  val failureGenerator = new FailureGenerator(Configuration.ips)

  val stream = new BufferedReader(new InputStreamReader((System.in)))
  var line = ""
  while ((line = stream.readLine()) != -1 && !line.equals("q")) {
    failureGenerator.doAction(line)
  }
}