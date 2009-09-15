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

import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Piotr Findeisen
 */
@ChannelPipelineCoverage("all")
public class ByteCountingHandler implements ChannelUpstreamHandler,
	ChannelDownstreamHandler {

	private static final Logger logger = LoggerFactory
		.getLogger(ByteCountingHandler.class);

	private AtomicLong bytesOut = new AtomicLong();
	private AtomicLong bytesIn = new AtomicLong();

	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
		throws Exception {

		updateByteCounter(e, bytesIn, "upstream");
		ctx.sendUpstream(e);
	}

	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
		throws Exception {

		updateByteCounter(e, bytesOut, "downstream");
		ctx.sendDownstream(e);
	}

	private void updateByteCounter(ChannelEvent e, AtomicLong counter,
		String direction) {
		if (e instanceof MessageEvent) {
			Object message = ((MessageEvent) e).getMessage();
			if (!(message instanceof ChannelBuffer)) {
				logger.warn("ByteCountingHandler got {} message of type {}",
					direction, message.getClass().getName());
			} else {
				counter.addAndGet(((ChannelBuffer) message).readableBytes());
			}
		}
	}

	public long getBytesOutCount() {
		return bytesOut.get();
	}
	
	public long getBytesInCount() {
		return bytesIn.get();
	}
}
