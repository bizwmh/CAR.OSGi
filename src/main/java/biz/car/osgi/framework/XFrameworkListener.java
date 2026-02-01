/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.framework;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

import biz.car.osgi.bundle.BND;
import biz.car.osgi.bundle.DIAG;
import biz.car.osgi.bundle.MSG;

/**
 * Logs the events of the OSGi framework.
 *
 * @version 2.0.0 08.01.2026 11:00:50
 */
public class XFrameworkListener implements FrameworkListener {

	/**
	 * Creates a default <code>XFrameworkListener</code> instance.
	 */
	public XFrameworkListener() {
		super();
	}

	@Override
	public void frameworkEvent(FrameworkEvent anEvent) {
		Bundle l_bundle = anEvent.getBundle();
		int l_type = anEvent.getType();
		String l_event = BND.frameworkEvent(l_type);

		DIAG.LOG.info(MSG.FRAMEWORK_EVENT, l_event, l_bundle.getSymbolicName());
	}
}
