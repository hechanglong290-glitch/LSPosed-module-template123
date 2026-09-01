```java
package com.example.module;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.os.StatFs;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

@SuppressLint({"PrivateApi", "BlockedPrivateApi"})
public class MainModule extends XposedModule {

    /*
     * ============================================================
     * Fake RAM / ROM
     *
     * RAM : ×2
     * ROM : ×4
     * ============================================================
     */

    private static final long RAM_MULTIPLIER = 2L;
    private static final long ROM_MULTIPLIER = 4L;


    /*
     * ============================================================
     * 1. system_server
     *
     * StorageManagerService.getPrimaryStorageSize()
     * ============================================================
     */

    @Override
    public void onSystemServerStarting(
            @NonNull SystemServerStartingParam param) {

        try {
            ClassLoader classLoader = param.getClassLoader();

            Class<?> clazz = classLoader.loadClass(
                    "com.android.server.StorageManagerService"
            );

            Method method = clazz.getDeclaredMethod(
                    "getPrimaryStorageSize"
            );

            method.setAccessible(true);

            hook(method).intercept(new XposedInterface.Hooker() {

                @Override
                public Object intercept(
                        @NonNull XposedInterface.Chain chain)
                        throws Throwable {

                    Object result = chain.proceed();

                    if (result instanceof Long) {
                        long original = (Long) result;

                        return original * ROM_MULTIPLIER;
                    }

                    return result;
                }
            });

            log(
                    android.util.Log.INFO,
                    "FakeRamRom",
                    "StorageManagerService.getPrimaryStorageSize() hooked"
            );

        } catch (Throwable t) {

            log(
                    android.util.Log.ERROR,
                    "FakeRamRom",
                    "Failed to hook StorageManagerService",
                    t
            );
        }
    }


    /*
     * ============================================================
     * 2. 普通进程
     *
     * 全局处理所有加载的 App。
     *
     * 不检查 packageName。
     * ============================================================
     */

    @Override
    public void onPackageReady(
            @NonNull PackageReadyParam param) {

        try {

            ClassLoader classLoader = param.getClassLoader();

            /*
             * ----------------------------------------------------
             * RAM
             *
             * ActivityManager.getMemoryInfo()
             * ----------------------------------------------------
             */

            hookActivityManager(classLoader);


            /*
             * ----------------------------------------------------
             * ROM
             *
             * StatFs
             * ----------------------------------------------------
             */

            hookStatFs(classLoader);


            /*
             * ----------------------------------------------------
             * ROM
             *
             * java.io.File
             * ----------------------------------------------------
             */

            hookFile(classLoader);

        } catch (Throwable t) {

            log(
                    android.util.Log.ERROR,
                    "FakeRamRom",
                    "Failed to install application hooks",
                    t
            );
        }
    }


    /*
     * ============================================================
     * ActivityManager
     * ============================================================
     */

    private void hookActivityManager(
            ClassLoader classLoader) {

        try {

            Class<?> activityManagerClass =
                    Class.forName(
                            "android.app.ActivityManager",
                            false,
                            classLoader
                    );

            Class<?> memoryInfoClass =
                    Class.forName(
                            "android.app.ActivityManager$MemoryInfo",
                            false,
                            classLoader
                    );

            Method method =
                    activityManagerClass.getDeclaredMethod(
                            "getMemoryInfo",
                            memoryInfoClass
                    );

            method.setAccessible(true);

            hook(method).intercept(
                    new XposedInterface.Hooker() {

                        @Override
                        public Object intercept(
                                @NonNull XposedInterface.Chain chain)
                                throws Throwable {

                            Object result = chain.proceed();

                            Object[] args = chain.getArgs();

                            if (args != null && args.length > 0) {

                                Object memoryInfo = args[0];

                                if (memoryInfo instanceof ActivityManager.MemoryInfo) {

                                    ActivityManager.MemoryInfo mi =
                                            (ActivityManager.MemoryInfo) memoryInfo;

                                    mi.totalMem =
                                            mi.totalMem * RAM_MULTIPLIER;

                                    mi.availMem =
                                            mi.availMem * RAM_MULTIPLIER;

                                    mi.threshold =
                                            mi.threshold * RAM_MULTIPLIER;
                                }
                            }

                            return result;
                        }
                    }
            );

        } catch (Throwable ignored) {
            /*
             * 某些进程可能没有该 API，
             * 不影响其他 Hook。
             */
        }
    }


    /*
     * ============================================================
     * StatFs
     * ============================================================
     */

    private void hookStatFs(
            ClassLoader classLoader) {

        try {

            Class<?> clazz =
                    Class.forName(
                            "android.os.StatFs",
                            false,
                            classLoader
                    );


            /*
             * block count
             */

            hookMethod(
                    clazz,
                    "getBlockCountLong"
            );


            /*
             * available blocks
             */

            hookMethod(
                    clazz,
                    "getAvailableBlocksLong"
            );


            /*
             * free blocks
             */

            hookMethod(
                    clazz,
                    "getFreeBlocksLong"
            );


            /*
             * total bytes
             */

            hookMethod(
                    clazz,
                    "getTotalBytes"
            );


            /*
             * free bytes
             */

            hookMethod(
                    clazz,
                    "getFreeBytes"
            );


            /*
             * available bytes
             */

            hookMethod(
                    clazz,
                    "getAvailableBytes"
            );

        } catch (Throwable ignored) {
            /*
             * 当前进程没有对应 API 时跳过。
             */
        }
    }


    /*
     * ============================================================
     * File
     * ============================================================
     */

    private void hookFile(
            ClassLoader classLoader) {

        try {

            Class<?> clazz =
                    Class.forName(
                            "java.io.File",
                            false,
                            classLoader
                    );


            /*
             * total space
             */

            hookMethod(
                    clazz,
                    "getTotalSpace"
            );


            /*
             * free space
             */

            hookMethod(
                    clazz,
                    "getFreeSpace"
            );


            /*
             * usable space
             */

            hookMethod(
                    clazz,
                    "getUsableSpace"
            );

        } catch (Throwable ignored) {
            /*
             * 不影响其他 Hook。
             */
        }
    }


    /*
     * ============================================================
     * 通用 Long 返回值 ×4
     *
     * 用于 StatFs / File
     * ============================================================
     */

    private void hookMethod(
            Class<?> clazz,
            String methodName) {

        try {

            Method method =
                    clazz.getDeclaredMethod(methodName);

            method.setAccessible(true);

            hook(method).intercept(
                    new XposedInterface.Hooker() {

                        @Override
                        public Object intercept(
                                @NonNull XposedInterface.Chain chain)
                                throws Throwable {

                            Object result =
                                    chain.proceed();

                            if (result instanceof Long) {

                                return ((Long) result)
                                        * ROM_MULTIPLIER;
                            }

                            if (result instanceof Integer) {

                                return ((Integer) result)
                                        * (int) ROM_MULTIPLIER;
                            }

                            return result;
                        }
                    }
            );

        } catch (Throwable ignored) {
            /*
             * 单个方法失败不影响其他方法。
             */
        }
    }
}
```
