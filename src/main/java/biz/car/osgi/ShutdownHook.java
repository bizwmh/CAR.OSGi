/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi;

import biz.car.osgi.deploy.InstallArea;
import biz.car.osgi.framework.XFramework;

/**
 * The hook to make sure that the framework is cleanly shutdown when the VM
 * exits.
 *
 * @version 2.0.0 20.01.2026 15:33:53
 */
public class ShutdownHook implements Runnable {

	/**
	 * Creates a default <code>ShutdownHook</code> instance.
	 */
	public ShutdownHook() {
		super();
	}

	@Override
	public void run() {
		InstallArea.watcher().stop();
		XFramework.stop(); // stop the OSGi framework
	}
}
