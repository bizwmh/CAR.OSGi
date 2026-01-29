/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.wmh.car.osgi.deploy;

import java.util.List;

import org.osgi.framework.Bundle;

import biz.wmh.car.SYS;
import biz.wmh.car.osgi.framework.XFramework;

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
	 * Processes the installation area after a change in the install area when the
	 * framework is already running.<br>
	 * Processes the complete deployment cycle: 1. Reconcile bundles
	 * (uninstall/update/install) 2. Refresh framework 3. Wait for
	 * PACKAGES_REFRESHED 4. Start new/updated bundles
	 */
	public void processDeploymentCycle() {
		InstallArea l_area = new InstallArea();
		List<Bundle> l_bl = l_area.reconcile();

		if (l_bl.size() > 0) {
			XFramework.refreshAndWait();
			startBundles(l_bl);
		}
	}

	/**
	 * Processes the installation area after the framework is initialized.
	 */
	public void processInstallArea() {
		InstallArea l_area = new InstallArea();
		List<Bundle> l_bl = l_area.reconcile();

		startBundles(l_bl);
	}

	private void startBundles(List<Bundle> aBundles) {
		// start bundles if not fragment
		for (Bundle l_installed : aBundles) {
			if (!XFramework.isFragment(l_installed)) {
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
}
