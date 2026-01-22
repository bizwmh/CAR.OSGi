/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.deploy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;

import org.osgi.framework.BundleContext;

import biz.car.SYS;
import biz.car.XRunnable;
import biz.car.config.ConfigAdapter;
import biz.car.osgi.bundle.VAL;
import biz.car.osgi.framework.XFramework;

/**
 * TODO DirectoryWatcher comment
 *
 * @version 2.0.0 19.01.2026 14:17:06
 */
public class DirectoryWatcher2 extends ConfigAdapter implements XRunnable {

	/**
	 * Creates a default <code>DirectoryWatcher</code> instance.
	 */
	public DirectoryWatcher2() {
		super();
	}

	@Override
	public void dispose() {
		// nothing to do
	}

	@Override
	public void exec() {
		BundleContext l_ctx = XFramework.getBundleContext();
		String l_area = l_ctx.getProperty(VAL.osgi_install_area);
		l_area = l_area.replace("file:/", ""); //$NON-NLS-1$ //$NON-NLS-2$
		Path l_path = Path.of(l_area);
		DirectoryVisitor l_visitor = new DirectoryVisitor(l_path);
		
		try {
			Files.walkFileTree(l_path, l_visitor);
		} catch (IOException anEx) {
			throw SYS.LOG.exception(anEx);
		}
	}
}
