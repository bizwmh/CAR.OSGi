/* --------------------------------------------------------------------------
 * Project: CAR OSGi
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.deploy;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

import biz.car.SYS;
import biz.car.osgi.bundle.MSG;

/**
 * Uses the Java WatchService to implement hot deployment for the OSGi install
 * area.
 *
 * @version 2.0.0 19.01.2026 14:17:06
 */
public class DirectoryWatcher implements Runnable {

	private Path myPath;
	private boolean isRunning;

	/**
	 * Creates a default <code>DirectoryWatcher</code> instance.
	 * 
	 * @param aPath the name of the directory to watch
	 */
	public DirectoryWatcher(String aPath) {
		super();

		myPath = Paths.get(aPath);
		isRunning = false;
	}

	@Override
	public void run() {
		try (WatchService l_ws = FileSystems.getDefault().newWatchService()) {
			isRunning = true;

			myPath.register(l_ws, 
					StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_MODIFY,
					StandardWatchEventKinds.ENTRY_DELETE);
			SYS.LOG.info(MSG.WATCHER_STARTED, myPath.toString());

			WatchKey key;
			while (isRunning && (key = l_ws.take()) != null) {
				key.pollEvents();
				handleEvents();
				
				if(!key.reset()) {
					break;
				}
			}
			SYS.LOG.info(MSG.WATCHER_STOPPED, myPath.toString());
		} catch (Exception anEx) {
			SYS.LOG.error(anEx);
		}
	}

	/**
	 * TODO handleEvents
	 * @throws InterruptedException 
	 */
	private void handleEvents() throws InterruptedException {
		TimeUnit.MILLISECONDS.sleep(500);	
	}

	public void stop() {
		isRunning = false;
	}
}
