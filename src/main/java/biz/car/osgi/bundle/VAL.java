/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.car.osgi.bundle;

import biz.wmh.car.util.SFI;

/**
 * Bundle constants.
 *
 * @version 2.0.0 08.01.2026 11:00:50
 */
public class VAL {

	public static String bundle_startLevel;
	public static String framework_configuration_area;
	public static String framework_data_area;
	public static String osgi_install_area;

	// -------------------------------------------------------------------------
	// Initialize the static fields
	// -------------------------------------------------------------------------
	static {
		SFI.initialize(VAL.class);
	}

	/**
	 * Creates a default VAR instance.
	 */
	private VAL() {
		super();
	}
}
