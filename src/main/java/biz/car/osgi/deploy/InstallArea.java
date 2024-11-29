/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.car.osgi.deploy;

import static biz.car.VAL._jar;
import static biz.car.osgi.bundle.VAL.*;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;

import biz.car.SYS;
import biz.car.osgi.launch.XFramework;

/**
 * Functions related to the OSGi installation area.
 *
 * @version 1.0.0 09.10.2024 11:29
 */
public class InstallArea {

	private static Pattern areaPath = Pattern.compile("(\\/\\d{2})"); //$NON-NLS-1$

	/**
	 * Gets a list of all installed bundles where the location points to the OSGi
	 * installation area.
	 * 
	 * @return the list of all bundles located in the installation area
	 */
	public static List<Bundle> bundles() {
		BundleContext l_ctx = XFramework.getBundleContext();
		Bundle[] l_bundles = l_ctx.getBundles();
		String l_areaName = l_ctx.getProperty(osgi_install_area);
		List<Bundle> l_ret = Arrays.asList(l_bundles).stream()
				.filter(b -> b.getLocation().startsWith(l_areaName))
				.collect(Collectors.toList());

		return l_ret;
	}

	/**
	 * Installs a bundle from the installation area into the OSGi framework.
	 * 
	 * @param aLocation the storage location within the installation area
	 */
	public static void install(String aLocation) {
		try {
			BundleContext l_ctx = XFramework.getBundleContext();
			Bundle l_bundle = l_ctx.installBundle(aLocation);
			String l_header = l_bundle.getHeaders().get(Constants.FRAGMENT_HOST);

			if (l_header == null) {
				BundleStartLevel l_bsl = l_bundle.adapt(BundleStartLevel.class);
				String l_lvl = l_ctx.getProperty(osgi_bundles_defaultStartLevel);
				Matcher l_matcher = areaPath.matcher(aLocation);

				if (l_matcher.find()) {
					l_lvl = l_matcher.group().substring(1, 3);
				}
				l_bsl.setStartLevel(Integer.parseInt(l_lvl));
				l_bundle.start(Bundle.START_ACTIVATION_POLICY);
			}
		} catch (BundleException anEx) {
			throw SYS.LOG.exception(anEx);
		}
	}

	/**
	 * Gets a list of all jar files located in the OSGi installation area.
	 * 
	 * @return the list of all jar files located in the installation area
	 */
	public static List<String> jarFiles() {
		try {
			BundleContext l_ctx = XFramework.getBundleContext();
			String l_areaName = l_ctx.getProperty(osgi_install_area);
			URI l_uri = new URI(l_areaName);
			File l_dir = new File(l_uri);
			List<String> l_ret = jarFiles(l_dir);

			return l_ret;
		} catch (URISyntaxException anEx) {
			throw SYS.LOG.exception(anEx);
		}
	}

	/**
	 * Uninstalls a framework bundle.
	 * 
	 * @param aBundle the bundle to uninstall
	 */
	public static void uninstall(Bundle aBundle) {
		try {
			aBundle.uninstall();
		} catch (BundleException anEx) {
			throw SYS.LOG.exception(anEx);
		}
	}

	/**
	 * Updates a framework bundle with the jar from the installation area.<br>
	 * The update operation is only performed if the jar file is newer than the
	 * bundle.
	 * 
	 * @param aBundle the bundle to update
	 */
	public static void upDate(Bundle aBundle) {
		long l_blm = aBundle.getLastModified();
		String l_loc = aBundle.getLocation();
		try {
			URI l_uri = new URI(l_loc);
			File l_jar = new File(l_uri);
			long l_jlm = l_jar.lastModified();

			if (l_jlm > l_blm) {
				aBundle.update();
			}
		} catch (Exception anEx) {
			throw SYS.LOG.exception(anEx);
		}
	}

	private static List<String> jarFiles(File aDir) {
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
}
