package com.github.cabutchei.jdtls.pde.buildsupport;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.osgi.framework.FrameworkUtil;

final class TargetPlatformLoader {
    private static final Object FAMILY = new Object();
    private static final ConcurrentMap<String, Long> LAST_RESOLVED = new ConcurrentHashMap<>();

    private TargetPlatformLoader() {
    }

    static void scheduleLoad(IFile targetFile) {
        if (targetFile == null) {
            return;
        }
        LogHelper.debug("Scheduling target resolution for " + targetFile.getFullPath());
        Job job = new Job("Resolve PDE target platform") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                return resolveTarget(targetFile, monitor);
            }

            @Override
            public boolean belongsTo(Object family) {
                return family == FAMILY;
            }
        };
        job.setSystem(true);
        job.schedule(200);
    }

    static void scheduleLoadLatestInWorkspace() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        if (root == null) {
            LogHelper.debug("Workspace root unavailable; skipping target scan.");
            return;
        }
        TargetFinder finder = new TargetFinder();
        try {
            root.accept(finder, IResource.NONE);
        } catch (CoreException e) {
            LogHelper.error("Failed to scan workspace for target definitions.", e);
            return;
        }
        if (finder.best != null) {
            LogHelper.debug("Found target definition: " + finder.best.getFullPath());
            scheduleLoad(finder.best);
        } else {
            LogHelper.warn("No .target definition found in workspace.");
        }
    }

    private static IStatus resolveTarget(IFile targetFile, IProgressMonitor monitor) {
        try {
            if (!targetFile.exists()) {
                LogHelper.warn("Target no longer exists: " + targetFile.getFullPath());
                return Status.CANCEL_STATUS;
            }
            String key = targetFile.getFullPath().toString();
            long stamp = targetFile.getModificationStamp();
            Long last = LAST_RESOLVED.get(key);
            if (last != null && last.longValue() == stamp) {
                LogHelper.debug("Target unchanged; skipping resolve: " + targetFile.getFullPath());
                return Status.OK_STATUS;
            }

            ITargetPlatformService service = TargetPlatformService.getDefault();
            ITargetHandle handle = service.getTarget(targetFile);
            if (handle == null || !handle.exists()) {
                LogHelper.warn("Target handle missing for " + targetFile.getFullPath());
                return Status.CANCEL_STATUS;
            }
            ITargetDefinition definition = handle.getTargetDefinition();
            if (definition == null) {
                LogHelper.warn("Target definition unavailable for " + targetFile.getFullPath());
                return Status.CANCEL_STATUS;
            }

            LoadTargetDefinitionJob.load(definition, new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    IStatus result = event.getResult();
                    if (result != null && !result.isOK()) {
                        LogHelper.error("Target load failed: " + targetFile.getFullPath() + " -> " + result,
                                result.getException());
                    } else {
                        LogHelper.debug("Target load finished: " + targetFile.getFullPath());
                    }
                    if (result != null && result.isOK()) {
                        LAST_RESOLVED.put(key, stamp);
                    }
                }
            });
            return Status.OK_STATUS;
        } catch (CoreException e) {
            LogHelper.error("Failed to resolve target " + targetFile.getName(), e);
            return new Status(IStatus.ERROR, getBundleId(), "Failed to resolve target " + targetFile.getName(), e);
        } catch (Exception e) {
            LogHelper.error("Unexpected error resolving target " + targetFile.getName(), e);
            return new Status(IStatus.ERROR, getBundleId(), "Unexpected error resolving target " + targetFile.getName(),
                    e);
        }
    }

    private static String getBundleId() {
        return Objects.requireNonNull(FrameworkUtil.getBundle(TargetPlatformLoader.class)).getSymbolicName();
    }

    private static final class TargetFinder implements IResourceProxyVisitor {
        IFile best;
        long bestStamp = Long.MIN_VALUE;

        @Override
        public boolean visit(IResourceProxy proxy) {
            if (proxy.getType() == IResource.FILE && proxy.getName().endsWith(".target")) {
                IFile file = (IFile) proxy.requestResource();
                long stamp = file.getModificationStamp();
                if (stamp > bestStamp) {
                    bestStamp = stamp;
                    best = file;
                }
                return false;
            }
            return true;
        }
    }
}
