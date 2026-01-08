/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.car.osgi.deploy;

import java.util.List;

import org.osgi.framework.Bundle;

/**
 * The processor of the jar files in the installation area for the OSGi
 * framework bundles.
 *
 * @version 2.0.0 08.01.2026 11:00:50
 */
public class Deployer {

	private List<String> jarList;

	/**
	 * All jar files of the installation area that are not yet processed will be
	 * installed in the framework.
	 */
	public void onCompleted() {
		jarList.forEach(InstallArea::install);
	}

	/**
	 * Initializes this instance of the deployer.
	 */
	public void onInit() {
		jarList = InstallArea.jarFiles();
	}

	/**
	 * Processes an installed bundle.<br>
	 * The value of the location attribute is used to look up the corresponding file
	 * on the file system. If not found, the bundle will be uninstalled. Otherwise
	 * it will be updated.
	 *
	 * @param aBundle the already installed bundle
	 */
	public void onNext(Bundle aBundle) {
		String l_jar = aBundle.getLocation();

		if (jarList.remove(l_jar)) {
			InstallArea.upDate(aBundle);
		} else {
			InstallArea.uninstall(aBundle);
		}
	}
}
