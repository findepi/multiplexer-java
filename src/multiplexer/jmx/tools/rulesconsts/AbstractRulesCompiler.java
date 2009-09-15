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

package multiplexer.jmx.tools.rulesconsts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import multiplexer.protocol.Protocol.MultiplexerMessageDescription;
import multiplexer.protocol.Protocol.MultiplexerPeerDescription;
import multiplexer.protocol.Protocol.MultiplexerRules;

import com.google.protobuf.TextFormat;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @author Piotr Findeisen
 */
public abstract class AbstractRulesCompiler implements
	ConstantsFromRulesCompiler {

	protected Options options;

	public void compile(Options options) throws FileNotFoundException,
		IOException, InvalidRulesFileException {

		this.options = options;
		MultiplexerRules rules = parse();
		ConstantsGroup group = buildGroup(rules);
		validateNames(group);
		validateUniqness(group);
		validateValues(group);
		output(group);
	}

	private MultiplexerRules parse() throws FileNotFoundException, IOException {

		MultiplexerRules.Builder rulesBuilder = MultiplexerRules.newBuilder();
		TextFormat.merge(new FileReader(options.rulesFile), rulesBuilder);
		return rulesBuilder.build();
	}

	private ConstantsGroup buildGroup(MultiplexerRules rules) {

		List<Constant<Integer>> peerTypes = new ArrayList<Constant<Integer>>();
		List<Constant<Integer>> messageTypes = new ArrayList<Constant<Integer>>();

		for (MultiplexerPeerDescription mpd : rules.getPeerList())
			peerTypes.add(new Constant<Integer>(mpd.getName(), mpd.getType()));
		for (MultiplexerMessageDescription mmd : rules.getTypeList())
			messageTypes
				.add(new Constant<Integer>(mmd.getName(), mmd.getType()));

		return new ConstantsGroup(peerTypes, messageTypes);
	}

	private void validateNames(ConstantsGroup group)
		throws InvalidRulesFileException {
		validateNames(group.getPeerTypes());
		validateNames(group.getMessageTypes());
	}

	private void validateNames(List<Constant<Integer>> constants)
		throws InvalidRulesFileException {

		for (Constant<?> c : constants) {
			if (!c.name.toUpperCase().equals(c.name)) {
				throw new InvalidRulesFileException("Constant name '" + c.name
					+ "' is not in upper case.");
			}
		}
	}

	private void validateUniqness(ConstantsGroup group)
		throws InvalidRulesFileException {

		validateUniqness(group.getPeerTypes());
		validateUniqness(group.getMessageTypes());
	}

	private void validateUniqness(List<Constant<Integer>> constants)
		throws InvalidRulesFileException {
		Map<Integer, String> values = new HashMap<Integer, String>();
		for (Constant<Integer> c : constants) {
			String otherName = values.put(c.value, c.name);
			if (otherName != null) {
				throw new InvalidRulesFileException("Constants '" + otherName
					+ "' and '" + c.name + "' have the same value of "
					+ c.value);
			}
		}
	}

	protected abstract void validateValues(ConstantsGroup group)
		throws InvalidRulesFileException;

	private void output(ConstantsGroup group) throws IOException {
		prepareOutputConfiguration(options);

		if (!new File(options.outputRoot).isDirectory()) {
			throw new RuntimeException("File '" + options.outputRoot
				+ "' does not exist or is not a directory.");
		}

		new File(options.outputFile).getParentFile().mkdirs();

		Writer writer = new FileWriter(options.outputFile);
		Template codeTemplate = loadConstantsSourceTemplate();

		Map<String, Object> templateContext = new HashMap<String, Object>();
		// standard definitions
		templateContext.put("MessageTypes", MessageTypes.class.getName());
		templateContext.put("PeerTypes", PeerTypes.class.getName());
		templateContext.put("ConstantsPack", ConstantsPack.class.getName());
		// definitions unique for this generated file
		templateContext.put("packageName", options.packageName);
		templateContext.put("className", options.outputClassName);
		templateContext.put("peerTypes", group.getPeerTypes());
		templateContext.put("messageTypes", group.getMessageTypes());

		try {
			codeTemplate.process(templateContext, writer);
		} catch (TemplateException e) {
			throw new RuntimeException("Malformed template.", e);
		}
		writer.close();
	}

	private Template loadConstantsSourceTemplate() throws IOException {
		Configuration templateConfiguration = new Configuration();
		templateConfiguration.setObjectWrapper(new BeansWrapper());
		Template codeTemplate = new Template("Constants",
			new InputStreamReader(AbstractRulesCompiler.class
				.getResourceAsStream("Constants.template")),
			templateConfiguration);
		return codeTemplate;
	}

	private static void prepareOutputConfiguration(Options options) {
		if (options.outputRoot == null) {
			throw new RuntimeException("No output dir specified.");
		}

		if (options.outputRoot.endsWith(File.separator)) {
			options.outputRoot = options.outputRoot.substring(0,
				options.outputRoot.length() - 1);
		}

		if (options.outputFile == null) {
			if (options.outputClass != null) {
				options.outputFile = options.outputRoot + File.separator
					+ options.outputClass.replace(".", File.separator)
					+ ".java";
			} else {
				throw new RuntimeException("No output file chosen.");
			}
		}
		if (!options.outputFile.startsWith(options.outputRoot)) {
			throw new RuntimeException(
				"Output file is not located within output dir.");
		}

		if (options.outputClass == null) {
			if (options.outputFile != null) {
				if (options.outputFile.lastIndexOf('.') == -1) {
					throw new RuntimeException(
						"Output file must have an extension.");
				}
				options.outputClass = options.outputFile.substring(0,
					options.outputFile.lastIndexOf('.')).substring(
					options.outputRoot.length() + 1).replace("/", ".");
			} else {
				throw new RuntimeException("Output class not specified.");
			}
		}

		if (options.packageName == null) {
			if (options.outputClass != null) {
				if (options.outputClass.lastIndexOf('.') == -1) {
					options.packageName = "";
				} else {
					options.packageName = options.outputClass.substring(0,
						options.outputClass.lastIndexOf('.'));
				}

			} else {
				throw new RuntimeException("Output package name not specified.");
			}
		}

		options.outputClassName = options.outputClass
			.substring(options.outputClass.lastIndexOf('.') + 1);
	}
}
