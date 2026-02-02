package com.github.cabutchei.jdtls.pde.buildsupport;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ls.core.internal.managers.IBuildSupport;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager.CHANGE_TYPE;

public class ManifestAndTargetBuildSupport implements IBuildSupport {
	private static final List<String> WATCH_PATTERNS = List.of(
			"**/META-INF/MANIFEST.MF",
			"**/*.target"
	);

	@Override
	public boolean applies(IProject project) {
		return true;
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
	public boolean fileChanged(IResource resource, CHANGE_TYPE changeType, IProgressMonitor monitor)
			throws CoreException {
		if (resource instanceof IFile file && file.getName().endsWith(".target")) {
			TargetPlatformLoader.scheduleLoad(file);
		}
		return false;
	}

	@Override
	public void update(IProject project, IProgressMonitor monitor) throws CoreException {
		if (project == null || !project.isAccessible()) {
			return;
		}
		IFile targetFile = findFirstTargetFile(project);
		if (targetFile != null) {
			TargetPlatformLoader.scheduleLoad(targetFile);
		}
	}

	@Override
	public List<String> getWatchPatterns() {
		return WATCH_PATTERNS;
	}

	private static IFile findFirstTargetFile(IProject project) throws CoreException {
		TargetFinder finder = new TargetFinder();
		project.accept(finder, IResource.NONE);
		return finder.result;
	}

	private static final class TargetFinder implements IResourceProxyVisitor {
		IFile result;

		@Override
		public boolean visit(IResourceProxy proxy) {
			if (result != null) {
				return false;
			}
			if (proxy.getType() == IResource.FILE && proxy.getName().endsWith(".target")) {
				result = (IFile) proxy.requestResource();
				return false;
			}
			return true;
		}
	}
}
