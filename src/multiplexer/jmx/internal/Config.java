package multiplexer.jmx.internal;

/**
 * @author Kasia Findeisen
 */
public class Config {

	public static long INITIAL_READ_IDLE_TIME = 7;
	public static long INITIAL_WRITE_IDLE_TIME = 3;

	public long getReadIdleTime(int peerType) {
		// for peerType PeerTypes.MULTIPLEXER this should be hard-coded as the
		// clients don't read the config
		return 7;
	}

	public long getWriteIdleTime(int peerType) {
		// for peerType PeerTypes.MULTIPLEXER this should be hard-coded as the
		// clients don't read the config
		return 3;
	}

}
