/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.car.osgi.bundle;

import static biz.car.VAL._properties;

import com.typesafe.config.Config;

import biz.car.config.ACS;

/**
 * Framework specific keys for runtime options.
 *
 * @version 2.0.0 14.10.2025 11:55:48
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
