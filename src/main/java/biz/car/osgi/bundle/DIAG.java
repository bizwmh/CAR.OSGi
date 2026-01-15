/* --------------------------------------------------------------------------
 * Project: XXX
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.wmh.car.XLogger;
import biz.wmh.car.config.ACS;

/**
 * The logger for diagnostic purposes.
 *
 * @version 1.0.0 15.01.2026 11:57:38
 */
public class DIAG implements XLogger {

	/**
	 * The default diagnose logger. <br>
	 * The name of the logger can be set in the application properties file. The key
	 * is 'diagnoseLogger'.
	 */
	public static final XLogger LOG = new DIAG();

	private final Logger logger;

	/**
	 * Creates a default <code>DIAG</code> instance.
	 */
	public DIAG() {
		super();

		String l_name = ACS.APP.getString("diagnoseLogger"); //$NON-NLS-1$
		logger = LoggerFactory.getLogger(l_name);
	}

	@Override
	public Logger logger() {
		return logger;
	}
}
