/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.deploy;

import java.io.File;

/**
 * Represents a folder in the installation area.
 *
 * @version 2.0.0 21.01.2026 10:17:49
 */
public interface BundleLocation {

	/**
	 * Converts a file paht name to the URI string.
	 * 
	 * @param aPath the name of the file paht
	 * @return the string representation of the file URI.
	 */
	static String toURI(String aPath) {
		String l_ret = aPath;
		File l_file = new File(aPath);

		if (l_file.exists()) {
			l_ret = l_file.toURI().toString();
		}
		return l_ret;
	}
}
