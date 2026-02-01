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
import biz.car.osgi.deploy.Deployer;
import biz.car.osgi.deploy.InstallArea;
import biz.car.osgi.framework.XFramework;

/**
 * Initializes and starts the OSGi framework.
 *
 * @version 2.0.0 08.01.2026 11:00:50
 */
public class Launcher implements Runnable {

	private static String DEF_PROPS = _default + _properties;
	private static String FWK = VAL.framework;
	private static String FWK_PROPS = FWK + _properties;
	private static String SYM = VAL.system;
	private static String SYM_PROPS = SYM + _properties;

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

			// Build the framework configuration
			Map<String, String> l_osgiConf = new HashMap<>();

			// Publish the diagnose logger
			String l_val = VAL.framework_logger_diagnose;
			String l_name = ACS.APP.getString(VAL.diagnoseLogger);

			l_osgiConf.put(l_val, l_name);

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

			// publish framework data area key
			String l_key = VAL.framework_data_area;
			String l_value = l_fwkProps.getString(l_key);
			l_osgiConf.put(l_key, l_value);
			
			// create an instance of the OSGi framework and initialize it
			XFramework.init(l_osgiConf);

			// process the OSGi install area by provisioning 
			// jar file changes to OSGi bundle storage
			Deployer l_deployer = new Deployer();
			
			l_deployer.processInstallArea();

			// Start hot deployment if enabled
			boolean l_enabled = l_fwkProps.getBoolean(VAL.framework_hotdeploy_enabled);
			
			if (l_enabled) {
				InstallArea.watcher().start();
			}
			// start the framework and wait for stop to exit the VM
			XFramework.start();
		} catch (XRuntimeException anEx) {
			SYS.LOG.error(MSG.FWK_ABENDED);
		} catch (Exception anEx) {
			SYS.LOG.error(anEx);
			SYS.LOG.error(MSG.FWK_ABENDED);
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
		// Stop the framework
		XFramework.stop();
		SYS.LOG.info(MSG.FWK_STOPPED);
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
