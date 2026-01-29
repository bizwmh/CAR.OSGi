/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.deploy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.FrameworkWiring;

import biz.car.CAR;
import biz.car.SYS;
import biz.car.osgi.bundle.VAL;
import biz.car.osgi.framework.XFramework;

/**
 * Watches the OSGi installation area for file changes and automatically
 * deploys, updates, or removes bundles using proper OSGi refresh cycle.
 * 
 * <p>This class implements the correct OSGi hot deployment pattern:
 * <ol>
 * <li>Detect file system events (CREATE/MODIFY/DELETE)</li>
 * <li>Reconcile bundles (uninstall/update/install)</li>
 * <li>Refresh framework packages</li>
 * <li>Wait for PACKAGES_REFRESHED event</li>
 * <li>Start newly installed/updated bundles</li>
 * </ol>
 *
 * @version 2.1.0 23.01.2026 14:00:00
 */
public class BundleDeploymentWatcher implements Runnable, CAR {

	private static final long SETTLE_TIME_MS = 500; // Wait time for file operations to complete
	private static final long REFRESH_TIMEOUT_MS = 30000; // 30 seconds timeout for refresh
	
	private final WatchService watchService;
	private final Map<WatchKey, Path> watchKeys;
	private final BundleContext bundleContext;
	private volatile boolean running;
	
	/**
	 * Creates a new BundleDeploymentWatcher instance.
	 * 
	 * @throws IOException if the watch service cannot be created
	 */
	public BundleDeploymentWatcher() throws IOException {
		this.watchService = FileSystems.getDefault().newWatchService();
		this.watchKeys = new HashMap<>();
		this.bundleContext = XFramework.context();
		this.running = false;
	}

