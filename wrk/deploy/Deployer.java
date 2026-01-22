/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.car.osgi.deploy;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.FrameworkWiring;

import biz.car.SYS;
import biz.car.osgi.framework.XFramework;

/**
 * 
 * @version 2.0.0 08.01.2026 11:00:50
 */
public class Deployer {

	private List<String> jarList;

	/**
	 * The installation area now only contains bundles that are not yet processed.
	 * We resolve all dependencies and then start the bundles.
	 */
	public void onCompleted() {
		List<Bundle> l_bl = new ArrayList<Bundle>();

		jarList.forEach(location -> {
			Bundle l_bundle = InstallArea.install(location);

			l_bl.add(l_bundle);
		});

		// start bundles if not fragment
		for (Bundle l_installed : l_bl) {
			if (!isFragment(l_installed)) {
				try {
					// Nutzung von START_TRANSIENT verhindert, dass der Start-Status
					// persistent gespeichert wird (sauberer für Development)
					l_installed.start(Bundle.START_ACTIVATION_POLICY);
				} catch (Exception anEx) {
					// Loggen, aber nicht den ganzen Batch abbrechen
					SYS.LOG.error(anEx.getMessage());
				}
			}
		}
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

	private boolean isFragment(Bundle aBundle) {
		return aBundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
	}
}
