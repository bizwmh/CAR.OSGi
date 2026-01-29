/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.wmh.car.osgi.framework;

import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import biz.wmh.car.osgi.bundle.BND;
import biz.wmh.car.osgi.bundle.DIAG;
import biz.wmh.car.osgi.bundle.MSG;

/**
 * A listener for the events of a bundle in the OSGi framework.
 *
 * @version 2.0.0 08.01.2026 11:00:50
 */
public class XBundleListener implements BundleListener {

    @Override
    public void bundleChanged(BundleEvent anEvent) {
        int l_ev = anEvent.getType();
        Bundle l_bundle = anEvent.getBundle();
        String l_name = l_bundle.getSymbolicName();

        if (l_bundle.getBundleId() != SYSTEM_BUNDLE_ID) {
        	DIAG.LOG.info(MSG.BUNDLE_EVENT, l_name, BND.bundleEvent(l_ev));
        }
    }
}
