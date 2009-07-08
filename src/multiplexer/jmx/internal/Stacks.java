package multiplexer.jmx.internal;

import java.io.PrintWriter;

import com.google.protobuf.ByteString;

/**
 * Helper methods for handling stacks.
 * 
 * @author Piotr Findeisen
 */
public class Stacks {
	private Stacks() {
	}

	public static ByteString stackTraceToByteString(Throwable e) {
		ByteString.Output output = ByteString.newOutput();
		PrintWriter writer = new PrintWriter(output);
		e.printStackTrace(writer);
		writer.close(); // force flush
		return output.toByteString();
	}
}
