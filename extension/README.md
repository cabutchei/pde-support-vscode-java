# PDE Support for VS Code Java

This extension contributes additional Eclipse/PDE bundles to the Java Language Server (JDT LS) so Eclipse plugin projects are recognized and get standard Java features like build, indexing, and syntax highlighting.

## What it does

- Ships PDE + supporting OSGi bundles under `bundles/`.
- Registers those JARs via the `javaExtensions` contribution point.
- No commands or UI — it is a runtime add-on for JDT LS.

## Usage

1) Install the VS Code Java extensions (e.g., the Java Extension Pack).
2) Install this extension (from a VSIX or marketplace).
3) Open an Eclipse plugin project (e.g., `MANIFEST.MF` / `plugin.xml` present).
4) If JDT LS is already running, reload the window to ensure the new bundles are picked up.

## Development

1) Install dependencies:

```
npm install
```

2) Compile:

```
npm run compile
```

3) Package (creates a VSIX):

```
npm run package
```

## Updating bundles

- Drop new JARs into `bundles/` (flat folder, no subdirectories).
- Run:

```
npm run update:java-extensions
```

- The script re-sorts and rewrites `package.json` so `contributes.javaExtensions` matches the folder contents.

## Limitations

- This does not add PDE UI tooling, target platform management, or launch configuration support.
- It only supplies bundles to JDT LS so PDE-style projects can load and compile.
