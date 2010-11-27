package com.butterfly;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * 
 * distinguish protocol
 * 
 * @author feng
 * @since 2010/11/27
 */
public class ProxyDecoder extends FrameDecoder {
	private static Logger logger = Logger.getLogger(ProxyDecoder.class);

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
	        ChannelBuffer buffer) throws Exception {
		if (buffer.readableBytes() < 2) {
			return null;
		}

		buffer.markReaderIndex();
		final int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
		final int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);

		if (isHttp(magic1, magic2)) {
			logger.info("magic 2 bit: " + magic1 + ", " + magic2 + "; "
			        + channel.getRemoteAddress() + " client want http");
			ChannelPipeline pipeline = ctx.getPipeline();
			pipeline.addLast("handler", new ProxyHanlder("android", 8080,
			        channel));
			pipeline.remove(this);
		} else if (isWindowsRemote(magic1, magic2)) {
			logger.info("magic 2 bit: " + magic1 + ", " + magic2 + ";"
			        + channel.getRemoteAddress() + " want windows remote");
			ChannelPipeline pipeline = ctx.getPipeline();
			pipeline.addLast("handler", new ProxyHanlder("192.168.0.83", 3389,
			        channel));
			pipeline.remove(this);
		} else {
			logger.warn(channel.getRemoteAddress()
			        + " not understand protocal, close");
			buffer.skipBytes(buffer.readableBytes());
			ctx.getChannel().close();
		}
		buffer.resetReaderIndex();
		return buffer.readBytes(buffer.readableBytes());
	}

	private boolean isWindowsRemote(final int magic1, final int magic2) {
		return magic1 == 3 && magic2 == 0;
	}

	private boolean isHttp(int magic1, int magic2) {
		return magic1 == 'G' && magic2 == 'E' || // GET
		        magic1 == 'P' && magic2 == 'O' || // POST
		        magic1 == 'P' && magic2 == 'U' || // PUT
		        magic1 == 'H' && magic2 == 'E' || // HEAD
		        magic1 == 'O' && magic2 == 'P' || // OPTIONS
		        magic1 == 'P' && magic2 == 'A' || // PATCH
		        magic1 == 'D' && magic2 == 'E' || // DELETE
		        magic1 == 'T' && magic2 == 'R' || // TRACE
		        magic1 == 'C' && magic2 == 'O'; // CONNECT
	}

}
