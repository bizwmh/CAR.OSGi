/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.wmh.car.osgi.deploy;

import static biz.wmh.car.osgi.bundle.VAL.osgi_install_area;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;

import biz.wmh.car.CAR;
import biz.wmh.car.SYS;
import biz.wmh.car.osgi.bundle.KEY;
import biz.wmh.car.osgi.bundle.VAL;
import biz.wmh.car.osgi.framework.XFramework;

/**
 * Functions related to the OSGi installation area.
 *
 * @version 2.0.0 21.01.2026 10:17:11
 */
public class InstallArea implements CAR {

	private static Pattern areaPath = Pattern.compile("(\\/\\d{2})"); //$NON-NLS-1$
	private static final InstallAreaWatcher watcher = new InstallAreaWatcher();

	/**
	 * @return a reference to the singleton <code>InstallAreaWatcher</code>
	 *         instance.
	 */
	public static InstallAreaWatcher watcher() {
		return watcher;
	}

	private List<String> jars;

	/**
	 * Creates a default <code>InstallArea</code> instance.
	 */
	public InstallArea() {
		super();

		jars = jarFiles();
	}

	/**
	 * Checks if a jar file exists that corresponds to the location identifier of
	 * the given bundle.
	 * 
	 * @param aBundle the bundle to check
	 * @return <code>true</code> if the install area contains a jar that matches the
	 *         location identifier of the bundle
	 */
	public boolean contains(Bundle aBundle) {
		String l_location = aBundle.getLocation();

		return jars.contains(l_location);
	}

	/**
	 * Synchronizes the jars in the install area with the bundles in the bundle
	 * storage.<br>
	 * Three cases are covered:
	 * <ul>
	 * <li>the jar already exists in the bundle storage and the jar is not newer. No
	 * to do.
	 * <li>the jar already exists in the bundle storage but has a newer timestamp.
	 * The bundle is updated.
	 * <li>the jar is not existing in the bundle storage. The jar is installed but
	 * not started.
	 * </ul>
	 * 
	 * @return the list of newly installed bundles
	 */
	public List<Bundle> reconcile() {
		BundleStorage l_bs = new BundleStorage();
		List<Bundle> l_ret = new ArrayList<Bundle>();

		l_bs.uninstallBundles(this);

		for (String l_location : jars) {
			Bundle l_bundle = l_bs.getBundle(l_location);

			if (l_bundle == null) {
				l_bundle = install(l_location);

				l_ret.add(l_bundle);
			} else {
				if (update(l_bundle)) {
					l_ret.add(l_bundle);
				}
			}
		}
		l_bs.dispose();
		jars.clear();

		return l_ret;
	}

	/**
	 * Installs a bundle from the installation area into the OSGi framework.<br>
	 * The bundle is not started.
	 * 
	 * @param aLocation the storage location within the installation area
	 * @return the installed bundle object
	 */
	private Bundle install(String aLocation) {
		try {
			BundleContext l_ctx = XFramework.context();
			Bundle l_ret = l_ctx.installBundle(aLocation);
			BundleStartLevel l_bsl = l_ret.adapt(BundleStartLevel.class);
			String l_key = KEY.conf.getString(VAL.bundle_startLevel);
			String l_lvl = l_ctx.getProperty(l_key);
			Matcher l_matcher = areaPath.matcher(aLocation);

			if (l_matcher.find()) {
				l_lvl = l_matcher.group().substring(1, 3);
			}
			l_bsl.setStartLevel(Integer.parseInt(l_lvl));

			return l_ret;
		} catch (BundleException anEx) {
			throw SYS.LOG.exception(anEx);
		}
	}

	/**
	 * Gets a list of all jar files located in the OSGi installation area.
	 * 
	 * @return the list of all jar files located in the installation area
	 */
	private List<String> jarFiles() {
		try {
			BundleContext l_ctx = XFramework.context();
			String l_areaName = l_ctx.getProperty(osgi_install_area);
			l_areaName = BundleLocation.toURI(l_areaName);
			URI l_uri = new URI(l_areaName);
			File l_dir = new File(l_uri);
			List<String> l_ret = jarFiles(l_dir);

			return l_ret;
		} catch (URISyntaxException anEx) {
			throw SYS.LOG.exception(anEx);
		}
	}

	private List<String> jarFiles(File aDir) {
		ArrayList<String> l_ret = new ArrayList<>();
		File[] l_jars = aDir.listFiles();

		if (l_jars != null) {
			for (int i = 0; i < l_jars.length; i++) {
				File l_jar = l_jars[i];

				if (l_jar.isDirectory()) {
					l_ret.addAll(jarFiles(l_jar));
				} else if (l_jar.getName().endsWith(_jar)) {
					l_ret.add(l_jar.toURI().toString());
				}
			}
		}
		return l_ret;
	}

	/**
	 * Updates a framework bundle with the jar from the installation area.<br>
	 * The update operation is only performed if the jar file is newer than the
	 * bundle.
	 * 
	 * @param aBundle the bundle to update
	 */
	private boolean update(Bundle aBundle) {
		boolean l_ret = false;
		long l_blm = aBundle.getLastModified();
		String l_loc = aBundle.getLocation();
		try {
			URI l_uri = new URI(l_loc);
			File l_jar = new File(l_uri);
			long l_jlm = l_jar.lastModified();

			if (l_jlm > l_blm) {
				l_ret = true;

				aBundle.update();
			}
			return l_ret;
		} catch (Exception anEx) {
			throw SYS.LOG.exception(anEx);
		}
	}
}
