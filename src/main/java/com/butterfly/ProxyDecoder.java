package com.butterfly;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import com.butterfly.Configration.ProxyItem;

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
		final int m1 = buffer.getUnsignedByte(buffer.readerIndex());
		final int m2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);

		Configration conf = Configration.getInstance();

		ProxyItem match = conf.match(m1, m2);
		if (match != null) {
			ChannelPipeline pipeline = ctx.getPipeline();
			pipeline.addLast("handler", new ProxyHanlder(match.host,
					match.port, channel));
			pipeline.remove(this);

			if (logger.isDebugEnabled()) {
				logger.debug("magic is " + m1 + " " + m2 + "; match " + match);
			}
		} else if (isConfig(m1, m2)) {
			ChannelPipeline pipeline = ctx.getPipeline();
			// Add the text line codec combination first,
			pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192,
					Delimiters.lineDelimiter()));
			pipeline.addLast("decoder", new StringDecoder());
			pipeline.addLast("encoder", new StringEncoder());
			pipeline.addLast("handler", new TelnetConfigHandler());
			pipeline.remove(this);

		} else {
			if (logger.isDebugEnabled()) {
				logger.debug(channel.getRemoteAddress()
						+ " not understand protocal, close");
			}
			buffer.skipBytes(buffer.readableBytes());
			ctx.getChannel().close();
		}

		buffer.resetReaderIndex();
		return buffer.readBytes(buffer.readableBytes());
	}

	private boolean isConfig(final int m1, final int m2) {
		return m1 == 'S' && m2 == 'F';
	}

}
