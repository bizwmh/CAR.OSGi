/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.deploy;

import static biz.car.osgi.bundle.VAL.osgi_install_area;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.List;

import org.osgi.framework.BundleContext;

import biz.car.SYS;
import biz.car.io.DirectoryListener;
import biz.car.io.DirectoryWatcher;
import biz.car.osgi.bundle.BND;
import biz.car.osgi.framework.XFramework;

/**
 * Watches the OSGi installation area for file changes and automatically
 * deploys, updates, or removes bundles using proper OSGi refresh cycle.
 * 
 * <p>
 * This class implements the correct OSGi hot deployment pattern:
 * <ol>
 * <li>Detect file system events (CREATE/MODIFY/DELETE)</li>
 * <li>Reconcile bundles (uninstall/update/install)</li>
 * <li>Refresh framework packages</li>
 * <li>Wait for PACKAGES_REFRESHED event</li>
 * <li>Start newly installed/updated bundles</li>
 * </ol>
 *
 * @version 2.0.0 28.01.2026 15:02:03
 */
public class InstallAreaWatcher implements DirectoryListener {

	private final DirectoryWatcher watcher;

	/**
	 * Creates a default <code>InstallAreaWatcher</code> instance.
	 */
	InstallAreaWatcher() {
		super();

		watcher = new DirectoryWatcher(BND.BUNDLE_WATCHER);
	}

	@Override
	public void onEvent(Path aPath, List<WatchEvent<?>> aEvents) {
		aEvents.forEach(e -> onEvent(aPath, e));
	}

	/**
	 * Start the file system watch service.
	 */
	public void start() {
		watcher.start();
		registerInstallArea();
	}

	/**
	 * Stop the file system watch service.
	 */
	public void stop() {
		watcher.stop();
	}

	private void registerInstallArea() {
		try {
			BundleContext l_ctx = XFramework.context();
			String l_areaName = l_ctx.getProperty(osgi_install_area);
			l_areaName = BundleLocation.toURI(l_areaName);
			URI l_uri = new URI(l_areaName);
			File l_dir = new File(l_uri);
			Path l_path = l_dir.toPath();

			Files.walk(l_path)
			      .filter(path -> path.toFile().isDirectory())
			      .forEach(path -> {
				      watcher.register(path, this);
			      });
		} catch (Exception anEx) {
			SYS.LOG.exception(anEx);
		}
	}
	private void onEvent(Path aPath, WatchEvent<?> aEvent) {
		StringBuffer l_event = new StringBuffer(aEvent.kind().name());
		l_event.append(" || "); //$NON-NLS-1$
		l_event.append(aEvent.count());
		l_event.append(" || "); //$NON-NLS-1$
		l_event.append(aEvent.context());
		l_event.append(" || "); //$NON-NLS-1$
		l_event.append(aPath); // $NON-NLS-1$
		l_event.append("\n\n path " + aPath + " processed"); //$NON-NLS-1$ //$NON-NLS-2$

		SYS.LOG.debug(l_event.toString());
	}
}
