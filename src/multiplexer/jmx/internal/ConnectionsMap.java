package multiplexer.jmx.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import multiplexer.jmx.exceptions.NoPeerForPeerIdException;
import multiplexer.jmx.exceptions.NoPeerForTypeException;
import multiplexer.protocol.Constants.PeerTypes;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.LinkedListMultimap;

/**
 * A class for managing groups of {@link Channel}s, indexed by connected peers'
 * types and Ids. Provides access to all {@code Channel}s as well as searching
 * by peer type (Peer types are described in {@link PeerTypes} and in any
 * additional generated files). Methods {@code getAny(peerType)} and {@code
 * getAll(peerType)} return one or all peers of the given type respectively.
 * 
 * For each peer (denoted by Id) only one channel can be held in the structure.
 * 
 * This object is thread-safe. TODO: check that ConnectionsMap is thread-safe
 * 
 * @author Kasia Findeisen
 * @author Piotr Findeisen
 */
// TODO napisać w README jak generować pliki z PeerTypes/MessageTypes
public class ConnectionsMap {

	/**
	 * A multimap of {@link Channel}s grouped by connected peers' types.
	 */
	private LinkedListMultimap<Integer, Channel> channelsByType = LinkedListMultimap
		.create();

	/**
	 * Provides access to all open channels while cleaning up.
	 */
	private ChannelGroup allChannels = new DefaultChannelGroup();

	/**
	 * A map of {@link Channel}s by peer Id. Id is associated with the peer's
	 * most recent connection.
	 */
	private BiMap<Long, Channel> channelsByPeerId = HashBiMap.create();

	/**
	 * Helper {@link Map}, reverse to {@code channelsByType}, which is a
	 * {@link LinkedListMultimap} and therefore has no reverse access.
	 */
	private Map<Channel, Integer> peerTypeByChannel = new WeakHashMap<Channel, Integer>();

	/**
	 * Adds a new channel to {@code allChannels} which is a {@link ChannelGroup}
	 * . Any closed channel will be removed automatically.
	 * 
	 * @param channel
	 *            a new connection
	 */
	public void addNew(Channel channel) {
		allChannels.add(channel);
	}

	/**
	 * Adds a new {@link Channel}, together with the connected peer's Id (
	 * {@code peerId}) and the connected peer's type ({@code peerType}) to
	 * global maps allowing indexing by peer types and Id. If a connection with
	 * the peer (a peer having the same Id as {@code peerId}) is already
	 * established, it is overwritten in the maps. The {@link Channel} of the
	 * previous connection is returned so that it can be closed by the callee.
	 * 
	 * @param channel
	 *            a new connection
	 * @param peerId
	 *            Id of the connected peer
	 * @param peerType
	 *            type of the connected peer
	 * @return a channel of a previous connection to the peer or null if the
	 *         peer wasn't connected
	 */
	public synchronized Channel add(Channel channel, long peerId, int peerType) {
		Channel oldChannel = channelsByPeerId.put(peerId, channel);
		if (oldChannel != null) {
			channelsByType
				.remove(peerTypeByChannel.get(oldChannel), oldChannel);
			peerTypeByChannel.remove(oldChannel);
		}
		peerTypeByChannel.put(channel, peerType);
		channelsByType.put(peerType, channel);
		return oldChannel;
	}

	/**
	 * Removes the {@link Channel} previously added with {@link #addNew} or
	 * {#link #add}. Returns true if the {@code channel} has been removed from
	 * any of internal structures.
	 * 
	 * @param channel
	 *            channel to be removed
	 * @return true, if the channel has been removed
	 */
	public synchronized boolean remove(Channel channel) {
		boolean removed = false;
		if (allChannels.remove(channel)) {
			// The channel was registered with `addNew`.
			removed = true;
		}

		Integer type = peerTypeByChannel.get(channel);
		if (type != null) {
			// The channel was registered with `add`.
			channelsByType.remove(type, channel);
			channelsByPeerId.inverse().remove(channel);
			peerTypeByChannel.remove(channel);
			removed = true;
		}
		return removed;
	}

	/**
	 * Returns a {@link Channel} associated with some peer of the given type (
	 * {@code peerType}). Chooses the channel on a basis of round-robin
	 * algorithm.
	 * 
	 * @param peerType
	 *            requested type of the peer
	 * @throws NoPeerForTypeException
	 *             when there are no Channels for given type
	 */
	public synchronized Channel getAny(int peerType)
		throws NoPeerForTypeException {

		List<Channel> list = channelsByType.get(peerType);
		if (list == null || list.size() == 0)
			throw new NoPeerForTypeException();

		Channel anyChannel;
		while (list.size() > 0) {
			anyChannel = list.remove(0);
			if (anyChannel.isOpen()) {
				list.add(anyChannel);
				return anyChannel;
			}
		}
		throw new NoPeerForTypeException();
	}

	/**
	 * Returns an {@link Iterator} of all {@link Channel}s associated with the
	 * given peer type ({@code peerType}). You should manually synchronize on
	 * this {@link ConnectionsMap} when calling this method and iterating over
	 * the returned value.
	 * 
	 * @param peerType
	 *            requested type of the peer
	 * @return iterator over connections of give type.
	 * @throws NoPeerForTypeException
	 */
	public Iterator<Channel> getAll(int peerType) throws NoPeerForTypeException {
		List<Channel> list = channelsByType.get(peerType);
		if (list == null || list.size() == 0)
			throw new NoPeerForTypeException();
		return list.iterator();
	}

	/**
	 * TODO(findepi) javadoc
	 * 
	 * @throws NoPeerForPeerIdException 
	 */
	public Channel getByPeerId(long peerId) throws NoPeerForPeerIdException {
		Channel channel;
		synchronized (this) {
			channel = channelsByPeerId.get(peerId);
		}
		if (channel == null)
			throw new NoPeerForPeerIdException();

		return channel;
	}

	/**
	 * Get all {@link Channel}s that have been added with {@link #addNew} and
	 * has not yet been closed. You should not modify the returned set.
	 * 
	 * @return all channels
	 */
	public ChannelGroup getAllChannels() {
		return allChannels;
	}
}
