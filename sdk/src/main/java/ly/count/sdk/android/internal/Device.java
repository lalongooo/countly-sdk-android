package ly.count.sdk.android.internal;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ly.count.sdk.internal.Params;

/**
 * Class encapsulating most of device-specific logic: metrics, info, etc.
 */

public class Device extends ly.count.sdk.internal.Device {

    /**
     * Get operation system name
     *
     * @return the display name of the current operating system.
     */
    public static String getOS() {
        return "Android";
    }

    /**
     * Get Android version
     *
     * @return current operating system version as a displayable string.
     */
    public static String getOSVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * Get device model
     *
     * @return device model name.
     */
    public static String getDevice() {
        return android.os.Build.MODEL;
    }

    /**
     * Get the non-scaled pixel resolution of the current default display being used by the
     * WindowManager in the specified context.
     *
     * @param context context to use to retrieve the current WindowManager
     * @return a string in the format "WxH", or the empty string "" if resolution cannot be determined
     */
    public static String getResolution(final android.content.Context context) {
        // user reported NPE in this method; that means either getSystemService or getDefaultDisplay
        // were returning null, even though the documentation doesn't say they should do so; so now
        // we catch Throwable and return empty string if that happens
        String resolution = "";
        try {
            final WindowManager wm = (WindowManager) context.getSystemService(android.content.Context.WINDOW_SERVICE);
            final Display display = wm.getDefaultDisplay();
            final DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            resolution = metrics.widthPixels + "x" + metrics.heightPixels;
        }
        catch (Throwable t) {
            L.w("Device resolution cannot be determined", t);
        }
        return resolution;
    }

    /**
     * Maps the current display density to a string constant.
     *
     * @param context context to use to retrieve the current display metrics
     * @return a string constant representing the current display density, or the
     *         empty string if the density is unknown
     */
    public static String getDensity(final android.content.Context context) {
        String densityStr = "";
        final int density = context.getResources().getDisplayMetrics().densityDpi;
        switch (density) {
            case DisplayMetrics.DENSITY_LOW:
                densityStr = "LDPI";
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                densityStr = "MDPI";
                break;
            case DisplayMetrics.DENSITY_TV:
                densityStr = "TVDPI";
                break;
            case DisplayMetrics.DENSITY_HIGH:
                densityStr = "HDPI";
                break;
            case DisplayMetrics.DENSITY_260:
                densityStr = "XHDPI";
                break;
            case DisplayMetrics.DENSITY_280:
                densityStr = "XHDPI";
                break;
            case DisplayMetrics.DENSITY_300:
                densityStr = "XHDPI";
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                densityStr = "XHDPI";
                break;
            case DisplayMetrics.DENSITY_340:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_360:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_400:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_420:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_560:
                densityStr = "XXXHDPI";
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                densityStr = "XXXHDPI";
                break;
            default:
                densityStr = "other";
                break;
        }
        return densityStr;
    }

    /**
     * Returns the display name of the current network operator from the
     * TelephonyManager from the specified context.
     *
     * @param context context to use to retrieve the TelephonyManager from
     * @return the display name of the current network operator, or the empty
     *         string if it cannot be accessed or determined
     */
    public static String getCarrier(final android.content.Context context) {
        String carrier = "";
        final TelephonyManager manager = (TelephonyManager) context.getSystemService(android.content.Context.TELEPHONY_SERVICE);
        if (manager != null) {
            carrier = manager.getNetworkOperatorName();
        }
        if (carrier == null || carrier.length() == 0) {
            carrier = "";
            L.w("No carrier found");
        }
        return carrier;
    }

    /**
     * Get application version
     *
     * @return string stored in the specified context's package info versionName field,
     * or "1.0" if versionName is not present.
     */
    public static String getAppVersion(final ly.count.sdk.android.internal.Ctx context) {
        String result = context.getConfig().getApplicationVersion();
        if (Utils.isEmpty(result)) {
            result = "1.0";
            try {
                result = context.getContext().getPackageManager().getPackageInfo(context.getContext().getPackageName(), 0).versionName;
            }
            catch (PackageManager.NameNotFoundException e) {
                L.w("No app version found", e);
            }
        }
        return result;
    }

