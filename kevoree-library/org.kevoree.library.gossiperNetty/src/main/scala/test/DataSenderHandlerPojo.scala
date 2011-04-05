package test

import com.google.protobuf.ByteString
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.UUID
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.kevoree.ContainerRoot
import org.kevoree.framework.KevoreeXmiHelper
import org.kevoree.library.gossip.Gossip
import org.kevoree.library.gossiperNetty.api.msg.KevoreeMessage.Message
import org.kevoree.library.version.Version.VectorClock
import org.slf4j.LoggerFactory
import org.kevoree.extra.marshalling.RichJSONObject

class DataSenderHandlerPojo(model : ContainerRoot) extends SimpleChannelUpstreamHandler {

	private var logger = LoggerFactory.getLogger(classOf[DataSenderHandlerPojo])

	override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
		//println("message received")
		//val message = e.getMessage.asInstanceOf[Message]
		println(e.getMessage)
		val message = Message.parseFrom(ByteString.copyFromUtf8(e.getMessage.asInstanceOf[String]))
		if (message.getContentClass.equals(classOf[Gossip.UUIDDataRequest].getName)) {
			val uuidDataRequest = Gossip.UUIDDataRequest.parseFrom(message.getContent)
			//val data = dataManager.getData(UUID.fromString(uuidDataRequest.getUuid))

			//val localObjJSON = new RichJSONObject(data._2)
			val localObjJSON = new RichJSONObject(stringFromModel)

			val res = localObjJSON.toJSON
			val modelBytes = ByteString.copyFromUtf8(res)
			//val modelBytes = ByteString.copyFrom(data._2.toString.getBytes("UTF8"))

			val vectorClock = VectorClock.newBuilder.setTimestamp(System.currentTimeMillis).build

			val modelBytes2 = Gossip.VersionedModel.newBuilder.setUuid(uuidDataRequest.getUuid).setVector(vectorClock).setModel(modelBytes).build.toByteString
			val responseBuilder: Message.Builder = Message.newBuilder.setDestName("titi").setDestNodeName("toto")
			responseBuilder.setContentClass(classOf[Gossip.VersionedModel].getName).setContent(modelBytes2)

			//e.getChannel.write(responseBuilder.build)
			e.getChannel.write(responseBuilder.build)
			//println("response sent")
		}
	}

	override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
		//NOOP
		logger.error(this.getClass + "\n" + e.getCause.getMessage + "\n" + e.getCause.getStackTraceString)
		e.getChannel.close.awaitUninterruptibly
	}

	private def stringFromModel(): Array[Byte] = {
		val out = new ByteArrayOutputStream
		KevoreeXmiHelper.saveStream(out, model)
		out.flush
		val bytes = out.toByteArray
		out.close
		bytes
	}
}
