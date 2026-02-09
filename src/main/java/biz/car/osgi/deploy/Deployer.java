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
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import biz.car.SYS;
import biz.car.XRuntimeException;
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
public class Deployer implements DirectoryListener {

	private ScheduledFuture<?> pendingRefresh = null;
	private final ScheduledExecutorService scheduler;
	private final DirectoryWatcher watcher;

	/**
	 * Creates a default <code>Deployer</code> instance.
	 */
	public Deployer() {
		super();

		watcher = new DirectoryWatcher(BND.BUNDLE_WATCHER);
		scheduler = Executors.newSingleThreadScheduledExecutor();
	}

	@Override
	public void onEvent(Path aPath, List<WatchEvent<?>> aEvents) {
		if (pendingRefresh != null && !pendingRefresh.isDone()) {
			pendingRefresh.cancel(false);
		}
		aEvents.forEach(e -> onEvent(aPath, e));

		pendingRefresh = scheduler.schedule(() -> {
			refreshFramework();
		}, 1000, TimeUnit.MILLISECONDS);
	}

	/**
	 * Processes the installation area after the framework is initialized.
	 */
	public void processInstallArea() {
		InstallArea l_area = new InstallArea();
		List<Bundle> l_bl = l_area.reconcile();

		XFramework.startBundles(l_bl);
	}

	/**
	 * Stop the file system watch service.
	 */
	public void stop() {
		watcher.stop();
	}

	/**
	 * Starts the file system watch service and registers the install area with the
	 * watch service.
	 */
	public void watchInstallArea() {
		try {
			watcher.start();
			registerInstallArea();
		} catch (Exception anEx) {
		}
	}

	private void onEvent(Path aPath, WatchEvent<?> aEvent) {
		Kind<?> l_kind = aEvent.kind();

		if (l_kind != StandardWatchEventKinds.OVERFLOW) {
			if (l_kind == StandardWatchEventKinds.ENTRY_CREATE) {
				Path l_path = (Path) aEvent.context();
				l_path = aPath.resolve(l_path);

				if (Files.isDirectory(l_path)) {
					watcher.registerAll(aPath, null);
				}
			}
		}

		StringBuffer l_event = new StringBuffer(aEvent.kind().name());
		l_event.append(" || "); //$NON-NLS-1$
		l_event.append(aEvent.count());
		l_event.append(" || "); //$NON-NLS-1$
		l_event.append(aEvent.context());
		l_event.append(" || "); //$NON-NLS-1$
		l_event.append(aPath); // $NON-NLS-1$

		SYS.LOG.debug(l_event.toString());
	}

	/**
	 * Processes the installation area after a change in the install area when the
	 * framework is already running.<br>
	 * Processes the complete deployment cycle:
	 * <ol>
	 * <li><b>Reconcile bundles (uninstall/update/install)</b>
	 * <div style="margin-left: 3px;"> Compares the current state with the target
	 * configuration and triggers the respective <code>Bundle.uninstall()</code>,
	 * <code>update()</code> or <code>installBundle()</code> operations. </div></li>
	 * <li><b>Refresh framework</b> <div style="margin-left: 3px;"> Invokes
	 * <code>FrameworkWiring.refreshBundles()</code> to re-resolve dependencies,
	 * wire exported packages, and clean up obsolete class loaders. </div></li>
	 * <li><b>Wait for PACKAGES_REFRESHED</b> <div style="margin-left: 3px;"> Blocks
	 * or synchronizes on the <code>FrameworkEvent.PACKAGES_REFRESHED</code> to
	 * ensure the bundle graph is stable and consistent. </div></li>
	 * <li><b>Start new/updated bundles</b> <div style="margin-left: 3px;"> Iterates
	 * through all bundles in the target state and calls <code>Bundle.start()</code>
	 * according to their activation policy and start level. </div></li>
	 * </ol>
	 */
	private void refreshFramework() {
		InstallArea l_area = new InstallArea();
		List<Bundle> l_bl = l_area.reconcile();

		if (l_bl.size() > 0) {
			XFramework.refreshAndWait();
			XFramework.startBundles(l_bl);
		}
	}

	private void registerInstallArea() {
		try {
			BundleContext l_ctx = XFramework.context();
			String l_areaName = l_ctx.getProperty(osgi_install_area);
			l_areaName = BundleLocation.toURI(l_areaName);
			URI l_uri = new URI(l_areaName);
			File l_dir = new File(l_uri);
			Path l_path = l_dir.toPath();

			watcher.registerAll(l_path, this);
		} catch (XRuntimeException anEx) {
		} catch (Exception anEx) {
			SYS.LOG.exception(anEx);
		}
	}
}
