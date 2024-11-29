/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.launch;

import biz.car.osgi.bundle.BND;

/**
 * Entry points for starting and stopping the OSGi framework.
 *
 * @version 1.0.0 11.10.2024 08:58:34
 */
public class Main {

	private static final String OSGI_MAIN = BND.OSGI_MAIN;
	
	/**
	 * Creates a default <code>Main</code> instance.
	 */
	private Main() {
		super();
	}

	private static Launcher myLauncher;

	/**
	 * Starts the OSGi framework launcher.
	 * 
	 * @param anArgList the list of user arguments.
	 */
	public static void main(String[] anArgList) {
		// Create the launcher thread
		myLauncher = new Launcher(anArgList);
		Thread l_thread = new Thread(myLauncher, OSGI_MAIN);

		// Start the launcher in its own thread
		l_thread.start();
	}

	/**
	 * Performs a shutdown of the OSGi framework.
	 *
	 * @param anArgList the list of user arguments.
	 */
	public static void stop(String[] anArgList) {
		// Stop the framework
		myLauncher.stop(anArgList);
	}
}
