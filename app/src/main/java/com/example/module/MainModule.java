package com.example.module;

import android.app.ActivityManager;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public class MainModule extends XposedModule {

    private static final long RAM_MULTIPLIER = 2L;
    private static final long STORAGE_MULTIPLIER = 4L;

    @Override
    public void onSystemServerStarting(
            @NonNull SystemServerStartingParam param) {

        ClassLoader cl = param.getClassLoader();

        log(
                android.util.Log.INFO,
                "FakeRamRom",
                "===== FakeRamRom SystemServer START =====",
                null
        );

        /*
         * =====================================================
         * 1. StorageManagerService
         * =====================================================
         */

        try {

            Class<?> cls = cl.loadClass(
                    "com.android.server.StorageManagerService"
            );

            for (Method method : cls.getDeclaredMethods()) {

                String name = method.getName();

                if (name.equals("getPrimaryStorageSize")) {

                    method.setAccessible(true);

                    hook(method).intercept(chain -> {

                        Object result = chain.proceed();

                        if (result instanceof Long) {

                            long original = (Long) result;

                            long fake = multiply(
                                    original,
                                    STORAGE_MULTIPLIER
                            );

                            log(
                                    android.util.Log.INFO,
                                    "FakeRamRom",
                                    "getPrimaryStorageSize: "
                                            + original
                                            + " -> "
                                            + fake,
                                    null
                            );

                            return fake;
                        }

                        return result;
                    });

                    log(
                            android.util.Log.INFO,
                            "FakeRamRom",
                            "HOOKED getPrimaryStorageSize",
                            null
                    );
                }
            }

        } catch (Throwable e) {

            log(
                    android.util.Log.ERROR,
                    "FakeRamRom",
                    "StorageManagerService ERROR",
                    e
            );
        }


        /*
         * =====================================================
         * 2. ActivityManagerService
         * =====================================================
         */

        try {

            Class<?> cls = cl.loadClass(
                    "com.android.server.am.ActivityManagerService"
            );

            for (Method method : cls.getDeclaredMethods()) {

                String name =
                        method.getName().toLowerCase();

                if (name.contains("memory")
                        || name.contains("meminfo")
                        || name.contains("ram")) {

                    try {

                        method.setAccessible(true);

                        hook(method).intercept(chain -> {

                            Object result = chain.proceed();

                            if (result instanceof ActivityManager.MemoryInfo) {

                                ActivityManager.MemoryInfo info =
                                        (ActivityManager.MemoryInfo) result;

                                info.totalMem =
                                        multiply(
                                                info.totalMem,
                                                RAM_MULTIPLIER
                                        );

                                info.availMem =
                                        multiply(
                                                info.availMem,
                                                RAM_MULTIPLIER
                                        );

                                info.threshold =
                                        multiply(
                                                info.threshold,
                                                RAM_MULTIPLIER
                                        );
                            }

                            return result;
                        });

                        log(
                                android.util.Log.INFO,
                                "FakeRamRom",
                                "RAM METHOD HOOKED: "
                                        + method.getName(),
                                null
                        );

                    } catch (Throwable ignored) {
                    }
                }
            }

        } catch (Throwable e) {

            log(
                    android.util.Log.ERROR,
                    "FakeRamRom",
                    "ActivityManagerService ERROR",
                    e
            );
        }


        log(
                android.util.Log.INFO,
                "FakeRamRom",
                "===== FakeRamRom SystemServer READY =====",
                null
        );
    }


    @Override
    public void onPackageLoaded(
            @NonNull PackageLoadedParam param) {

        /*
         * 暂时不 Hook 普通 App。
         *
         * 第一阶段只验证 System Server。
         */
    }


    @Override
    public void onPackageReady(
            @NonNull PackageReadyParam param) {
    }


    private static long multiply(
            long value,
            long multiplier) {

        if (value <= 0) {
            return value;
        }

        if (value > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }

        return value * multiplier;
    }
}
