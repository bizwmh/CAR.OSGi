/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.deploy;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import biz.car.SYS;

/**
 * The processor of the jar files in the installation area for the OSGi
 * framework bundles.
 *
 * @version 2.0.0 21.01.2026 10:16:53
 */
public class Deployer {
	
	/**
	 * Creates a default <code>Deployer</code> instance.
	 */
	public Deployer() {
		super();
	}

	/**
	 * Processes the installation area after the framework is initialized.
	 */
	public void processInstallArea() {
		BundleStorage l_bs = new BundleStorage();
		InstallArea l_area = new InstallArea();
		
		l_bs.uninstallBundles(l_area);
		
		List<Bundle> l_bl = l_area.reconcile(l_bs);
		
		startBundles(l_bl);
		l_bs.dispose();
		l_area.dispose();
	}

	private void startBundles(List<Bundle> aBundles) {
		// start bundles if not fragment
		for (Bundle l_installed : aBundles) {
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

	private boolean isFragment(Bundle aBundle) {
		return aBundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
	}
}
