import fs from 'node:fs';
import path from 'node:path';

const repoRoot = process.cwd();
const bundlesDir = path.join(repoRoot, 'bundles');
const packageJsonPath = path.join(repoRoot, 'package.json');

const entries = fs
  .readdirSync(bundlesDir, { withFileTypes: true })
  .filter((dirent) => dirent.isFile() && !dirent.name.startsWith('.'))
  .map((dirent) => `bundles/${dirent.name}`)
  .sort((a, b) => a.localeCompare(b));

const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
packageJson.contributes = packageJson.contributes ?? {};
packageJson.contributes.javaExtensions = entries;

const output = JSON.stringify(packageJson, null, 2) + '\n';
fs.writeFileSync(packageJsonPath, output, 'utf8');

console.log(`Updated javaExtensions with ${entries.length} bundle(s).`);
