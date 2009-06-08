/**
 * 
 */
package multiplexer.jmx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.netty.channel.Channel;

import com.google.common.collect.LinkedListMultimap;

/**
 * @author Kasia Findeisen
 *
 */
public class ConnectionsMap {
	
	private static final int UNGROUPPED_CHANNELS = 0;
	
	private LinkedListMultimap<Integer, Channel> channelsByType = LinkedListMultimap.create();
	private Map<Long, Channel> channelsByPeerId = new HashMap<Long, Channel>();
	
	public void addNew(Channel channel) {
		synchronized (channelsByType) {
			channelsByType.put(UNGROUPPED_CHANNELS, channel);
		}
	}
	
	public void add(Channel channel, long peerId, int peerType) {
		assert channelsByType.containsEntry(UNGROUPPED_CHANNELS, channel);
		channelsByType.remove(UNGROUPPED_CHANNELS, channel);
		channelsByType.put(peerType, channel);
		channelsByPeerId.put(peerId, channel);
	}
	
	public Channel getAny(int peerType) {
		List<Channel> list = channelsByType.get(peerType);
		Channel anyChannel = list.remove(0);
		list.add(anyChannel);
		return anyChannel;
	}
	
	public Iterator<Channel> getAll(int peerType) {
		List<Channel> list = channelsByType.get(peerType);
		return list.iterator();
	}
	
	

}
