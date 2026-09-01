package com.example.module;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * This is the entry point class for the Xposed module.
 * Customization suggestions:
 * 1. Change the package name `com.example.module` to your own.
 * 2. Add your Hook logic in `onSystemServerStarting`, `onPackageLoaded`, or `onPackageReady`.
 */
@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
public class MainModule extends XposedModule {

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        // Add Hook logic for System Server here
        // For example:
        // try {
        //     var classLoader = param.getClassLoader();
        //     var clazz = classLoader.loadClass("com.android.server.wm.WindowManagerService");
        //     var method = clazz.getDeclaredMethod("exampleMethod");
        //     hook(method).intercept(new ExampleHooker());
        // } catch (Throwable t) {
        //     log(android.util.Log.ERROR, "MainModule", "Hook failed", t);
        // }
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        // Called when the default classloader is ready, before AppComponentFactory instantiation.
        // Note: getClassLoader() is NOT available here. Use onPackageReady() for classloader access.
        // if (param.getPackageName().equals("com.target.package")) {
        //     // ...
        // }
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        // Called after AppComponentFactory has created the app classloader.
        // Use param.getClassLoader() here to load and hook target classes.
        // if (param.getPackageName().equals("com.target.package")) {
        //     var classLoader = param.getClassLoader();
        //     // ...
        // }
    }

    /**
     * This is a simple Hooker example using the OkHttp-style interceptor chain.
     */
    private static class ExampleHooker implements XposedInterface.Hooker {
        @Override
        public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
            // Logic to execute before method execution

            // Call the next interceptor in the chain (or the original method)
            Object result = chain.proceed();

            // Logic to execute after method execution

            return result;
        }
    }
}
