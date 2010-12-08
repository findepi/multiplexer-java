// Copyright 2009 Warsaw University, Faculty of Physics
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package multiplexer.jmx.internal;

import java.nio.ByteOrder;

import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Piotr Findeisen
 */
@Sharable
public class ChannelBufferFactorySettingHandler implements
	ChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory
		.getLogger(ChannelBufferFactorySettingHandler.class);

	/**
	 * Internal.
	 */
	public static final ChannelBufferFactorySettingHandler LITTLE_ENDIAN_BUFFER_FACTORY_SETTER = new ChannelBufferFactorySettingHandler(
		HeapChannelBufferFactory.getInstance(ByteOrder.LITTLE_ENDIAN));

	private final ChannelBufferFactory factory;

	public ChannelBufferFactorySettingHandler(ChannelBufferFactory factory) {
		this.factory = factory;
	}

	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
		throws Exception {
		logger.debug("CBFSH welcomes {}", ctx.getChannel());
		e.getChannel().getConfig().setBufferFactory(factory);
		ctx.getPipeline().remove(this);
		ctx.sendUpstream(e);
	}
}
