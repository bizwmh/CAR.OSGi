/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi;

import static biz.car.bundle.VAL._default;
import static biz.car.bundle.VAL._properties;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import biz.car.SYS;
import biz.car.XRuntimeException;
import biz.car.config.ACS;
import biz.car.config.XConfig;
import biz.car.osgi.bundle.BND;
import biz.car.osgi.bundle.KEY;
import biz.car.osgi.bundle.MSG;
import biz.car.osgi.bundle.VAL;
import biz.car.osgi.deploy.BundleDeploymentWatcher;
import biz.car.osgi.deploy.Deployer;
import biz.car.osgi.framework.XFramework;

/**
 * Initializes and starts the OSGi framework with hot deployment support.
 *
 * @version 2.1.0 22.01.2026 13:00:00
 */
public class Launcher implements Runnable {

	private static String DEF_PROPS = _default + _properties;
	private static String FWK = VAL.framework;
	private static String FWK_PROPS = FWK + _properties;
	private static String SYM = VAL.system;
	private static String SYM_PROPS = SYM + _properties;

	private BundleDeploymentWatcher hotDeploymentWatcher;
	private Thread watcherThread;

	/**
	 * Creates a default <code>Launcher</code> instance.
	 */
	public Launcher() {
		super();
	}

	/**
	 * This method performs the main task of constructing an OSGi framework instance
	 * and starting its execution. It
	 * <ul>
	 * <li>loads the configuration files
	 * <li>creates a framework instance
	 * <li>initializes the framework with runtime options from the configuration
	 * <li>processes the framework's bundle area
	 * <li>starts hot deployment monitoring (if enabled)
	 * <li>starts the execution of the framework
	 * </ul>
	 */
	@Override
	public void run() {
		try {
			// load system properties
			Config l_sysProps = loadProperties(SYM);

			SYS.addProperties(l_sysProps);
			SYS.LOG.info(MSG.PROPERTIES_LOADED, SYM_PROPS);

			// load framework properties
			Config l_fwkProps = loadProperties(FWK);

			SYS.LOG.info(MSG.PROPERTIES_LOADED, FWK_PROPS);

			// process the framework data area
			String l_da = l_fwkProps.getString(VAL.framework_data_area);
			File l_ws = new File(l_da);

			l_ws.mkdirs();

			// register a shutdown hook to make sure the framework is
			// cleanly shutdown when the VM exits
			ShutdownHook l_hook = new ShutdownHook();
			Thread l_thread = new Thread(l_hook, BND.SHUTDOWN_HOOK);

			Runtime.getRuntime().addShutdownHook(l_thread);

			// Publish the diagnose logger
			Map<String, String> l_osgiConf = new HashMap<>();
			String l_val = VAL.framework_logger_diagnose;
			String l_name = ACS.APP.getString(VAL.diagnoseLogger);

			l_osgiConf.put(l_val, l_name);

			// Build the framework configuration
			// Converts the given framework configuration to a map.
			// The map then contains only keys as required by the OSGi framework
			// implementation or framework related properties
			XConfig l_fwkKeys = () -> KEY.conf;

			l_fwkProps.entrySet().stream()
					.forEach(entry -> {
						String l_key = entry.getKey();
						Object l_value = entry.getValue().unwrapped();
						l_key = l_fwkKeys.getString(l_key, l_key);

						l_osgiConf.put(l_key, l_value.toString());
					});

			// create an instance of the OSGi framework and initialize it
			XFramework.init(l_osgiConf);

			// process the OSGi install area by provisioning 
			// jar file changes to OSGi bundle storage
			Deployer l_deployer = new Deployer();
			
			l_deployer.processInstallArea();

			// Start hot deployment watcher if enabled
			startHotDeployment(l_fwkProps);

			// start the framework and wait for stop to exit the VM
			XFramework.start();
		} catch (XRuntimeException anEx) {
			SYS.LOG.error(MSG.FWK_ABENDED);
		} catch (Exception anEx) {
			SYS.LOG.error(anEx);
			SYS.LOG.error(MSG.FWK_ABENDED);
		} finally {
			// Stop hot deployment watcher
			stopHotDeployment();
		}
		// Shut down Java VM
		System.exit(0);
	}

	/**
	 * Performs a shutdown of the OSGi framework.
	 *
	 * @param ignoredArgList the list of user arguments.
	 */
	public void stop(String[] ignoredArgList) {
		// Stop hot deployment first
		stopHotDeployment();
		
		// Stop the framework
		XFramework.stop();
		SYS.LOG.info(MSG.FWK_STOPPED);
	}

	/**
	 * Starts the hot deployment watcher if enabled in configuration.
	 * 
	 * @param fwkProps the framework properties
	 */
	private void startHotDeployment(Config fwkProps) {
		try {
			// Check if hot deployment is enabled (default: true)
			boolean hotDeployEnabled = true;
			
			if (fwkProps.hasPath("framework.hotdeploy.enabled")) {
				hotDeployEnabled = fwkProps.getBoolean("framework.hotdeploy.enabled");
			}
			
			if (!hotDeployEnabled) {
				SYS.LOG.info("Hot deployment is disabled");
				return;
			}
			
			// Create and start the watcher
			hotDeploymentWatcher = new BundleDeploymentWatcher();
			watcherThread = new Thread(hotDeploymentWatcher, "BundleDeploymentWatcher");
			watcherThread.setDaemon(true); // Daemon thread so it doesn't prevent JVM shutdown
			watcherThread.start();
			
			SYS.LOG.info("Hot deployment enabled");
			
		} catch (Exception e) {
			SYS.LOG.error("Failed to start hot deployment watcher", e);
			// Continue without hot deployment
		}
	}

	/**
	 * Stops the hot deployment watcher.
	 */
	private void stopHotDeployment() {
		if (hotDeploymentWatcher != null) {
			try {
				hotDeploymentWatcher.stop();
				
				// Wait for watcher thread to finish (with timeout)
				if (watcherThread != null && watcherThread.isAlive()) {
					watcherThread.join(5000); // Wait max 5 seconds
				}
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				SYS.LOG.warn("Interrupted while stopping hot deployment watcher");
			} catch (Exception e) {
				SYS.LOG.error("Error stopping hot deployment watcher", e);
			} finally {
				hotDeploymentWatcher = null;
				watcherThread = null;
			}
		}
	}

	/**
	 * Loads the configuration for the given base name of a property file. Base name
	 * may be 'system' or 'framework'.
	 *
	 * @param aName the base name of the properties file
	 * @return the configuration for the given base name
	 */
	private Config loadProperties(String aName) {
		String l_resource = aName + DEF_PROPS;
		Config l_ret = ConfigFactory.parseResources(l_resource);
		l_resource = aName + _properties;
		Optional<Config> l_conf = ACS.parseResource(l_resource);

		if (l_conf.isEmpty()) {
			File l_file = new File(l_resource);

			if (l_file.isFile()) {
				l_conf = Optional.of(ConfigFactory.parseFile(l_file));
			}
		}
		if (l_conf.isPresent()) {
			l_ret = l_conf.get().withFallback(l_ret);
		}
		return l_ret;
	}
}
