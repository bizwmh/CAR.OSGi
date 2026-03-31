/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.car.osgi.bundle;

import static biz.car.bundle.VAL._properties;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

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
		String l_res = BND.FRAMEWORK + _properties;
		ClassLoader l_cl = KEY.class.getClassLoader();
        conf = ConfigFactory.parseResources(l_cl, l_res);
	}
	
	/**
	 * Creates a default <code>KEY</code> instance.
	 */
	private KEY() {
		super();
	}
}
