/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.bundle;

import biz.car.config.ACS;

/**
 * Bundle messages.
 *
 * @version 2.0.0 13.10.2025 15:25:05
 */
public class MSG {

	public static String BUNDLE_EVENT;
	public static String FRAMEWORK_EVENT;
	public static String FWK_ABENDED;
	public static String FWK_INIT_ERROR;
	public static String FWK_INITIALIZED;
	public static String FWK_STARTED;
	public static String FWK_STOP_ERROR;
	public static String FWK_STOPPED;
	public static String FWK_TERMINATED;
	public static String PROPERTIES_LOADED;

	// -------------------------------------------------------------------------
	// Initialize the static fields
	// -------------------------------------------------------------------------
	static {
		ACS.initialize(MSG.class, "MSG.properties"); //$NON-NLS-1$
	}

	/**
	 * Creates a default <code>VAR</code> instance.
	 */
	private MSG() {
		super();
	}
}