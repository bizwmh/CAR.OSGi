/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.bundle;

import biz.car.XLogger;
import biz.car.XLoggerFactory;
import biz.car.config.ACS;

/**
 * The logger for diagnostic purposes.
 *
 * @version 2.0.0 15.01.2026 11:57:38
 */
public class DIAG {

	/**
	 * The default diagnose logger. <br>
	 * The name of the logger can be set in the application properties file. The key
	 * is 'diagnoseLogger'.
	 */
	public static final XLogger LOG;
	
	static {
		String l_name = ACS.APP.getString(VAL.diagnoseLogger);
		LOG = XLoggerFactory.getLogger(l_name);
	}

	/**
	 * Creates a default <code>DIAG</code> instance.
	 */
	private DIAG() {
		super();
	}
}
