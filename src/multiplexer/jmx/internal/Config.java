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
