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

package multiplexer.jmx.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestConstants implements multiplexer.jmx.tools.rulesconsts.ConstantsPack {

	public static class PeerTypes implements multiplexer.jmx.tools.rulesconsts.PeerTypes {
		
		public static final PeerTypes instance = new PeerTypes();

		public final static int WEBSITE = 102;
		public final static int TEST_SERVER = 106;
		public final static int TEST_CLIENT = 107;
		public final static int LOG_STREAMER = 108;
		public final static int LOG_COLLECTOR = 109;
		public final static int EVENTS_COLLECTOR = 110;
		public final static int LOG_RECEIVER_EXAMPLE = 111;
		public final static int ECHO_SERVER = 112;

		private static class ConstantsByNameMapHolder {
			public final static Map<String, Integer> map;
			static {
				Map<String, Integer> tmp = new HashMap<String, Integer>();
				tmp.put("WEBSITE", WEBSITE);
				tmp.put("TEST_SERVER", TEST_SERVER);
				tmp.put("TEST_CLIENT", TEST_CLIENT);
				tmp.put("LOG_STREAMER", LOG_STREAMER);
				tmp.put("LOG_COLLECTOR", LOG_COLLECTOR);
				tmp.put("EVENTS_COLLECTOR", EVENTS_COLLECTOR);
				tmp.put("LOG_RECEIVER_EXAMPLE", LOG_RECEIVER_EXAMPLE);
				tmp.put("ECHO_SERVER", ECHO_SERVER);
				map = Collections.unmodifiableMap(tmp);
			}
		}

		private static class ConstantsNamesMapHolder {
			public final static Map<Integer, String> map;
			static {
				Map<Integer, String> tmp = new HashMap<Integer, String>();
				tmp.put(WEBSITE, "WEBSITE");
				tmp.put(TEST_SERVER, "TEST_SERVER");
				tmp.put(TEST_CLIENT, "TEST_CLIENT");
				tmp.put(LOG_STREAMER, "LOG_STREAMER");
				tmp.put(LOG_COLLECTOR, "LOG_COLLECTOR");
				tmp.put(EVENTS_COLLECTOR, "EVENTS_COLLECTOR");
				tmp.put(LOG_RECEIVER_EXAMPLE, "LOG_RECEIVER_EXAMPLE");
				tmp.put(ECHO_SERVER, "ECHO_SERVER");
				map = Collections.unmodifiableMap(tmp);
			}
		}

		public Map<String, Integer> getConstantsByName() {
			return ConstantsByNameMapHolder.map;
		};

		public Map<Integer, String> getConstantsNames() {
			return ConstantsNamesMapHolder.map;
		};
	}

	public multiplexer.jmx.tools.rulesconsts.PeerTypes getPeerTypes() {
		return PeerTypes.instance;
	}

	public static class MessageTypes implements multiplexer.jmx.tools.rulesconsts.MessageTypes {
		
		public static final MessageTypes instance = new MessageTypes();

		public final static int TEST_REQUEST = 110;
		public final static int TEST_RESPONSE = 111;
		public final static int PICKLE_RESPONSE = 112;
		public final static int LOGS_STREAM = 115;
		public final static int LOGS_STREAM_RESPONSE = 116;
		public final static int SEARCH_COLLECTED_LOGS_REQUEST = 117;
		public final static int SEARCH_COLLECTED_LOGS_RESPONSE = 118;
		public final static int REPLAY_EVENTS_REQUEST = 126;

		private static class ConstantsByNameMapHolder {
			public final static Map<String, Integer> map;
			static {
				Map<String, Integer> tmp = new HashMap<String, Integer>();
				tmp.put("TEST_REQUEST", TEST_REQUEST);
				tmp.put("TEST_RESPONSE", TEST_RESPONSE);
				tmp.put("PICKLE_RESPONSE", PICKLE_RESPONSE);
				tmp.put("LOGS_STREAM", LOGS_STREAM);
				tmp.put("LOGS_STREAM_RESPONSE", LOGS_STREAM_RESPONSE);
				tmp.put("SEARCH_COLLECTED_LOGS_REQUEST", SEARCH_COLLECTED_LOGS_REQUEST);
				tmp.put("SEARCH_COLLECTED_LOGS_RESPONSE", SEARCH_COLLECTED_LOGS_RESPONSE);
				tmp.put("REPLAY_EVENTS_REQUEST", REPLAY_EVENTS_REQUEST);
				map = Collections.unmodifiableMap(tmp);
			}
		}

		private static class ConstantsNamesMapHolder {
			public final static Map<Integer, String> map;
			static {
				Map<Integer, String> tmp = new HashMap<Integer, String>();
				tmp.put(TEST_REQUEST, "TEST_REQUEST");
				tmp.put(TEST_RESPONSE, "TEST_RESPONSE");
				tmp.put(PICKLE_RESPONSE, "PICKLE_RESPONSE");
				tmp.put(LOGS_STREAM, "LOGS_STREAM");
				tmp.put(LOGS_STREAM_RESPONSE, "LOGS_STREAM_RESPONSE");
				tmp.put(SEARCH_COLLECTED_LOGS_REQUEST, "SEARCH_COLLECTED_LOGS_REQUEST");
				tmp.put(SEARCH_COLLECTED_LOGS_RESPONSE, "SEARCH_COLLECTED_LOGS_RESPONSE");
				tmp.put(REPLAY_EVENTS_REQUEST, "REPLAY_EVENTS_REQUEST");
				map = Collections.unmodifiableMap(tmp);
			}
		}

		public Map<String, Integer> getConstantsByName() {
			return ConstantsByNameMapHolder.map;
		};

		public Map<Integer, String> getConstantsNames() {
			return ConstantsNamesMapHolder.map;
		};
	}

	public multiplexer.jmx.tools.rulesconsts.MessageTypes getMessageTypes() {
		return MessageTypes.instance;
	}
}
