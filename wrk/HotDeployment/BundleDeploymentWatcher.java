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
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import biz.car.CAR;
import biz.car.SYS;
import biz.car.osgi.bundle.VAL;
import biz.car.osgi.framework.XFramework;

/**
 * Watches the OSGi installation area for file changes and automatically
 * deploys, updates, or removes bundles.
 * 
 * <p>This class monitors the install area directory and all subdirectories
 * for JAR file modifications, additions, and deletions, then performs the
 * appropriate OSGi bundle operations.
 *
 * @version 2.0.0 22.01.2026 13:00:00
 */
public class BundleDeploymentWatcher implements Runnable, CAR {

	private static final long SETTLE_TIME_MS = 500; // Wait time for file operations to complete
	
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
				
				// Process all pending events
				for (WatchEvent<?> event : key.pollEvents()) {
					processEvent(event, directory);
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
	 * Registers the installation area and all subdirectories for monitoring.
	 * 
	 * @throws IOException if registration fails
	 */
	private void registerInstallArea() throws IOException {
		String installArea = bundleContext.getProperty(VAL.osgi_install_area);
		
		if (installArea == null || installArea.isEmpty()) {
			throw new IllegalStateException("Install area not configured");
		}
		
		// Convert to URI and then to File
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
	 * Processes a file system event.
	 * 
	 * @param event the watch event
	 * @param directory the directory where the event occurred
	 */
	private void processEvent(WatchEvent<?> event, Path directory) {
		WatchEvent.Kind<?> kind = event.kind();
		
		// Ignore overflow events
		if (kind == StandardWatchEventKinds.OVERFLOW) {
			return;
		}
		
		// Get the file name
		@SuppressWarnings("unchecked")
		WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
		Path fileName = pathEvent.context();
		Path fullPath = directory.resolve(fileName);
		File file = fullPath.toFile();
		
		// Only process JAR files
		if (!file.getName().endsWith(_jar)) {
			// Check if it's a new directory that needs to be registered
			if (kind == StandardWatchEventKinds.ENTRY_CREATE && file.isDirectory()) {
				try {
					registerDirectory(file.toPath());
					registerSubdirectories(file);
					SYS.LOG.info("Registered new directory for watching: {}", file.getName());
				} catch (IOException e) {
					SYS.LOG.error("Failed to register new directory: {}", file.getName(), e);
				}
			}
			return;
		}
		
		// Wait for file operations to settle
		waitForFileSettling(file);
		
		// Process based on event type
		String location = file.toURI().toString();
		
		if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
			handleBundleInstall(location, file);
		} else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
			handleBundleUpdate(location, file);
		} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
			handleBundleUninstall(location);
		}
	}

	/**
	 * Waits for file operations to complete before processing.
	 * This prevents processing incomplete file writes.
	 * 
	 * @param file the file to check
	 */
	private void waitForFileSettling(File file) {
		try {
			Thread.sleep(SETTLE_TIME_MS);
			
			// Additional check: verify file is readable and stable
			long lastSize = -1;
			long currentSize = file.length();
			int attempts = 0;
			
			while (lastSize != currentSize && attempts < 5) {
				Thread.sleep(100);
				lastSize = currentSize;
				currentSize = file.length();
				attempts++;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Handles bundle installation.
	 * 
	 * @param location the bundle location
	 * @param file the JAR file
	 */
	private void handleBundleInstall(String location, File file) {
		try {
			// Check if bundle already exists
			Bundle existingBundle = bundleContext.getBundle(location);
			
			if (existingBundle != null) {
				// Bundle exists, update instead
				handleBundleUpdate(location, file);
				return;
			}
			
			// Install the bundle
			Bundle bundle = bundleContext.installBundle(location);
			
			// Set start level if needed (extracted from directory structure)
			setBundleStartLevel(bundle, file);
			
			// Start the bundle if it's not a fragment
			if (!isFragment(bundle)) {
				bundle.start(Bundle.START_ACTIVATION_POLICY);
			}
			
			SYS.LOG.info("Hot deployed bundle: {} [{}]", 
				bundle.getSymbolicName(), bundle.getBundleId());
			
		} catch (BundleException e) {
			SYS.LOG.error("Failed to install bundle from: {}", location, e);
		}
	}

	/**
	 * Handles bundle updates.
	 * 
	 * @param location the bundle location
	 * @param file the JAR file
	 */
	private void handleBundleUpdate(String location, File file) {
		try {
			Bundle bundle = bundleContext.getBundle(location);
			
			if (bundle == null) {
				// Bundle doesn't exist, install instead
				handleBundleInstall(location, file);
				return;
			}
			
			// Check if file is actually newer
			long bundleLastModified = bundle.getLastModified();
			long fileLastModified = file.lastModified();
			
			if (fileLastModified > bundleLastModified) {
				bundle.update();
				SYS.LOG.info("Updated bundle: {} [{}]", 
					bundle.getSymbolicName(), bundle.getBundleId());
			}
			
		} catch (BundleException e) {
			SYS.LOG.error("Failed to update bundle from: {}", location, e);
		}
	}

	/**
	 * Handles bundle uninstallation.
	 * 
	 * @param location the bundle location
	 */
	private void handleBundleUninstall(String location) {
		try {
			Bundle bundle = bundleContext.getBundle(location);
			
			if (bundle != null && bundle.getBundleId() != 0) { // Don't uninstall system bundle
				String symbolicName = bundle.getSymbolicName();
				long bundleId = bundle.getBundleId();
				
				bundle.uninstall();
				
				SYS.LOG.info("Uninstalled bundle: {} [{}]", symbolicName, bundleId);
			}
			
		} catch (BundleException e) {
			SYS.LOG.error("Failed to uninstall bundle from: {}", location, e);
		}
	}

	/**
	 * Sets the bundle start level based on directory structure.
	 * Directory names like /01, /02, etc. indicate the start level.
	 * 
	 * @param bundle the bundle
	 * @param file the JAR file
	 */
	private void setBundleStartLevel(Bundle bundle, File file) {
		try {
			String path = file.getAbsolutePath();
			
			// Look for pattern /nn/ in the path
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[\\\\/](\\d{2})[\\\\/]");
			java.util.regex.Matcher matcher = pattern.matcher(path);
			
			if (matcher.find()) {
				int startLevel = Integer.parseInt(matcher.group(1));
				org.osgi.framework.startlevel.BundleStartLevel bsl = 
					bundle.adapt(org.osgi.framework.startlevel.BundleStartLevel.class);
				
				if (bsl != null) {
					bsl.setStartLevel(startLevel);
				}
			}
		} catch (Exception e) {
			// Ignore errors, use default start level
			SYS.LOG.debug("Could not set start level for bundle: {}", bundle.getSymbolicName());
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
