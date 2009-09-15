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

package multiplexer.jmx.util;

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
