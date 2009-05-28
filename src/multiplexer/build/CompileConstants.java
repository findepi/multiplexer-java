package multiplexer.build;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import multiplexer.Multiplexer;

import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;

public class CompileConstants {

	Multiplexer.MultiplexerRules rules;

	CompileConstants(String fileName) throws ParseException, IOException {
		FileReader fr = new FileReader(fileName);
		Multiplexer.MultiplexerRules.Builder rulesBuilder = Multiplexer.MultiplexerRules
			.newBuilder();
		TextFormat.merge(fr, rulesBuilder);
		rules = rulesBuilder.build();
	}

	private void writeTypes(String src, String path) throws IOException {
		OutputFileProperties ofp = parsePaths(src, path);
		System.out.println("generating " + ofp.path);
		FileWriter output = new FileWriter(ofp.path);
		output.write("package " + ofp.packageName + ";\n");
		output.write("public final class " + ofp.className + " {\n");
		for (Multiplexer.MultiplexerMessageDescription msg : rules.getTypeList()) {
			output.write("\tpublic static final int " + msg.getName() + " = " + msg.getType() + ";\n");
		}
		output.write("}\n");
		output.close();
	}

	private void writePeers(String src, String path) throws IOException {
		OutputFileProperties ofp = parsePaths(src, path);
		System.out.println("generating " + ofp.path);
		FileWriter output = new FileWriter(ofp.path);
		output.write("package " + ofp.packageName + ";\n");
		output.write("public final class " + ofp.className + " {\n");
		for (Multiplexer.MultiplexerPeerDescription peer : rules.getPeerList()) {
			output.write("\tpublic static final int " + peer.getName() + " = " + peer.getType() + ";\n");
		}
		output.write("}\n");
		output.close();
	}

	OutputFileProperties parsePaths(String src, String path) {
		OutputFileProperties ofp = new OutputFileProperties();
		ofp.path = src + File.separatorChar + path;
		ofp.className = path.substring(0, path.lastIndexOf('.')).substring(
			path.lastIndexOf('/') + 1);
		ofp.packageName = path.substring(0, path.lastIndexOf('/')).replace('/',
			'.');
		return ofp;
	}

	class OutputFileProperties {
		String className;
		String path;
		String packageName;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws ParseException, IOException {

		if (args.length != 4) {
			System.err.println("Usage: " + CompileConstants.class.getName()
				+ " <rules-file> <src-dir> <Peers.java> <Types.java>");
			System.exit(1);
		}

		CompileConstants cc = new CompileConstants(args[0]);
		cc.writePeers(args[1], args[2]);
		cc.writeTypes(args[1], args[3]);
	}

}
