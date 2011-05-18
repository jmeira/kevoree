package org.kevoree.experiment.trace.gui.alg

import actors.{TIMEOUT, DaemonActor}
import org.jfree.chart.ChartPanel
import java.io.{InputStream, File}
import org.kevoree.experiment.trace.TraceMessages
import javax.swing.{JPanel, WindowConstants, JFrame}
import java.awt.BorderLayout

class TraceFileLookup(traceFile: File, frame: JFrame, nodeName: String,maxVal:Int) extends DaemonActor {

  var previousCheck: Long = 0l

  var previousPanel: JPanel = null

  def update() {
    val input: InputStream = this.getClass.getClassLoader.getResourceAsStream("./trace_out")
    val traces: TraceMessages.Traces = TraceMessages.Traces.parseFrom(input)
    val linkedTrace = TracePath.getPathFrom(nodeName, maxVal, traces)
    linkedTrace match {
      case Some(ltrace) => {
        println(ltrace.toString)

        val chart = new VectorClockSingleDisseminationChartScala(ltrace);
        val chartPanel = new ChartPanel(chart.buildChart());
        chartPanel.setOpaque(false);

        if (previousPanel != null) {
          frame.remove(previousPanel)
        }

        //frame.removeAll();

        val previousSize = frame.getSize

        frame.add(chartPanel, BorderLayout.CENTER);
        previousPanel = chartPanel;
        frame.pack();
        frame.setSize(previousSize);
        frame.setPreferredSize(previousSize)


      }
      case None => println("Not found")
    }
  }


  def act() {
    loop {
      reactWithin(4000) {
        case TIMEOUT => {
          if (traceFile.lastModified() > previousCheck) {
             update()
          }
        }
      }
    }
  }


}