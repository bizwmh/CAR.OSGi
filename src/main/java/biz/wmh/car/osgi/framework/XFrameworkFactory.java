/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved.
 * -------------------------------------------------------------------------- */

package biz.wmh.car.osgi.framework;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import biz.wmh.car.SYS;
import biz.wmh.car.bundle.MSG;

/**
 * Creates a new instance of the OSGi framework.
 *
 * @version 2.0.0 08.01.2026 11:00:50
 */
public class XFrameworkFactory {

    /**
     * Creates a new instance of the OSGi framework.
     *
     * @param aConfiguration The framework properties to configure the new framework
     *                       instance.<br>
     *                       May be <code>null</code>. For configuration parameters
     *                       not set the framework will use default values.
     * @return the new framework instance
     */
    static Framework get(Map<String, String> aConfiguration) {
        ServiceLoader<FrameworkFactory> l_sl = ServiceLoader.load(FrameworkFactory.class);
        Optional<FrameworkFactory> l_ofac = l_sl.findFirst();
        FrameworkFactory l_fac = l_ofac.orElseThrow(() -> {
        	return SYS.LOG.exception(MSG.RESOURCE_NOT_FOUND, FrameworkFactory.class.getName());
        });
        Framework l_ret = l_fac.newFramework(aConfiguration);

        return l_ret;
    }
}
