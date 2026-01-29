/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.wmh.car.osgi.deploy;

import static biz.wmh.car.osgi.bundle.VAL.osgi_install_area;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import biz.wmh.car.SYS;
import biz.wmh.car.osgi.framework.XFramework;

/**
 * Represents the OSGi storage for caching the bundles of the framework.
 *
 * @version 2.0.0 21.01.2026 10:17:23
 */
public class BundleStorage {

	private Map<String, Bundle> myMap;
	private List<Bundle> myList;
	
	/**
	 * Creates a default <code>BundleStorage</code> instance.
	 */
	public BundleStorage() {
		super();
		
		myMap = new HashMap<String, Bundle>();
		myList = new ArrayList<Bundle>();
		
		loadBundles();
	}

	/**
	 * Cleanup internal storage.
	 */
	public void dispose() {
		myMap.clear();
		myList.clear();
		
		myMap = null;
		myList = null;
	}
	
	/**
	 * Look up a bundle by its location id.
	 * 
	 * @param aLocation the storage loccation in the install area
	 * @return the bundle found or <code>null</code>
	 */
	public Bundle getBundle(String aLocation) {
		return myMap.get(aLocation);
	}

	/**
	 * Uninstalls all bundles where the corresponding jar file is not existing in
	 * the installation area.
	 * 
	 * @param anArea the reference to the installation area
	 */
	public void uninstallBundles(InstallArea anArea) {
		myList.stream()
		    .filter(b -> !anArea.contains(b))
		    .forEach(b -> uninstall(b));
	}

	/**
	 * Loads all installed bundles where the location points to the OSGi
	 * installation area.
	 */
	private void loadBundles() {
		BundleContext l_ctx = XFramework.context();
		Bundle[] l_bundles = l_ctx.getBundles();
		String l_areaName = l_ctx.getProperty(osgi_install_area);
		
		for (Bundle l_bundle : l_bundles) {
			String l_location = l_bundle.getLocation();
			
			if (l_location.startsWith(l_areaName)) {
				myMap.put(l_location, l_bundle);
				myList.add(l_bundle);
			}
		}
	}

	/**
	 * Uninstalls a framework bundle.
	 * 
	 * @param aBundle the bundle to uninstall
	 */
	private void uninstall(Bundle aBundle) {
		try {
			aBundle.uninstall();
		} catch (BundleException anEx) {
			throw SYS.LOG.exception(anEx);
		}
	}
}
