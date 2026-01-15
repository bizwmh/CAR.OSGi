/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.car.osgi.framework;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;

import biz.car.osgi.bundle.BND;
import biz.car.osgi.bundle.MSG;
import biz.wmh.car.SYS;
import biz.wmh.car.util.XTimestamp;

/**
 * Facade to the OSGi framework implementation.
 *
 * @version 2.0.0 08.01.2026 11:00:50
 */
public class XFramework {

	private static Framework fwk;

	/**
	 * @return a reference to the framework bundle context.
	 */
	public static BundleContext getBundleContext() {
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
			// the listener only catches event during init
			fwk.init(l_fl);
			SYS.LOG.info(MSG.FWK_INITIALIZED, fwk.getSymbolicName(), fwk.getVersion());
			debug();

			// Activate Bundle Listener
			XBundleListener l_bl = new XBundleListener();
			getBundleContext().addBundleListener(l_bl);
			// Re-Activate framework for listening after init
			getBundleContext().addFrameworkListener(l_fl);
		} catch (BundleException anEx) {
			SYS.LOG.exception(anEx);
			throw SYS.LOG.exception(MSG.FWK_INIT_ERROR);
		}
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
				debug();

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
	 * Writes the framework properties and bundle information to the log file.
	 */
	private static void debug() {
		if (!SYS.LOG.isDebugEnabled())
			return;

		FrameworkDTO l_dto = fwk.adapt(FrameworkDTO.class);
		FrameworkStartLevelDTO l_sl = fwk.adapt(FrameworkStartLevelDTO.class);
		Map<String, Object> l_props = l_dto.properties;
		Bundle[] l_bundles = fwk.getBundleContext().getBundles();
		StringBuffer l_bProps = new StringBuffer();

		// list the framework properties
		l_props.entrySet().stream()
				.forEach(entry -> {
					l_bProps.append("\n\t\t\t\t" + entry.getKey()); //$NON-NLS-1$
					l_bProps.append(" = "); //$NON-NLS-1$
					l_bProps.append(entry.getValue());
				});
		l_bProps.append("\n\n\t\t\t\tStartlevel = " + l_sl.startLevel); //$NON-NLS-1$
		SYS.LOG.debug("Framework Properties {}", l_bProps); //$NON-NLS-1$

		// list the info about the installed bundles
		Arrays.asList(l_bundles).stream()
				.map(bundle -> {
					BundleDTO l_bd = bundle.adapt(BundleDTO.class);
					StringBuffer l_bBundle = new StringBuffer("Bundle " + l_bd.symbolicName); //$NON-NLS-1$
					BundleStartLevelDTO l_bsl = bundle.adapt(BundleStartLevelDTO.class);

					Date l_date = new Date(l_bd.lastModified);
					XTimestamp l_ts = new XTimestamp(l_date);
					l_bBundle.append("\n\t\t\t\tID         " + l_bd.id); //$NON-NLS-1$
					l_bBundle.append("\n\t\t\t\tState      " + BND.state(l_bd.state)); //$NON-NLS-1$
					l_bBundle.append("\n\t\t\t\tVersion    " + l_bd.version); //$NON-NLS-1$
					l_bBundle.append("\n\t\t\t\tupdated    " + l_ts.toString()); //$NON-NLS-1$
					l_bBundle.append("\n\t\t\t\tStartlevel " + l_bsl.startLevel); //$NON-NLS-1$
					l_bBundle.append("\n\t\t\t\tLocation   " + bundle.getLocation()); //$NON-NLS-1$
					l_bBundle.append("\n\t\t\t\tData Area  " + bundle.getDataFile("")); //$NON-NLS-1$ //$NON-NLS-2$

					return l_bBundle;
				})
				.forEach(info -> SYS.LOG.debug(info.toString()));
	}
}
