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

package multiplexer.jmx.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;

import multiplexer.jmx.internal.MessageReceivedListener;
import multiplexer.protocol.Protocol.MultiplexerMessage;

/**
 * @author Piotr Findeisen
 */
public class ClientMessageReceivedListener implements MessageReceivedListener {
	
	final private ConcurrentMap<Long, BlockingQueue<IncomingMessageData>> queryResponses;
	final private BlockingQueue<IncomingMessageData> messageQueue;
	
	public ClientMessageReceivedListener(
		ConcurrentMap<Long, BlockingQueue<IncomingMessageData>> queryResponses,
		BlockingQueue<IncomingMessageData> messageQueue) {
		super();
		this.queryResponses = queryResponses;
		this.messageQueue = messageQueue;
	}

	public void onMessageReceived(MultiplexerMessage message,
		Connection connection) {
		long id = message.getReferences();
		IncomingMessageData msg = new IncomingMessageData(message,
			connection);
		BlockingQueue<IncomingMessageData> queryQueue = queryResponses
			.get(id);
		if (queryQueue == null) {
			messageQueue.add(msg);
		} else {
			queryQueue.add(msg);
		}
	}
}
