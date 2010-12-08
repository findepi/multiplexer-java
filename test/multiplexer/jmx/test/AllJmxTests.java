package multiplexer.jmx.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestByteBufferSerialization.class,
	TestConnectivity.class, TestGCing.class,
	TestMultiplexerMessageWithServer.class, TestMultiplexerPassword.class,
	TestMultiplexerProtocolHandlerWithServer.class, TestQuery.class,
	TestThreadsShutdown.class })
public class AllJmxTests {
}