    /**
     * Get package name of an app which installed this app
     *
     * @return package name of the store
     */
    static String getStore(final android.content.Context context) {
        String result = "";
        try {
            result = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        } catch (Exception e) {
            L.w("Can't get Installer package", e);
        }
        if (result == null || result.length() == 0) {
            result = "";
            L.w("No store found");
        }
        return result;
    }

    /**
     * Build metrics {@link Params} object as required by Countly server
     *
     * @param ctx Ctx in which to request metrics
     */
    public static Params buildMetrics(final ly.count.sdk.android.internal.Ctx ctx) {
        android.content.Context context = ctx.getContext();
        Params params = new Params();
        params.obj("metrics")
                .put("_device", getDevice())
                .put("_os", getOS())
                .put("_os_version", getOSVersion())
                .put("_carrier", getCarrier(context))
                .put("_resolution", getResolution(context))
                .put("_density", getDensity(context))
                .put("_locale", getLocale())
                .put("_app_version", getAppVersion(ctx))
                .put("_store", getStore(context))
            .add();

        return params;
    }

    public static boolean API(int min) {
        return Build.VERSION.SDK_INT >= min;
    }

    /**
     * Get total RAM in Mb
     *
     * @return total RAM in Mb or null if cannot determine
     */
    public static Long getRAMTotal() {
        RandomAccessFile reader = null;
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            String load = reader.readLine();

            // Get the Number value from the string
            Pattern p = Pattern.compile("(\\d+)");
            Matcher m = p.matcher(load);
            String value = "";
            while (m.find()) {
                value = m.group(1);
            }
            return Long.parseLong(value) / 1024;
        } catch (NumberFormatException e){
            L.e("Cannot parse meminfo", e);
            return null;
        } catch (IOException e) {
            L.e("Cannot read meminfo", e);
            return null;
        }
        finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Get current device manufacturer name.
     *
     * @return device manufacturer string
     */
    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * Get current device cpu.
     *
     * @return main CPU ABI
     */
    public static String getCpu() {
        if (Utils.API(Build.VERSION_CODES.LOLLIPOP)) {
            return Build.SUPPORTED_ABIS[0];
        } else {
            return Build.CPU_ABI;
        }
    }

