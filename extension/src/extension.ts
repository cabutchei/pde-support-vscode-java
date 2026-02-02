import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';

export function activate(context: vscode.ExtensionContext) {
  const output = vscode.window.createOutputChannel('PDE Target Diagnostics');
  context.subscriptions.push(
    output,
    vscode.commands.registerCommand('pdeSupport.targetDiagnostics', async () => {
      output.clear();
      output.show(true);

      const targets = await vscode.workspace.findFiles('**/*.target', '**/{node_modules,.git,.metadata}/**', 100);
      if (targets.length === 0) {
        output.appendLine('No .target files found in the workspace.');
        return;
      }

      let latest = targets[0];
      let latestMtime = (await vscode.workspace.fs.stat(latest)).mtime;
      for (const target of targets.slice(1)) {
        const stat = await vscode.workspace.fs.stat(target);
        if (stat.mtime > latestMtime) {
          latest = target;
          latestMtime = stat.mtime;
        }
      }

      try {
        const bytes = await vscode.workspace.fs.readFile(latest);
        await vscode.workspace.fs.writeFile(latest, bytes);
        output.appendLine(`Touched target: ${latest.fsPath}`);
      } catch (error) {
        output.appendLine(`Failed to touch target: ${latest.fsPath}`);
        output.appendLine(String(error));
      }

      const workspaceStorageRoot = getWorkspaceStorageRoot(context);
      if (!workspaceStorageRoot) {
        output.appendLine('Could not locate VS Code workspaceStorage directory.');
        return;
      }

      const pools = await findBundlePools(workspaceStorageRoot);
      if (pools.length === 0) {
        output.appendLine('No PDE bundle_pool found under workspaceStorage.');
        return;
      }

      output.appendLine(`Found ${pools.length} bundle_pool location(s):`);
      for (const pool of pools) {
        const stats = await getBundlePoolStats(pool);
        output.appendLine(`- ${pool}`);
        output.appendLine(`  plugins: ${stats.count}, size: ${formatBytes(stats.size)}`);
      }
    })
  );
}

export function deactivate() {
  // no-op
}

function getWorkspaceStorageRoot(context: vscode.ExtensionContext): string | undefined {
  const globalStorage = context.globalStorageUri?.fsPath;
  if (globalStorage) {
    const userDir = path.resolve(globalStorage, '..', '..');
    const candidate = path.join(userDir, 'workspaceStorage');
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }

  const home = os.homedir();
  if (process.platform === 'darwin') {
    const candidate = path.join(home, 'Library', 'Application Support', 'Code', 'User', 'workspaceStorage');
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  } else if (process.platform === 'win32') {
    const appData = process.env.APPDATA;
    if (appData) {
      const candidate = path.join(appData, 'Code', 'User', 'workspaceStorage');
      if (fs.existsSync(candidate)) {
        return candidate;
      }
    }
  } else {
    const candidate = path.join(home, '.config', 'Code', 'User', 'workspaceStorage');
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }

  return undefined;
}

async function findBundlePools(workspaceStorageRoot: string): Promise<string[]> {
  const pools: string[] = [];
  const entries = await fs.promises.readdir(workspaceStorageRoot, { withFileTypes: true });
  for (const entry of entries) {
    if (!entry.isDirectory()) {
      continue;
    }
    const pool = path.join(
      workspaceStorageRoot,
      entry.name,
      'redhat.java',
      'jdt_ws',
      '.metadata',
      '.plugins',
      'org.eclipse.pde.core',
      '.bundle_pool'
    );
    if (fs.existsSync(pool)) {
      pools.push(pool);
    }
  }
  return pools;
}

async function getBundlePoolStats(poolPath: string): Promise<{ count: number; size: number }> {
  const pluginsDir = path.join(poolPath, 'plugins');
  if (!fs.existsSync(pluginsDir)) {
    return { count: 0, size: 0 };
  }
  const entries = await fs.promises.readdir(pluginsDir, { withFileTypes: true });
  let count = 0;
  let size = 0;
  for (const entry of entries) {
    if (!entry.isFile() || !entry.name.endsWith('.jar')) {
      continue;
    }
    count += 1;
    const stat = await fs.promises.stat(path.join(pluginsDir, entry.name));
    size += stat.size;
  }
  return { count, size };
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const units = ['KB', 'MB', 'GB', 'TB'];
  let value = bytes / 1024;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value.toFixed(1)} ${units[unitIndex]}`;
}
