package com.github.cabutchei.jdtls.pde.buildsupport;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.ls.core.internal.managers.IBuildSupport;

public class ManifestAndTargetBuildSupport implements IBuildSupport {
	private static final List<String> WATCH_PATTERNS = List.of(
			"**/META-INF/MANIFEST.MF",
			"**/*.target"
	);

	@Override
	public boolean applies(IProject project) {
		return false;
	}

	@Override
	public boolean isBuildFile(IResource resource) {
		if (resource == null) {
			return false;
		}
		String name = resource.getName();
		if (name != null && name.endsWith(".target")) {
			return true;
		}
		if ("MANIFEST.MF".equals(name)) {
			IPath path = resource.getProjectRelativePath();
			return path != null
					&& path.segmentCount() >= 2
					&& "META-INF".equals(path.segment(path.segmentCount() - 2));
		}
		return false;
	}

	@Override
	public boolean isBuildLikeFileName(String fileName) {
		if (fileName == null) {
			return false;
		}
		return "MANIFEST.MF".equals(fileName) || fileName.endsWith(".target");
	}

	@Override
	public List<String> getWatchPatterns() {
		return WATCH_PATTERNS;
	}
}
