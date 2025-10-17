/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.car.osgi.framework;

import static org.osgi.framework.Constants.*;

import biz.car.osgi.bundle.BND;
import biz.car.osgi.bundle.MSG;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import biz.car.SYS;

/**
 * A listener for the events of a bundle in the OSGi framework.
 *
 * @version 2.0.0 13.10.2025 15:29:07
 */
public class XBundleListener implements BundleListener {

    @Override
    public void bundleChanged(BundleEvent anEvent) {
        int l_ev = anEvent.getType();
        Bundle l_bundle = anEvent.getBundle();
        String l_name = l_bundle.getSymbolicName();

        if (l_bundle.getBundleId() != SYSTEM_BUNDLE_ID) {
            SYS.LOG.info(MSG.BUNDLE_EVENT, l_name, BND.bundleEvent(l_ev));
        }
    }
}
