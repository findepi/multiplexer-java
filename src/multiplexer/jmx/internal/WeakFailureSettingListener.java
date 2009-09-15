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

import java.lang.ref.WeakReference;
import java.nio.channels.ClosedChannelException;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * @author Piotr Findeisen
 */
public class WeakFailureSettingListener implements ChannelFutureListener {

	private final WeakReference<ChannelFuture> target;

	public WeakFailureSettingListener(ChannelFuture target) {
		this.target = new WeakReference<ChannelFuture>(target);
	}

	public void operationComplete(ChannelFuture future) throws Exception {
		ChannelFuture target = this.target.get();
		if (target != null) {
			target.setFailure(new ClosedChannelException());
		}
	}

}
