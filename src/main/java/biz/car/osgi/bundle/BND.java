/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.car.osgi.bundle;

import com.typesafe.config.Config;

import biz.car.config.ACS;

/**
 * Bundle Constants.
 *
 * @version 2.0.0 13.10.2025 15:24:37
 */
public class BND {

	public static String FRAMEWORK;
	public static String OSGI_MAIN;
	public static String SHUTDOWN_HOOK;

	private static final Config conf;

	// -------------------------------------------------------------------------
	// Initialize the static fields
	// -------------------------------------------------------------------------
	static {
		conf = ACS.initialize(BND.class, "BND.conf"); //$NON-NLS-1$
	}

	/**
	 * Converts a type integer to a string.
	 * 
	 * @param aType the integer value for the type
	 * @return the type as a string
	 */
	public static String bundleEvent(int aType) {
		String l_ret = Integer.toString(aType);
		String l_state = "BUNDLE_EVENT" + l_ret; //$NON-NLS-1$

		if (conf.hasPath(l_state)) {
			l_ret = conf.getString(l_state);
		}
		return l_ret;
	}

	/**
	 * Converts a state integer to a string.
	 * 
	 * @param aState the integer value for the state
	 * @return the state as a string
	 */
	public static String state(int aState) {
		String l_ret = Integer.toString(aState);
		String l_state = "STATE" + l_ret; //$NON-NLS-1$

		if (conf.hasPath(l_state)) {
			l_ret = conf.getString(l_state);
		}
		return l_ret;
	}
}
