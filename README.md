# Xposed Module Template (Based on libxposed)

This is a general-purpose Xposed module template built on [libxposed](https://github.com/libxposed/api).

## How to Use

1. **Modify Package Name**: Change `namespace` and `applicationId` in `app/build.gradle` to your own package name.
2. **Rename Package Directory**: Rename the `app/src/main/java/com/example/module` directory to match your package structure.
3. **Update Module Entry Point**: 
   - Implement your logic in `MainModule.java`.
   - Update the class name in `app/src/main/resources/META-INF/xposed/java_init.list` to ensure it points to your `XposedModule` implementation class.
4. **Configure Scope**:
   - List the package names of the apps you want to hook in `app/src/main/resources/META-INF/xposed/scope.list` (one per line).
5. **Set Compilation Parameters**:
   - Modify `compileSdk` and `targetSdkVersion` in `app/build.gradle` as needed.

## Key Components

- `MainModule.java`: The main entry point of the module, inheriting from `XposedModule`.
- `java_init.list`: Tells libxposed which class is the entry point for the module.
- `scope.list`: Defines the scope (apps) where the module will be active.
- `module.prop`: Contains metadata information for the module.

## Notes

- This template uses the `libxposed` API. Please refer to its official documentation for more advanced usage.
- Ensure that `compileOnly` dependencies are correctly configured during development to avoid bundling the Xposed API into your APK.
