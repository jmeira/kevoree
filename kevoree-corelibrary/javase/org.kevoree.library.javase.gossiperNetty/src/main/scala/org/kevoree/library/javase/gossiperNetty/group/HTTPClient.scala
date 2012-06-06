package org.kevoree.library.javase.gossiperNetty.group

import org.kevoree.ContainerRoot
import org.kevoree.framework.{KevoreeXmiHelper, Constants, KevoreePropertyHelper}
import java.io._
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import org.jboss.netty.channel.socket.nio.{NioClientSocketChannelFactory, NioServerSocketChannelFactory}
import org.jboss.netty.bootstrap.{ClientBootstrap, ServerBootstrap}
import org.jboss.netty.channel._
import group.DefaultChannelGroup
import java.net._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import java.nio.charset.Charset

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 14/04/12
 * Time: 17:22
 *
 * @author Erwan Daubert
 * @version 1.0
 */

class HTTPClient (groupName: String) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val handler = new HTTPClientHandler()

  var factoryClient = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
  var bootstrapClient = new ClientBootstrap(factoryClient)
  bootstrapClient.setPipelineFactory(new ChannelPipelineFactory() {
    override def getPipeline: ChannelPipeline = {
      val p: ChannelPipeline = Channels.pipeline()
      p.addLast("decoder", new HttpResponseDecoder)
      p.addLast("aggregator", new HttpChunkAggregator(1024048576))
      p.addLast("encoder", new HttpRequestEncoder)
      p.addLast("handler", handler)
      p
    }
  })

  bootstrapClient.setOption("tcpNoDelay", true)
  //  var channelServer: Channel = bootstrapServer.bind(new InetSocketAddress(port))

  val channelGroup = new DefaultChannelGroup("Gossiper-TCP-client")


  private class HTTPClientHandler extends SimpleChannelUpstreamHandler {
    private var push: Boolean = false
    private var zip: Boolean = false
    private var ip: String = ""
    private var port: Int = 0
    private var model: ContainerRoot = null
    private var targetNodeName: String = ""

    def setZip (zip: Boolean) {
      this.zip = zip
    }

    override def messageReceived (ctx: ChannelHandlerContext, e: MessageEvent) {
      logger.debug("model sent")
      e.getChannel.close()
    }

    override def exceptionCaught (ctx: ChannelHandlerContext, e: ExceptionEvent) {
      logger.debug("Unable to send model", e)
      if (zip) {
        zip = false
        logger.debug("url=>" + "http://" + ip + ":" + port + "/model/current")
        if (!sendModel(model, ip, port, "/model/current", zip = false)) {
          logger.debug("Unable to push a model on " + targetNodeName)
        }

      }
    }

    def push (ip: String, port: Int, model: ContainerRoot, targetNodeName: String) {
      this.ip = ip
      this.port = port
      this.model = model
      this.targetNodeName = targetNodeName
      /*zip = true
      logger.debug("url=>" + "http://" + ip + ":" + port + "/model/current/zip")
      if (!sendModel(model, ip, port, "/model/current/zip", zip)) {
          logger.debug("Unable to push a model on " + targetNodeName)
      }*/
      zip = false
      logger.debug("url=>" + "http://" + ip + ":" + port + "/model/current")
      if (!sendModel(model, ip, port, "/model/current", zip = false)) {
        logger.debug("Unable to push a model on " + targetNodeName)
      }
    }

    private def sendModel (model: ContainerRoot, hostName: String, port: Int, uri: String, zip: Boolean): Boolean = {
      val socketAddress = new InetSocketAddress(hostName, port)
      val socketChannel = bootstrapClient.connect(socketAddress)
      socketChannel.awaitUninterruptibly()
      if (socketChannel.isSuccess) {
        var channelBuffer: ChannelBuffer = null
        // Prepare the HTTP request.
        val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri)
        if (zip) {
          val outStream = new ByteArrayOutputStream
          KevoreeXmiHelper.saveCompressedStream(outStream, model)
          outStream.flush()
          channelBuffer = ChannelBuffers.buffer(outStream.size())
          channelBuffer.writeBytes(outStream.toByteArray)
          request.addHeader(HttpHeaders.Names.CONTENT_TYPE, "application/stream")
        } else {
          val modelString = KevoreeXmiHelper.saveToString(model, prettyPrint = false)
          channelBuffer = ChannelBuffers.copiedBuffer(modelString, Charset.forName("UTF-8"))
          request.addHeader(HttpHeaders.Names.CONTENT_TYPE, "plain/text; charset=UTF-8")
        }
        request.addHeader(HttpHeaders.Names.CONTENT_LENGTH, channelBuffer.readableBytes())
        request.setContent(channelBuffer)
        // Send the HTTP request.
        socketChannel.getChannel.write(request)
        channelGroup.add(socketChannel.getChannel)
        true
      } else {
        logger.debug("Unable to connect to {}:{}", Array[AnyRef](hostName, Integer.toString(port)), socketChannel.getCause)
        false
      }
    }
  }

  def push (model: ContainerRoot, targetNodeName: String) {
    val ipOption = KevoreePropertyHelper.getStringNetworkProperty(model, targetNodeName, Constants.KEVOREE_PLATFORM_REMOTE_NODE_IP)
    var ip = "127.0.0.1"
    if (ipOption.isDefined) {
      ip = ipOption.get
    }
    val portOption = KevoreePropertyHelper.getIntPropertyForGroup(model, groupName, "http_port", isFragment = true, nodeNameForFragment = targetNodeName)
    var port: Int = 8000
    if (portOption.isDefined) {
      port = portOption.get
    }

    handler.push(ip, port, model, targetNodeName)

  }

  def pull (model: ContainerRoot, targetNodeName: String): ContainerRoot = {
    var localhost: String = "localhost"
    var port: Int = 8000
    try {
      val addressOption = KevoreePropertyHelper.getStringNetworkProperty(model, targetNodeName, Constants.KEVOREE_PLATFORM_REMOTE_NODE_IP)
      if (addressOption.isDefined) {
        localhost = addressOption.get
      }
      val portOption = KevoreePropertyHelper.getIntPropertyForGroup(model, groupName, "port", isFragment = true, nodeNameForFragment = targetNodeName)
      if (portOption.isDefined) {
        port = portOption.get
      }
    }
    catch {
      case e: IOException => {
        logger.error("Unable to getAddress or Port of {}", targetNodeName, e)
      }
    }
    logger.debug("Pulling model for {} on {}", targetNodeName, "http://" + localhost + ":" + port + "/model/current")

    var root = pullModel("http://" + localhost + ":" + port + "/model/current/zip", zip = true)
    if (root == null) {
      root = pullModel("http://" + localhost + ":" + port + "/model/current", zip = false)
      if (root == null) {
        logger.warn("Unable to get model for {}", targetNodeName)
      }
    }
    root
  }

  private def pullModel (urlPath: String, zip: Boolean): ContainerRoot = {
    try {
      val url: URL = new URL(urlPath)
      val conn: URLConnection = url.openConnection
      conn.setConnectTimeout(2000)
      val inputStream: InputStream = conn.getInputStream
      if (zip) {
        KevoreeXmiHelper.loadCompressedStream(inputStream)
      }
      else {
        KevoreeXmiHelper.loadStream(inputStream)
      }
    } catch {
      case e: IOException => {
        logger.error("error while pulling model on {}", urlPath, e)
        null
      }
    }
  }

}
