/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.butterfly;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.butterfly.Configration.MagicBit;
import com.butterfly.Configration.ProxyItem;

/**
 * 
 * http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/telnet/package-
 * summary.html
 */
public class TelnetConfigHandler extends SimpleChannelUpstreamHandler {
	private boolean authendicate = false;
	private static final String AUTH = "SF, hello world!";
	private static final String WELCOME = "welcome! hello world\n>>";
	private static final String BAD = "opps, wrong!\n>>";
	private static final String HELP = "commands: \n"
			+ "1. dump                                          ----dump configration;\n"
			+ "2. add host:port magic1 magic2; magic1 magic2;   ----add a proxy\n"
			+ "       shorthand add host:port http              ----http   \n"
			+ "       shorthand add host:port remote            ----http   \n"
			+ "3. del host:port                                 ----remove a config\n"
			+ "4. close                                         ----close this connection\n"
			+ "5. help                                          ----print this help message \n>>";
	private static final String UNKNOWNCOMMAND = "unkown command; try again\n>>";
	private static final String CLOSEING = "closing, byte\n";
	private static final String PROPMT = ">>";

	private static final Logger logger = Logger
			.getLogger(TelnetConfigHandler.class);

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
			throws Exception {
		if (e instanceof ChannelStateEvent) {
			logger.info(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

		try {
			// Cast to a String first.
			// We know it is a String because we put some codec in
			// TelnetPipelineFactory.
			String request = ((String) e.getMessage()).trim();

			if (!authendicate) {
				if (AUTH.equals(request)) {
					authendicate = true;
					e.getChannel().write(WELCOME);
				} else {
					ChannelFuture future = e.getChannel().write(BAD);
					future.addListener(ChannelFutureListener.CLOSE);
				}
			} else {
				TelnetCommandDecoder decoder = new TelnetCommandDecoder(request);

				Configration conf = Configration.getInstance();

				switch (decoder.getCommand()) {
				case DUMP:
					String mesge = conf.dump();
					e.getChannel().write(mesge + PROPMT);
					break;
				case DEL:
					ProxyItem item = decoder.getRemoveProxy();
					conf.removeProxyItem(item.host, item.port);
					e.getChannel().write(conf.dump() + PROPMT);
					break;
				case ADD:
					ProxyItem proxyitem = decoder.getAddProxy();
					conf.addProxyItem(proxyitem);
					e.getChannel().write(conf.dump() + PROPMT);
					break;
				case HELP:
					e.getChannel().write(HELP);
					break;
				case CLOSE:
					e.getChannel().write(CLOSEING);
					e.getChannel().close();
					break;
				case EMPTY:
					e.getChannel().write(PROPMT);
					break;
				default:
					e.getChannel().write(UNKNOWNCOMMAND);
					break;
				}
			}
		} catch (Exception ex) {
			e.getChannel().write("Error: " + ex.getMessage() + "\n" + PROPMT);
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.warn("Unexpected exception from downstream.", e.getCause());
		e.getChannel().close();
	}
}

class TelnetCommandDecoder {
	public static enum Command {
		DUMP, ADD, DEL, HELP, UNKNOWN, CLOSE, EMPTY
	}

	private String mCommand;

	public TelnetCommandDecoder(String command) {
		mCommand = command.replaceAll("\\s+", " ").trim();

	}

	public ProxyItem getRemoveProxy() {
		String[] split = mCommand.split(":");
		return new ProxyItem(split[0].trim(), Integer.parseInt(split[1]), null);
	}

	public ProxyItem getAddProxy() {
		int index = mCommand.indexOf(' ');
		String c = mCommand.substring(0, index);
		mCommand = mCommand.substring(index + 1);
		String[] split = c.split(":");

		List<MagicBit> magicBits = new ArrayList<MagicBit>();
		if (mCommand.indexOf("http") != -1) {
			magicBits = Configration.HTTPMAGIC;
		} else if (mCommand.indexOf("remote") != -1) {
			magicBits = Configration.REMOTEMAGIC;
		} else {
			String[] magics = mCommand.split(";");
			for (String m : magics) {
				m = m.trim();
				if (m.length() > 2) {
					magicBits.add(MagicBit.valueOf(m));
				}
			}
		}
		return new ProxyItem(split[0], Integer.valueOf(split[1]), magicBits);

	}

	public Command getCommand() {
		int index = mCommand.indexOf(' ');
		String c = null;
		if (index != -1) {
			c = mCommand.substring(0, index).toUpperCase();
			mCommand = mCommand.substring(index + 1).trim();
		} else {
			c = mCommand.toUpperCase();
			if (c.length() == 0) {
				c = "EMPTY";
			}
		}
		try {
			Command command = Command.valueOf(c);
			return command;
		} catch (Exception e) {
		}
		return Command.UNKNOWN;
	}
}
