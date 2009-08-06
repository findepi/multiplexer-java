package multiplexer.jmx.server;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;

public class Options {
	@Option(name = "-host", usage = "local bind address (default 0.0.0.0)")
	public String localHost = "0.0.0.0";

	@Option(name = "-port", usage = "local bind port (default 1980)")
	public int localPort = 1980;

	@Option(name = "-rules", usage = "rules file (may be repeated)")
	public List<String> rulesFiles = new ArrayList<String>();
	
	@Option(name="-print", usage="how often print transfer statistics (in millis; default 10000)")
	public long transferUpdateIntervalMillis = 10000;
}