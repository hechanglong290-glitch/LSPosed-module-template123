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
     * 全局伪装倍率
     *
     * RAM   = 2 倍
     * ROM   = 4 倍
     */
    private static final long RAM_MULTIPLIER = 2L;
    private static final long STORAGE_MULTIPLIER = 4L;

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        try {
            ClassLoader classLoader = param.getClassLoader();

            /*
             * StorageManagerService
             *
             * System Server 中负责存储相关信息。
             */
            Class<?> storageManagerService =
                    classLoader.loadClass("com.android.server.StorageManagerService");

            Method getPrimaryStorageSize =
                    storageManagerService.getDeclaredMethod("getPrimaryStorageSize");

            getPrimaryStorageSize.setAccessible(true);

            hook(getPrimaryStorageSize).intercept(chain -> {
                Object result = chain.proceed();

                if (result instanceof Long) {
                    long original = (Long) result;
                    return multiplyLong(original, STORAGE_MULTIPLIER);
                }

                return result;
            });

            log(
                    android.util.Log.INFO,
                    "FakeRamRom",
                    "StorageManagerService.getPrimaryStorageSize hooked",
                    null
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

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        /*
         * 不在这里限定普通 App。
         *
         * 本模块的核心 Hook 放在 System Server。
         */
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        /*
         * 不指定普通 App。
         *
         * RAM / Storage 的核心伪装通过 System Server 完成。
         */
    }

    /**
     * 防止 long 乘法溢出。
     */
    private static long multiplyLong(long value, long multiplier) {
        if (value <= 0) {
            return value;
        }

        if (value > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }

        return value * multiplier;
    }

    /**
     * 下面这些 Hooker 是为了给后续需要扩展
     * App 侧 API 时保留统一结构。
     */
    private static class MemoryInfoHooker implements XposedInterface.Hooker {

        @Override
        public Object intercept(@NonNull XposedInterface.Chain chain)
                throws Throwable {

            Object result = chain.proceed();

            if (result instanceof ActivityManager.MemoryInfo) {
                ActivityManager.MemoryInfo info =
                        (ActivityManager.MemoryInfo) result;

                info.totalMem =
                        multiplyLong(info.totalMem, RAM_MULTIPLIER);

                info.availMem =
                        multiplyLong(info.availMem, RAM_MULTIPLIER);

                info.threshold =
                        multiplyLong(info.threshold, RAM_MULTIPLIER);
            }

            return result;
        }
    }

    /**
     * StatFs / File 系列使用的统一乘法逻辑。
     *
     * 注意：
     * 这些类属于应用侧 API。
     * 当前版本先不在这里盲目 Hook，
     * 避免把 System Server 和普通 App 的 ClassLoader 混在一起。
     */
    private static long multiplyStorage(long value) {
        return multiplyLong(value, STORAGE_MULTIPLIER);
    }
}
