/* --------------------------------------------------------------------------
 * Project: CAR OSGi - Test Bundle
 * --------------------------------------------------------------------------
 * Simple test bundle to demonstrate hot deployment functionality
 * -------------------------------------------------------------------------- */

package biz.car.osgi.test;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Simple test bundle activator for hot deployment testing.
 * 
 * <p>This bundle logs messages when it starts and stops, making it easy
 * to verify hot deployment is working correctly.
 *
 * @version 1.0.0 22.01.2026 13:30:00
 */
public class TestBundleActivator implements BundleActivator {

	private static final String BUNDLE_NAME = "CAR OSGi Test Bundle";
	
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("========================================");
		System.out.println(BUNDLE_NAME + " STARTED");
		System.out.println("Bundle ID: " + context.getBundle().getBundleId());
		System.out.println("Bundle Version: " + context.getBundle().getVersion());
		System.out.println("========================================");
		
		// Register a simple service for testing
		TestService service = new TestServiceImpl();
		context.registerService(TestService.class.getName(), service, null);
		
		System.out.println("Test service registered");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("========================================");
		System.out.println(BUNDLE_NAME + " STOPPED");
		System.out.println("Bundle ID: " + context.getBundle().getBundleId());
		System.out.println("========================================");
	}
}

/**
 * Simple test service interface.
 */
interface TestService {
	String getMessage();
}

/**
 * Simple test service implementation.
 */
class TestServiceImpl implements TestService {
	
	@Override
	public String getMessage() {
		return "Hot Deployment is working! Version 1.0";
	}
}
