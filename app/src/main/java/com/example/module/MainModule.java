package com.example.module;

import android.app.ActivityManager;
import android.os.StatFs;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public class MainModule extends XposedModule {

    // ===== 实验倍率 =====
    // 真实 2GB RAM -> 显示约 4GB
    private static final long RAM_MULTIPLIER = 2L;

    // 真实 32GB ROM -> 显示约 128GB
    private static final long STORAGE_MULTIPLIER = 4L;


    // ============================================================
    // System Server
    // ============================================================

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {

        ClassLoader cl = param.getClassLoader();

        log(android.util.Log.INFO,
                "FakeRamRom",
                "========== FakeRamRom START ==========",
                null);

        // --------------------------------------------------------
        // 1. StorageManagerService
        // --------------------------------------------------------

        try {
            Class<?> cls = cl.loadClass(
                    "com.android.server.StorageManagerService"
            );

            hookMethodsContaining(cls, "getPrimaryStorageSize");

            log(android.util.Log.INFO,
                    "FakeRamRom",
                    "StorageManagerService loaded",
                    null);

        } catch (Throwable e) {
            log(android.util.Log.ERROR,
                    "FakeRamRom",
                    "StorageManagerService hook failed",
                    e);
        }


        // --------------------------------------------------------
        // 2. ActivityManagerService
        // --------------------------------------------------------

        try {
            Class<?> cls = cl.loadClass(
                    "com.android.server.am.ActivityManagerService"
            );

            hookMemoryMethods(cls);

            log(android.util.Log.INFO,
                    "FakeRamRom",
                    "ActivityManagerService loaded",
                    null);

        } catch (Throwable e) {
            log(android.util.Log.ERROR,
                    "FakeRamRom",
                    "ActivityManagerService hook failed",
                    e);
        }


        // --------------------------------------------------------
        // 3. ActivityManagerService.MemoryInfo
        // --------------------------------------------------------

        try {
            Class<?> cls = cl.loadClass(
                    "com.android.server.am.ActivityManagerService"
            );

            for (Method m : cls.getDeclaredMethods()) {

                String name = m.getName();

                if (name.toLowerCase().contains("memory")
                        || name.toLowerCase().contains("meminfo")) {

                    try {
                        m.setAccessible(true);

                        hook(m).intercept(chain -> {

                            Object result = chain.proceed();

                            modifyMemoryObject(result);

                            return result;
                        });

                        log(android.util.Log.INFO,
                                "FakeRamRom",
                                "RAM method hooked: " + name,
                                null);

                    } catch (Throwable ignored) {
                    }
                }
            }

        } catch (Throwable e) {
            log(android.util.Log.ERROR,
                    "FakeRamRom",
                    "RAM scan failed",
                    e);
        }


        log(android.util.Log.INFO,
                "FakeRamRom",
                "========== FakeRamRom READY ==========",
                null);
    }


    // ============================================================
    // 普通 App
    // ============================================================

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {

        try {

            ClassLoader cl = param.getClassLoader();

            // ----------------------------------------------------
            // StatFs
            // ----------------------------------------------------

            hookStatFs(cl);

            // ----------------------------------------------------
            // File
            // ----------------------------------------------------

            hookFile(cl);

            // ----------------------------------------------------
            // ActivityManager
            // ----------------------------------------------------

            hookActivityManager(cl);

        } catch (Throwable e) {

            log(android.util.Log.ERROR,
                    "FakeRamRom",
                    "App side hook failed: "
                            + param.getPackageName(),
                    e);
        }
    }


    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
    }


    // ============================================================
    // RAM
    // ============================================================

    private void hookMemoryMethods(Class<?> cls) {

        for (Method m : cls.getDeclaredMethods()) {

            String name = m.getName().toLowerCase();

            if (name.contains("memory")
                    || name.contains("meminfo")
                    || name.contains("ram")) {

                try {

                    m.setAccessible(true);

                    hook(m).intercept(chain -> {

                        Object result = chain.proceed();

                        modifyMemoryObject(result);

                        return result;
                    });

                    log(android.util.Log.INFO,
                            "FakeRamRom",
                            "RAM hook: " + m.getName(),
                            null);

                } catch (Throwable ignored) {
                }
            }
        }
    }


    private void hookActivityManager(ClassLoader cl) {

        try {

            Class<?> cls = cl.loadClass(
                    "android.app.ActivityManager"
            );

            Method m = cls.getDeclaredMethod(
                    "getMemoryInfo",
                    ActivityManager.MemoryInfo.class
            );

            m.setAccessible(true);

            hook(m).intercept(chain -> {

                Object result = chain.proceed();

                Object[] args = chain.getArgs();

                if (args != null && args.length > 0) {

                    modifyMemoryObject(args[0]);
                }

                return result;
            });

            log(android.util.Log.INFO,
                    "FakeRamRom",
                    "ActivityManager.getMemoryInfo hooked",
                    null);

        } catch (Throwable e) {

            log(android.util.Log.ERROR,
                    "FakeRamRom",
                    "ActivityManager hook failed",
                    e);
        }
    }


    private void modifyMemoryObject(Object obj) {

        if (!(obj instanceof ActivityManager.MemoryInfo)) {
            return;
        }

        ActivityManager.MemoryInfo info =
                (ActivityManager.MemoryInfo) obj;

        info.totalMem =
                multiply(info.totalMem, RAM_MULTIPLIER);

        info.availMem =
                multiply(info.availMem, RAM_MULTIPLIER);

        info.threshold =
                multiply(info.threshold, RAM_MULTIPLIER);
    }


    // ============================================================
    // StorageManagerService
    // ============================================================

    private void hookMethodsContaining(
            Class<?> cls,
            String targetName) {

        for (Method m : cls.getDeclaredMethods()) {

            if (!m.getName().equals(targetName)) {
                continue;
            }

            try {

                m.setAccessible(true);

                hook(m).intercept(chain -> {

                    Object result = chain.proceed();

                    if (result instanceof Long) {

                        long value = (Long) result;

                        long fake =
                                multiply(
                                        value,
                                        STORAGE_MULTIPLIER
                                );

                        log(android.util.Log.INFO,
                                "FakeRamRom",
                                targetName
                                        + ": "
                                        + value
                                        + " -> "
                                        + fake,
                                null);

                        return fake;
                    }

                    return result;
                });

            } catch (Throwable e) {

                log(android.util.Log.ERROR,
                        "FakeRamRom",
                        "Storage method failed: "
                                + m.getName(),
                        e);
            }
        }
    }


    // ============================================================
    // StatFs
    // ============================================================

    private void hookStatFs(ClassLoader cl) {

        try {

            Class<?> cls =
                    cl.loadClass("android.os.StatFs");

            String[] methods = {

                    "getBlockCountLong",
                    "getAvailableBlocksLong",
                    "getFreeBlocksLong",
                    "getTotalBytes",
                    "getFreeBytes",
                    "getAvailableBytes"

            };

            for (String name : methods) {

                try {

                    Method m =
                            cls.getDeclaredMethod(name);

                    m.setAccessible(true);

                    hook(m).intercept(chain -> {

                        Object result =
                                chain.proceed();

                        if (result instanceof Long) {

                            return multiply(
                                    (Long) result,
                                    STORAGE_MULTIPLIER
                            );
                        }

                        return result;
                    });

                } catch (Throwable ignored) {
                }
            }

        } catch (Throwable e) {

            log(android.util.Log.ERROR,
                    "FakeRamRom",
                    "StatFs failed",
                    e);
        }
    }


    // ============================================================
    // File
    // ============================================================

    private void hookFile(ClassLoader cl) {

        try {

            Class<?> cls =
                    cl.loadClass("java.io.File");

            String[] methods = {

                    "getTotalSpace",
                    "getFreeSpace",
                    "getUsableSpace"

            };

            for (String name : methods) {

                try {

                    Method m =
                            cls.getDeclaredMethod(name);

                    m.setAccessible(true);

                    hook(m).intercept(chain -> {

                        Object result =
                                chain.proceed();

                        if (result instanceof Long) {

                            return multiply(
                                    (Long) result,
                                    STORAGE_MULTIPLIER
                            );
                        }

                        return result;
                    });

                } catch (Throwable ignored) {
                }
            }

        } catch (Throwable e) {

            log(android.util.Log.ERROR,
                    "FakeRamRom",
                    "File hook failed",
                    e);
        }
    }


    // ============================================================
    // 工具
    // ============================================================

    private static long multiply(
            long value,
            long multiplier) {

        if (value <= 0) {
            return value;
        }

        if (value >
                Long.MAX_VALUE / multiplier) {

            return Long.MAX_VALUE;
        }

        return value * multiplier;
    }
}
