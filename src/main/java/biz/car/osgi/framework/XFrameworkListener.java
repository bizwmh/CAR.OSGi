/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.framework;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

import biz.car.SYS;
import biz.car.osgi.bundle.MSG;

/**
 * Logs the events of the OSGi framework.
 *
 * @version 1.0.0 11.10.2024 08:28:44
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

		SYS.LOG.info(MSG.FRAMEWORK_EVENT, l_type, l_bundle.getSymbolicName());
	}
}
