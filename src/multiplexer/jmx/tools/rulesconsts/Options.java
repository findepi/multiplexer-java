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

import java.util.List;

import org.kohsuke.args4j.Option;

/**
 * @author Piotr Findeisen
 */
public class Options {
	@Option(name = "-system", usage = "indicate to generate constants for internal use by library")
	public boolean system = false;

	@Option(name = "-input", usage = "input file with text-encoded MultiplexerRules protobuf")
	public String rulesFile;

	@Option(name = "-outdir", usage = "where to put generated files")
	public String outputRoot;

	@Option(name = "-output", usage = "path to generate file within output dir")
	public String outputFile;

	@Option(name = "-class", usage = "name of the generated class")
	public String outputClass;

	// inferred
	public String outputClassName;

	@Option(name = "-package", usage = "name of the package for generated class; use '' for default")
	public String packageName;

	@Option(name = "-check", usage = "validate constants uniqness with respect to other constants builds")
	public List<String> checkConstants;

	@Option(name = "-python", usage = "generate constants file for Python")
	public boolean python = false;
}
