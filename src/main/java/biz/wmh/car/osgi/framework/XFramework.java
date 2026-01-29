/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.framework;

import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.FrameworkWiring;

import biz.car.SYS;
import biz.car.osgi.bundle.MSG;

/**
 * Facade to the OSGi framework implementation.
 *
 * @version 2.0.0 20.01.2026 15:49:46
 */
public class XFramework {

	private static Framework fwk;

	/**
	 * @return a reference to the framework bundle context.
	 */
	public static BundleContext context() {
		return fwk.getBundleContext();
	}

	/**
	 * Creates an instance of the OSGi framework and initializes it.
	 *
	 * @param aConfig the configuration properties
	 * @throws BundleException on initialization error
	 */
	public static void init(Map<String, String> aConfig) {
		try {
			// Create an instance of the OSGi framework.
			fwk = XFrameworkFactory.get(aConfig);

			// Create a framework listener
			XFrameworkListener l_fl = new XFrameworkListener();

			// Initialize the OSGi framework using the framework listener
			// the listener only catches events during init phase
			fwk.init(l_fl);
			SYS.LOG.info(MSG.FWK_INITIALIZED, fwk.getSymbolicName(), fwk.getVersion());

			// Re-Activate framework for listening after init
			context().addFrameworkListener(l_fl);
			// Activate Service Listener
			XServiceListener l_sl = new XServiceListener();
			context().addServiceListener(l_sl);
			// Activate Bundle Listener
			XBundleListener l_bl = new XBundleListener();
			context().addBundleListener(l_bl);
		} catch (BundleException anEx) {
			SYS.LOG.exception(anEx);
			throw SYS.LOG.exception(MSG.FWK_INIT_ERROR);
		}
	}

	/**
	 * Resolve the package wiring of all bundles in the framework.
	 */
	public static void resolveBundles() {
		BundleContext l_ctx = context();
		Bundle l_sb = l_ctx.getBundle(Constants.SYSTEM_BUNDLE_ID);
		FrameworkWiring l_wire = l_sb.adapt(FrameworkWiring.class);

		l_wire.resolveBundles(null); // all framework bundles
	}

	/**
	 * Starts the OSGi framework.
	 */
	public static void start() {
		FrameworkEvent l_event = null;

		try {
			do {
				// Start the framework
				fwk.start();
				SYS.LOG.info(MSG.FWK_STARTED);

				// Wait for framework to stop to exit the VM
				l_event = fwk.waitForStop(0);
			}
			// If the framework was updated, then restart it
			while (l_event.getType() == FrameworkEvent.STOPPED_UPDATE);

			SYS.LOG.info(MSG.FWK_TERMINATED);
		} catch (InterruptedException anEx) {
		} catch (Exception anEx) {
			throw SYS.LOG.exception(anEx);
		}
	}

	/**
	 * Performs a shutdown of the OSGi framework.
	 */
	public static void stop() {
		if (fwk != null) {
			try {
				fwk.stop();
				fwk.waitForStop(0);
				SYS.LOG.info(MSG.FWK_STOPPED);

				fwk = null;
			} catch (InterruptedException ignored) {
			} catch (Exception anEx) {
				SYS.LOG.error(MSG.FWK_STOP_ERROR, anEx);
			}
		}
	}

	/**
	 * Creates a default <code>XFramework</code> instance.
	 */
	private XFramework() {
		super();
	}
}
