package com.xuan.pokemonz;

import android.content.Context;
import android.util.Log;

/**
 * xuanDun Native Protection Wrapper.
 * 
 * Requires: libxuanDunProtect.so in jniLibs/
 * 
 * Usage in XuanDunApp.attachBaseContext():
 *   XuanDunProtect.init(this);
 */
public class XuanDunProtect {

    private static final String TAG = "xuanDun";

    /* ── Detection Flags ──────────────────────────────────────────── */
    public static final int FLAG_NONE        = 0x00000000;
    public static final int FLAG_FRIDA       = 0x00000001;
    public static final int FLAG_DEBUGGER    = 0x00000002;
    public static final int FLAG_TRACER      = 0x00000004;
    public static final int FLAG_XPOSED      = 0x00000008;
    public static final int FLAG_HOOK        = 0x00000010;
    public static final int FLAG_VIRTUAL     = 0x00000020;
    public static final int FLAG_DUMPER      = 0x00000040;
    public static final int FLAG_DEX_TAMPER  = 0x00000080;
    public static final int FLAG_MEM_SCAN    = 0x00000100;
    public static final int FLAG_EMULATOR    = 0x00000200;
    public static final int FLAG_GAMEGUARDIAN = 0x00000400;

    /* ── State ────────────────────────────────────────────────────── */
    private static boolean sInitialized = false;
    private static int     sLastFlags   = 0;

    static {
        try {
            System.loadLibrary("xuanDunProtect");
            ////Log.e(TAG, "Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            //Log.e(TAG, "Failed to load xuanDunProtect: " + e.getMessage());
        }
    }

    /* ═══════════════════════════════════════════════════════════════
     *  Public API
     * ═══════════════════════════════════════════════════════════════ */

    /**
     * Initialize native protection.
     * Call from XuanDunApp.attachBaseContext() BEFORE any other logic.
     * Will crash the app immediately if threats are detected.
     *
     * @param context  Application context
     * @return true if init succeeded, false if library not loaded
     */
    public static boolean init(Context context) {
        if (sInitialized) {
            //Log.e(TAG, "Already initialized — run scan() for re-check");
            return true;
        }

        try {
            sInitialized = nativeInit(context.getApplicationContext());
            if (sInitialized) {
                ////Log.e(TAG, "✅ xuanDun Protection active");
            } else {
                //Log.e(TAG, "❌ Native init returned false");
            }
        } catch (Throwable t) {
            //Log.e(TAG, "Init crashed: " + t.getMessage(), t);
            sInitialized = false;
        }

        return sInitialized;
    }

    /**
     * Manual scan — returns detection flags (0 = clean).
     */
    public static int scan() {
        try {
            sLastFlags = nativeScan();
            if (sLastFlags != 0) {
                //Log.e(TAG, "⚠ Threats: " + describeFlags(sLastFlags));
            }
        } catch (Throwable t) {
            //Log.e(TAG, "Scan error: " + t.getMessage());
            sLastFlags = -1;
        }
        return sLastFlags;
    }

    /**
     * Quick Frida check (no crash).
     */
    public static boolean isFridaDetected() {
        try { return nativeCheckFrida(); }
        catch (Throwable t) { return false; }
    }

    /**
     * Quick debugger check (no crash).
     */
    public static boolean isDebuggerDetected() {
        try { return nativeCheckDebugger(); }
        catch (Throwable t) { return false; }
    }

    /**
     * Quick hook check (no crash).
     */
    public static boolean isHookDetected() {
        try { return nativeCheckHook(); }
        catch (Throwable t) { return false; }
    }

    /**
     * Quick Xposed check (no crash).
     */
    public static boolean isXposedDetected() {
        try { return nativeCheckXposed(); }
        catch (Throwable t) { return false; }
    }

    /**
     * Verify DEX integrity.
     * @return true if ALL loaded DEX files are untampered
     */
    public static boolean verifyDexIntegrity() {
        try { return nativeVerifyDex(); }
        catch (Throwable t) { return false; }
    }

    /**
     * Get human-readable description of flags.
     */
    public static String describeFlags(int flags) {
        try { return nativeDescribeFlags(flags); }
        catch (Throwable t) { return "ERROR: " + t.getMessage(); }
    }

    /**
     * Get last scan result flags.
     */
    public static int getLastFlags() {
        return sLastFlags;
    }

    /**
     * Check if protection is initialized.
     */
    public static boolean isInitialized() {
        return sInitialized;
    }

    /**
     * Force crash (for testing).
     */
    public static void forceCrash(String reason) {
        try { nativeCrash(reason); }
        catch (Throwable t) {
            throw new SecurityException("xuanDun crash: " + reason);
        }
    }

    /* ═══════════════════════════════════════════════════════════════
     *  Native Methods
     * ═══════════════════════════════════════════════════════════════ */

    private static native boolean nativeInit(Context context);
    private static native int     nativeScan();
    private static native boolean nativeCheckFrida();
    private static native boolean nativeCheckDebugger();
    private static native boolean nativeCheckHook();
    private static native boolean nativeCheckXposed();
    private static native boolean nativeVerifyDex();
    private static native String  nativeDescribeFlags(int flags);
    private static native void    nativeCrash(String message);
}