    /**
     * Get current device OpenGL version.
     *
     * @return OpenGL version, falls back to 1 if cannot determine
     */
    public static Integer getOpenGL(android.content.Context context) {
        PackageManager packageManager = context.getPackageManager();
        FeatureInfo[] featureInfos = packageManager.getSystemAvailableFeatures();
        if (featureInfos == null || featureInfos.length == 0) {
            return 1;
        }
        for (FeatureInfo featureInfo : featureInfos) {
            // Null feature name means this feature is the open gl es version feature.
            if (featureInfo.name == null) {
                if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
                    return (featureInfo.reqGlEsVersion & 0xffff0000) >> 16;
                } else {
                    return 1; // Lack of property means OpenGL ES version 1
                }
            }
        }
        return 1;
    }

    /**
     * Get current device RAM amount.
     *
     * @return currently available RAM in Mb or {@code null} if couldn't determine
     */
    public static Long getRAMAvailable(android.content.Context context) {
        Long total = getRAMTotal();
        if (total == null) {
            return null;
        }
        total = total * BYTES_IN_MB;
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return (total - mi.availMem) / BYTES_IN_MB;
    }

    /**
     * Get current device disk space.
     *
     * @return currently available disk space in Mb
     */
    public static Long getDiskAvailable() {
        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long total = getDiskTotal() * BYTES_IN_MB, free;
        if (Utils.API(18)) {
            free = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
        }  else {
            free = ((long)statFs.getAvailableBlocks() * (long)statFs.getBlockSize());
        }
        return (total - free) / BYTES_IN_MB;
    }

    /**
     * Get total device disk space.
     *
     * @return total device disk space in Mb
     */
    public static Long getDiskTotal() {
        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long total;
        if (Utils.API(18)) {
            total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
        }  else {
            total = ((long)statFs.getBlockCount() * (long)statFs.getBlockSize());
        }
        return total / BYTES_IN_MB;
    }

    /**
     * Get current device battery level.
     *
     * @return device battery left in percent or {@code null} if couldn't determine
     */
    public static Float getBatteryLevel(android.content.Context context) {
        try {
            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                // Error checking that probably isn't needed but I added just in case.
                if (level > -1 && scale > 0) {
                    return 100.0f * (float) level / (float) scale;
                }
            }
        } catch (Exception e) {
            L.w("Can't get batter level", e);
        }

        return null;
    }

    /**
     * Get current device orientation.
     *
     * @return orientation name or {@code null} if couldn't determine
     */
    public static String getOrientation(android.content.Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        switch (orientation) {
            case  Configuration.ORIENTATION_LANDSCAPE:
                return "Landscape";
            case Configuration.ORIENTATION_PORTRAIT:
                return "Portrait";
            case Configuration.ORIENTATION_UNDEFINED:
                return "Unknown";
            default:
                return null;
        }
    }

    /**
     * Check if device is rooted.
     *
     * @return {@code true} if rooted, {@code false} otherwise
     */
    public static Boolean isRooted() {
        String[] paths = {
                "/sbin/su", "/system/bin/su", "/system/xbin/su",
                "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su" };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    /**
     * Check if device is online.
     *
     * @return {@code true} if has connectivity, {@code false} if doesn't, {@code null} if cannot determine
     */
    @SuppressLint("MissingPermission")
    public static Boolean isOnline(android.content.Context context) {
        try {
            ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            if (conMgr != null && conMgr.getActiveNetworkInfo() != null
                    && conMgr.getActiveNetworkInfo().isAvailable()
                    && conMgr.getActiveNetworkInfo().isConnected()) {

                return true;
            }
            return false;
        } catch(Exception e) {
            L.w("Exception while determining connectivity", e);
        }
        return null;
    }

    /**
     * Check if device is muted.
     *
     * @return {@code true} if muted, {@code false} if not, {@code null}  if cannot determine
     */
    public static Boolean isMuted(android.content.Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(android.content.Context.AUDIO_SERVICE);
        if (audio == null) {
            return null;
        }
        switch( audio.getRingerMode() ){
            case AudioManager.RINGER_MODE_SILENT:
                return true;
            case AudioManager.RINGER_MODE_VIBRATE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check whether app is running in foreground.
     *
     * @param context context to check in
     * @return {@code true} if running in foreground, {@code false} otherwise
     */
    public static boolean isAppRunningInForeground (android.content.Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) {
                return false;
            }
            final String packageName = context.getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isDebuggerConnected() {
        return Debug.isDebuggerConnected();
    }

    /**
     * Return current process name
     *
     * @param context Ctx to run in
     * @return process name String or {@code null} if cannot determine
     */
    public static String getProcessName(android.content.Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
        if (manager != null) {
            List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
            int pid = android.os.Process.myPid();

            for (ActivityManager.RunningAppProcessInfo info : infos) {
                if (info.pid == pid) {
                    return info.processName;
                }
            }
        }

        return null;
    }

    public static boolean isInLimitedProcess(android.content.Context context) {
        return Utils.contains(getProcessName(context), ":countly");
    }

    public static boolean isSingleProcess(android.content.Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
        if (manager != null) {
            List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();

            int pid = android.os.Process.myPid();

            for (ActivityManager.RunningAppProcessInfo info : infos) {
                if (info.pid == pid) {
                    for (ActivityManager.RunningAppProcessInfo i : infos) {
                        if (i != info && (Utils.contains(i.processName, info.processName) || Utils.contains(info.processName, i.processName))) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return true;
    }

}