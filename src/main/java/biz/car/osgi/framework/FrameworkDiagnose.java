/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.framework;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;

import biz.car.osgi.bundle.BND;
import biz.car.osgi.bundle.DIAG;
import biz.car.util.XTimestamp;

/**
 * Inspects an OSGi framework for diagnostic purposes.
 *
 * @version 2.0.0 03.02.2026 15:15:40
 */
public interface FrameworkDiagnose {

	/**
	 * Logs diagnostic information about the OSGi framework instance.
	 * 
	 * @param aFWK the Framework instance to inspect
	 */
	static void accept(Framework aFWK) {
		FrameworkDTO l_dto = aFWK.adapt(FrameworkDTO.class);
		FrameworkStartLevelDTO l_sl = aFWK.adapt(FrameworkStartLevelDTO.class);
		Map<String, Object> l_props = l_dto.properties;
		Bundle[] l_bundles = aFWK.getBundleContext().getBundles();
		StringBuffer l_bProps = new StringBuffer();

		// list the framework properties
		l_props.entrySet().stream()
		      .forEach(entry -> {
			      l_bProps.append("\n\t\t\t\t" + entry.getKey()); //$NON-NLS-1$
			      l_bProps.append(" = "); //$NON-NLS-1$
			      l_bProps.append(entry.getValue());
		      });
		l_bProps.append("\n\n\t\t\t\tStartlevel = " + l_sl.startLevel); //$NON-NLS-1$
		DIAG.LOG.info("Framework Properties {}", l_bProps); //$NON-NLS-1$

		// list the info about the installed bundles
		Arrays.asList(l_bundles).stream()
		      .map(bundle -> {
			      BundleDTO l_bd = bundle.adapt(BundleDTO.class);
			      StringBuffer l_bBundle = new StringBuffer("Bundle " + l_bd.symbolicName); //$NON-NLS-1$
			      BundleStartLevelDTO l_bsl = bundle.adapt(BundleStartLevelDTO.class);

			      Date l_date = new Date(l_bd.lastModified);
			      XTimestamp l_ts = new XTimestamp(l_date);
			      l_bBundle.append("\n\t\t\t\tID         " + l_bd.id); //$NON-NLS-1$
			      l_bBundle.append("\n\t\t\t\tState      " + BND.state(l_bd.state)); //$NON-NLS-1$
			      l_bBundle.append("\n\t\t\t\tVersion    " + l_bd.version); //$NON-NLS-1$
			      l_bBundle.append("\n\t\t\t\tupdated    " + l_ts.toString()); //$NON-NLS-1$
			      l_bBundle.append("\n\t\t\t\tStartlevel " + l_bsl.startLevel); //$NON-NLS-1$
			      l_bBundle.append("\n\t\t\t\tLocation   " + bundle.getLocation()); //$NON-NLS-1$

			      return l_bBundle;
		      })
		      .forEach(info -> DIAG.LOG.info(info.toString()));
	}
}
