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

import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;

/**
 * @author Piotr Findeisen
 */
@ChannelPipelineCoverage("all")
public class MessageCountingHandler implements ChannelUpstreamHandler,
	ChannelDownstreamHandler {

	private AtomicLong messagesOut = new AtomicLong();
	private AtomicLong messagesIn = new AtomicLong();

	public MessageCountingHandler() {
	}

	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
		throws Exception {

		updateMessageCounter(e, messagesIn);
		ctx.sendUpstream(e);
	}

	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
		throws Exception {

		updateMessageCounter(e, messagesOut);
		ctx.sendDownstream(e);
	}

	private void updateMessageCounter(ChannelEvent e, AtomicLong counter) {
		if (e instanceof MessageEvent) {
				counter.incrementAndGet();
		}
	}
	
	public long getMessagesOutCount() {
		return messagesOut.get();
	}
	
	public long getMessagesInCount() {
		return messagesIn.get();
	}

}
