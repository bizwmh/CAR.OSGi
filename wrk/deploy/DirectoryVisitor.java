/* --------------------------------------------------------------------------
 * Project: XXX
 * --------------------------------------------------------------------------
 * Use of this software is subject to license terms. All Rights Reserved. 
 * -------------------------------------------------------------------------- */

package biz.car.osgi.deploy;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Start the directory watch service for each folder of the install area.
 *
 * @version 2.0.0 19.01.2026 16:26:25
 */
public class DirectoryVisitor extends SimpleFileVisitor<Path> {

	private Path start;

	/**
	 * Creates a default <code>DirectoryVisitor</code> instance.
	 * 
	 * @param l_path
	 */
	public DirectoryVisitor(Path aPath) {
		super();

		start = aPath;
	}

	/**
	 * Invoked for a directory before entries in the directory are visited.
	 */
	@Override
	public FileVisitResult preVisitDirectory(Path aPath, BasicFileAttributes anAttrs)
			throws IOException {
		Objects.requireNonNull(aPath);
		Objects.requireNonNull(anAttrs);

		if (aPath.equals(start)) {
			return FileVisitResult.CONTINUE;
		} else {
			return FileVisitResult.SKIP_SUBTREE;
		}
	}
}
