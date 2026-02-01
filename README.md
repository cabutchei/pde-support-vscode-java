# PDE Support for VS Code Java

This repo contains a VS Code extension that contributes Eclipse PDE and related OSGi bundles to the Java Language Server (JDT LS). It also includes the Tycho build project that produces the custom PDE build-support bundle shipped with the extension. This projects aims to bring to VS Code some of the PDE convenience
found in Eclipse.

## Repository layout

- `extension/` — VS Code extension project.
- `extension/bundles/` — PDE + supporting JARs consumed by JDT LS.
- `pde-build-support/` — Tycho build project for `com.github.cabutchei.jdtls.pde.buildsupport`.

## Typical workflow

1) Build the PDE build-support bundle in `pde-build-support/` using Maven/Tycho.
2) Copy the resulting JAR into `extension/bundles/`.
3) Run `npm run update:java-extensions` from `extension/` to refresh `contributes.javaExtensions`.
4) Package the extension with `npm run package`.

## Notes

- The extension is a runtime add-on for JDT LS and does not add UI or commands.
- For extension-specific details, see `extension/README.md`.