	/**
	 * Starts monitoring the installation area for changes.
	 */
	@Override
	public void run() {
		try {
			// Register the installation area and all subdirectories
			registerInstallArea();
			
			running = true;
			SYS.LOG.info("Hot deployment watcher started");
			
			// Main watch loop
			while (running) {
				WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
				
				if (key == null) {
					continue;
				}
				
				Path directory = watchKeys.get(key);
				if (directory == null) {
					key.reset();
					continue;
				}
				
				// Check if any JAR files were affected
				boolean jarFilesChanged = false;
				for (WatchEvent<?> event : key.pollEvents()) {
					if (isJarFileEvent(event, directory)) {
						jarFilesChanged = true;
						break;
					}
				}
				
				// Process JAR changes if detected
				if (jarFilesChanged) {
					// Wait for file operations to settle
					waitForFileSettling();
					
					// Process deployment cycle
					processDeploymentCycle();
				}
				
				// Register new directories
				for (WatchEvent<?> event : key.pollEvents()) {
					handleNewDirectory(event, directory);
				}
				
				// Reset the key
				boolean valid = key.reset();
				if (!valid) {
					watchKeys.remove(key);
					if (watchKeys.isEmpty()) {
						break;
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			SYS.LOG.info("Hot deployment watcher interrupted");
		} catch (Exception e) {
			SYS.LOG.error("Hot deployment watcher error", e);
		} finally {
			cleanup();
		}
	}

	/**
	 * Stops the deployment watcher.
	 */
	public void stop() {
		running = false;
		SYS.LOG.info("Stopping hot deployment watcher");
	}

	/**
	 * Processes the complete deployment cycle:
	 * 1. Reconcile bundles (uninstall/update/install)
	 * 2. Refresh framework
	 * 3. Wait for PACKAGES_REFRESHED
	 * 4. Start new/updated bundles
	 */
	private void processDeploymentCycle() {
		try {
			SYS.LOG.info("Hot deployment cycle started");
			
			// Step 1: Reconcile bundles using existing Deployer logic
			BundleStorage bundleStorage = new BundleStorage();
			InstallArea installArea = new InstallArea();
			
			// Uninstall removed bundles
			bundleStorage.uninstallBundles(installArea);
			
			// Install new and update existing bundles (without starting)
			java.util.List<Bundle> newOrUpdatedBundles = installArea.reconcile(bundleStorage);
			
			SYS.LOG.info("Reconciled {} bundle(s)", newOrUpdatedBundles.size());
			
			// Step 2 & 3: Refresh framework and wait for PACKAGES_REFRESHED
			if (!newOrUpdatedBundles.isEmpty()) {
				refreshAndWait();
			}
			
			// Step 4: Start bundles after successful refresh
			startBundles(newOrUpdatedBundles);
			
			// Cleanup
			bundleStorage.dispose();
			installArea.dispose();
			
			SYS.LOG.info("Hot deployment cycle completed");
			
		} catch (Exception e) {
			SYS.LOG.error("Hot deployment cycle failed", e);
		}
	}

	/**
	 * Refreshes the framework and waits for PACKAGES_REFRESHED event.
	 * 
	 * @throws InterruptedException if waiting is interrupted
	 */
	private void refreshAndWait() throws InterruptedException {
		SYS.LOG.info("Refreshing framework packages");
		
		// Create a latch to wait for refresh completion
		final CountDownLatch refreshLatch = new CountDownLatch(1);
		
		// Create framework listener for PACKAGES_REFRESHED event
		FrameworkListener refreshListener = new FrameworkListener() {
			@Override
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
					refreshLatch.countDown();
				}
			}
		};
		
		try {
			// Register the listener
			bundleContext.addFrameworkListener(refreshListener);
			
			// Trigger framework refresh
			Bundle systemBundle = bundleContext.getBundle(0);
			FrameworkWiring frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
			
			if (frameworkWiring != null) {
				// Refresh all bundles (null = all bundles)
				frameworkWiring.refreshBundles(null, refreshListener);
				
				// Wait for PACKAGES_REFRESHED event
				boolean refreshed = refreshLatch.await(REFRESH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
				
				if (refreshed) {
					SYS.LOG.info("Framework packages refreshed successfully");
				} else {
					SYS.LOG.warn("Framework refresh timed out after {} ms", REFRESH_TIMEOUT_MS);
				}
			} else {
				SYS.LOG.error("Could not adapt system bundle to FrameworkWiring");
			}
			
		} finally {
			// Always remove the listener
			bundleContext.removeFrameworkListener(refreshListener);
		}
	}

	/**
	 * Starts bundles that are not fragments.
	 * 
	 * @param bundles the list of bundles to start
	 */
	private void startBundles(java.util.List<Bundle> bundles) {
		int started = 0;
		int failed = 0;
		
		for (Bundle bundle : bundles) {
			if (!isFragment(bundle)) {
				try {
					// Use START_ACTIVATION_POLICY for lazy activation
					bundle.start(Bundle.START_ACTIVATION_POLICY);
					started++;
					
					SYS.LOG.info("Started bundle: {} [{}]", 
						bundle.getSymbolicName(), bundle.getBundleId());
					
				} catch (Exception e) {
					failed++;
					SYS.LOG.error("Failed to start bundle: {} [{}] - {}", 
						bundle.getSymbolicName(), bundle.getBundleId(), e.getMessage());
				}
			}
		}
		
		if (started > 0 || failed > 0) {
			SYS.LOG.info("Bundle start summary: {} started, {} failed", started, failed);
		}
	}

	/**
	 * Checks if a bundle is a fragment.
	 * 
	 * @param bundle the bundle to check
	 * @return true if the bundle is a fragment
	 */
	private boolean isFragment(Bundle bundle) {
		return bundle.getHeaders().get(org.osgi.framework.Constants.FRAGMENT_HOST) != null;
	}

	/**
	 * Checks if a watch event involves a JAR file.
	 * 
	 * @param event the watch event
	 * @param directory the directory where event occurred
	 * @return true if event involves a JAR file
	 */
	private boolean isJarFileEvent(WatchEvent<?> event, Path directory) {
		WatchEvent.Kind<?> kind = event.kind();
		
		if (kind == StandardWatchEventKinds.OVERFLOW) {
			return false;
		}
		
		@SuppressWarnings("unchecked")
		WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
		Path fileName = pathEvent.context();
		
		return fileName.toString().endsWith(_jar);
	}

	/**
	 * Handles detection of new directories that need to be registered.
	 * 
	 * @param event the watch event
	 * @param directory the parent directory
	 */
	private void handleNewDirectory(WatchEvent<?> event, Path directory) {
		WatchEvent.Kind<?> kind = event.kind();
		
		if (kind != StandardWatchEventKinds.ENTRY_CREATE) {
			return;
		}
		
		@SuppressWarnings("unchecked")
		WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
		Path fileName = pathEvent.context();
		Path fullPath = directory.resolve(fileName);
		File file = fullPath.toFile();
		
		if (file.isDirectory()) {
			try {
				registerDirectory(file.toPath());
				registerSubdirectories(file);
				SYS.LOG.info("Registered new directory for watching: {}", file.getName());
			} catch (IOException e) {
				SYS.LOG.error("Failed to register new directory: {}", file.getName(), e);
			}
		}
	}

	/**
	 * Registers the installation area and all subdirectories for monitoring.
	 * 
	 * @throws IOException if registration fails
	 */
	private void registerInstallArea() throws IOException {
		String installArea = bundleContext.getProperty(VAL.osgi_install_area);
		
		if (installArea == null || installArea.isEmpty()) {
			throw new IllegalStateException("Install area not configured");
		}
		
		try {
			installArea = BundleLocation.toURI(installArea);
			URI uri = new URI(installArea);
			File rootDir = new File(uri);
			
			if (!rootDir.exists() || !rootDir.isDirectory()) {
				throw new IllegalStateException("Install area does not exist: " + rootDir);
			}
			
			// Register root directory
			registerDirectory(rootDir.toPath());
			
			// Register all subdirectories
			registerSubdirectories(rootDir);
			
			SYS.LOG.info("Registered install area for hot deployment: {}", rootDir.getAbsolutePath());
		} catch (Exception e) {
			throw new IOException("Failed to register install area", e);
		}
	}

	/**
	 * Recursively registers all subdirectories for monitoring.
	 * 
	 * @param directory the directory to scan
	 * @throws IOException if registration fails
	 */
	private void registerSubdirectories(File directory) throws IOException {
		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					registerDirectory(file.toPath());
					registerSubdirectories(file);
				}
			}
		}
	}

	/**
	 * Registers a single directory for monitoring.
	 * 
	 * @param path the directory path
	 * @throws IOException if registration fails
	 */
	private void registerDirectory(Path path) throws IOException {
		WatchKey key = path.register(
			watchService,
			StandardWatchEventKinds.ENTRY_CREATE,
			StandardWatchEventKinds.ENTRY_MODIFY,
			StandardWatchEventKinds.ENTRY_DELETE
		);
		watchKeys.put(key, path);
	}

	/**
	 * Waits for file operations to complete before processing.
	 * This prevents processing incomplete file writes.
	 */
	private void waitForFileSettling() {
		try {
			Thread.sleep(SETTLE_TIME_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Cleanup resources.
	 */
	private void cleanup() {
		try {
			watchService.close();
			watchKeys.clear();
			SYS.LOG.info("Hot deployment watcher stopped");
		} catch (IOException e) {
			SYS.LOG.error("Error closing watch service", e);
		}
	}
}
