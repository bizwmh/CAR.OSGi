/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.wmh.car.osgi.framework;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import biz.wmh.car.osgi.bundle.BND;
import biz.wmh.car.osgi.bundle.DIAG;
import biz.wmh.car.osgi.bundle.MSG;

/**
 * Logs the events of the OSGi framework.
 *
 * @version 2.0.0 08.01.2026 11:00:50
 */
public class XServiceListener implements ServiceListener {

	/**
	 * Creates a default <code>XFrameworkListener</code> instance.
	 */
	public XServiceListener() {
		super();
	}

	@Override
	public void serviceChanged(ServiceEvent anEvent) {
		ServiceReference<?> l_ref = anEvent.getServiceReference();
		int l_type = anEvent.getType();
		String l_event = BND.serviceEvent(l_type);

		DIAG.LOG.info(MSG.SERVICE_EVENT, l_event, l_ref.toString());
	}
}
