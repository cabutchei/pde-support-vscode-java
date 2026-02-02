package com.github.cabutchei.jdtls.pde.buildsupport;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

final class LogHelper {
    private static final String DEBUG_PROPERTY = "pde.support.debug";
    private static final boolean DEBUG = Boolean.getBoolean(DEBUG_PROPERTY);

    private LogHelper() {
    }

    static void debug(String message) {
        if (!DEBUG) {
            return;
        }
        log(IStatus.INFO, message, null);
    }

    static void error(String message, Throwable error) {
        log(IStatus.ERROR, message, error);
    }

    static void warn(String message) {
        log(IStatus.WARNING, message, null);
    }

    private static void log(int severity, String message, Throwable error) {
        Bundle bundle = FrameworkUtil.getBundle(LogHelper.class);
        if (bundle == null) {
            return;
        }
        ILog log = Platform.getLog(bundle);
        if (log == null) {
            return;
        }
        log.log(new Status(severity, bundle.getSymbolicName(), message, error));
    }
}
