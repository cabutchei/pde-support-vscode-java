# PDE Build Support

This project builds the `com.github.cabutchei.jdtls.pde.buildsupport` Eclipse plugin used by the VS Code extension. It is a Tycho-based build that also defines a target platform module. The plugin makes a contribution to the `org.eclipse.jdt.ls.core.buildSupport` extension point, so that the jdt server will auto-sync with
pde build files, like `MANIFEST.MF` and `.target` files.

## Modules

- `com.github.cabutchei.jdtls.pde.buildsupport/` — the Eclipse plugin project.
- `targetplatform/` — the target platform definition for Tycho.

## Requirements

- Java 17
- Maven

## Build

From the repository root:

```
mvn -f pde-build-support/pom.xml verify
```

The built plugin JAR will be under `pde-build-support/com.github.cabutchei.jdtls.pde.buildsupport/target/`.

## Using the output in the extension

1) Copy the built JAR into `extension/bundles/`.
2) Run `npm run update:java-extensions` from `extension/`.
3) Package the extension as usual.
