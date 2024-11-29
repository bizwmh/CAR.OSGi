/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.car.osgi.bundle;

import biz.car.util.SFI;

/**
 * Bundle constants.
 *
 * @version 1.0.0 11.10.2024 08:22:46
 */
public class VAL {

	public static String framework_data_area;
	public static String osgi_bundles_defaultStartLevel;
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
