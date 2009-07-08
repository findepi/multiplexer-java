package multiplexer.jmx.internal;

public class Config {

	public static long INITIAL_READ_IDLE_TIME = 7;
	public static long INITIAL_WRITE_IDLE_TIME = 3;

	public long getReadIdleTime(int peerType) {
		return 7;
	}

	public long getWriteIdleTime(int peerType) {
		return 3;
	}

}
