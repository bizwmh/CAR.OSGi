/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.wmh.car.osgi.bundle;

import static biz.wmh.car.bundle.VAL._properties;

import com.typesafe.config.Config;

import biz.wmh.car.config.ACS;

/**
 * Framework specific keys for runtime options.
 *
 * @version 2.0.0 08.01.2026 11:00:50
 */
public class KEY {

	public static final Config conf;
	
	// -------------------------------------------------------------------------
	// Initialize the static fields
	// -------------------------------------------------------------------------
	static {
        conf = ACS.initialize(KEY.class, BND.FRAMEWORK + _properties);
	}
	
	/**
	 * Creates a default <code>KEY</code> instance.
	 */
	private KEY() {
		super();
	}
}
