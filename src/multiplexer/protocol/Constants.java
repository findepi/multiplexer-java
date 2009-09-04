package multiplexer.protocol;

import java.util.*;

public class Constants implements multiplexer.jmx.tools.rulesconsts.ConstantsPack {

	public static class PeerTypes implements multiplexer.jmx.tools.rulesconsts.PeerTypes {

		public final static int MULTIPLEXER = 1;
		public final static int ALL_TYPES = 2;
		public final static int MAX_MULTIPLEXER_SPECIAL_PEER_TYPE = 99;

		private static class ConstantsByNameMapHolder {
			public final static Map<String, Integer> map;
			static {
				Map<String, Integer> tmp = new HashMap<String, Integer>();
				tmp.put("MULTIPLEXER", MULTIPLEXER);
				tmp.put("ALL_TYPES", ALL_TYPES);
				tmp.put("MAX_MULTIPLEXER_SPECIAL_PEER_TYPE", MAX_MULTIPLEXER_SPECIAL_PEER_TYPE);
				map = Collections.unmodifiableMap(tmp);
			}
		}

		private static class ConstantsNamesMapHolder {
			public final static Map<Integer, String> map;
			static {
				Map<Integer, String> tmp = new HashMap<Integer, String>();
				tmp.put(MULTIPLEXER, "MULTIPLEXER");
				tmp.put(ALL_TYPES, "ALL_TYPES");
				tmp.put(MAX_MULTIPLEXER_SPECIAL_PEER_TYPE, "MAX_MULTIPLEXER_SPECIAL_PEER_TYPE");
				map = Collections.unmodifiableMap(tmp);
			}
		}

		/**
		 * @deprecated Use {@link #getConstantsByName} instead.
		 */
		public Map<String, Integer> getMap() {
			return getConstantsByName();
		};

		public Map<String, Integer> getConstantsByName() {
			return ConstantsByNameMapHolder.map;
		};

		public Map<Integer, String> getConstantsNames() {
			return ConstantsNamesMapHolder.map;
		};
	}

	public multiplexer.jmx.tools.rulesconsts.PeerTypes getPeerTypes() {
		return new PeerTypes();
	}

	public static class MessageTypes implements multiplexer.jmx.tools.rulesconsts.MessageTypes {

		public final static int PING = 1;
		public final static int CONNECTION_WELCOME = 2;
		public final static int BACKEND_FOR_PACKET_SEARCH = 3;
		public final static int HEARTBIT = 4;
		public final static int DELIVERY_ERROR = 5;
		public final static int MAX_MULTIPLEXER_META_PACKET = 99;
		public final static int REQUEST_RECEIVED = 113;
		public final static int BACKEND_ERROR = 114;

		private static class ConstantsByNameMapHolder {
			public final static Map<String, Integer> map;
			static {
				Map<String, Integer> tmp = new HashMap<String, Integer>();
				tmp.put("PING", PING);
				tmp.put("CONNECTION_WELCOME", CONNECTION_WELCOME);
				tmp.put("BACKEND_FOR_PACKET_SEARCH", BACKEND_FOR_PACKET_SEARCH);
				tmp.put("HEARTBIT", HEARTBIT);
				tmp.put("DELIVERY_ERROR", DELIVERY_ERROR);
				tmp.put("MAX_MULTIPLEXER_META_PACKET", MAX_MULTIPLEXER_META_PACKET);
				tmp.put("REQUEST_RECEIVED", REQUEST_RECEIVED);
				tmp.put("BACKEND_ERROR", BACKEND_ERROR);
				map = Collections.unmodifiableMap(tmp);
			}
		}

		private static class ConstantsNamesMapHolder {
			public final static Map<Integer, String> map;
			static {
				Map<Integer, String> tmp = new HashMap<Integer, String>();
				tmp.put(PING, "PING");
				tmp.put(CONNECTION_WELCOME, "CONNECTION_WELCOME");
				tmp.put(BACKEND_FOR_PACKET_SEARCH, "BACKEND_FOR_PACKET_SEARCH");
				tmp.put(HEARTBIT, "HEARTBIT");
				tmp.put(DELIVERY_ERROR, "DELIVERY_ERROR");
				tmp.put(MAX_MULTIPLEXER_META_PACKET, "MAX_MULTIPLEXER_META_PACKET");
				tmp.put(REQUEST_RECEIVED, "REQUEST_RECEIVED");
				tmp.put(BACKEND_ERROR, "BACKEND_ERROR");
				map = Collections.unmodifiableMap(tmp);
			}
		}

		/**
		 * @deprecated Use {@link #getConstantsByName} instead.
		 */
		public Map<String, Integer> getMap() {
			return getConstantsByName();
		};

		public Map<String, Integer> getConstantsByName() {
			return ConstantsByNameMapHolder.map;
		};

		public Map<Integer, String> getConstantsNames() {
			return ConstantsNamesMapHolder.map;
		};
	}

	public multiplexer.jmx.tools.rulesconsts.MessageTypes getMessageTypes() {
		return new MessageTypes();
	}
}
