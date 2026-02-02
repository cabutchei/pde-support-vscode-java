package com.github.cabutchei.jdtls.pde.buildsupport;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public final class Activator implements BundleActivator, IResourceChangeListener {
    private static final Object SCAN_FAMILY = new Object();

    @Override
    public void start(BundleContext context) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
        LogHelper.debug("PDE build support activated; workspace=" + workspace.getRoot().getLocation());
        scheduleWorkspaceScan();
    }

    @Override
    public void stop(BundleContext context) {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        LogHelper.debug("PDE build support deactivated.");
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta == null) {
            return;
        }
        TargetDeltaFinder finder = new TargetDeltaFinder();
        try {
            delta.accept(finder);
        } catch (CoreException e) {
            LogHelper.error("Failed to process resource delta for target changes.", e);
            return;
        }
        if (finder.changed != null) {
            LogHelper.debug("Target changed: " + finder.changed.getFullPath());
            TargetPlatformLoader.scheduleLoad(finder.changed);
        } else if (finder.sawRemoval) {
            LogHelper.debug("Target removed; scanning workspace.");
            scheduleWorkspaceScan();
        }
    }

    private static void scheduleWorkspaceScan() {
        Job.getJobManager().cancel(SCAN_FAMILY);
        Job job = new Job("Scan PDE target definitions") {
            @Override
            protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                LogHelper.debug("Scanning workspace for .target definitions.");
                TargetPlatformLoader.scheduleLoadLatestInWorkspace();
                return org.eclipse.core.runtime.Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(Object family) {
                return family == SCAN_FAMILY;
            }
        };
        job.setSystem(true);
        job.schedule(300);
    }

    private static final class TargetDeltaFinder implements IResourceDeltaVisitor {
        IFile changed;
        long bestStamp = Long.MIN_VALUE;
        boolean sawRemoval;

        @Override
        public boolean visit(IResourceDelta delta) {
            if (delta.getResource() instanceof IFile file && file.getName().endsWith(".target")) {
                int kind = delta.getKind();
                if (kind == IResourceDelta.REMOVED) {
                    sawRemoval = true;
                } else if (kind == IResourceDelta.ADDED || kind == IResourceDelta.CHANGED) {
                    long stamp = file.getModificationStamp();
                    if (stamp > bestStamp) {
                        bestStamp = stamp;
                        changed = file;
                    }
                }
                return false;
            }
            return true;
        }
    }
}
