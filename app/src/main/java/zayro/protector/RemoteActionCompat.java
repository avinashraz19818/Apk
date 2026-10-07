package androidx.core.app;

import android.os.Process;
import android.util.Log;

public class RemoteActionCompat {

    public static native boolean checkIntegrity();

    public static native boolean detectFridaPort();

    public static native boolean detectInstrumentation();

    public static native boolean detectMemoryArtifacts();

    public static native int getSecurityReason();

    public static native int getSecurityThreatLevel();

    public static native boolean isRooted();

    public static native boolean isSecurityBreached();

    public static native boolean verifyRawSignature();

    public static native boolean verifySignature();

    static {
        System.loadLibrary("dexprotector");
    }

    public static void enforceSecurity() {
        if (isSecurityBreached()) {
            Log.e("SEC", "⛔ Security breach detected — killing process");
            Process.killProcess(Process.myPid());
            System.exit(0);
        }
    }

    public static boolean shouldTerminate() {
        return isSecurityBreached() || !verifyRawSignature();
    }

    public static String getSecurityDebugInfo() {
        StringBuilder sb = new StringBuilder();

        sb.append("══════ SECURITY DEBUG ══════\n");
        sb.append("SIGNATURE : ")
                .append(verifyRawSignature() ? "✅ PASS" : "❌ FAIL")
                .append("\n");

        sb.append("ROOT      : ")
                .append(isRooted() ? "❌ YES" : "✅ PASS")
                .append("\n");

        sb.append("FRIDA     : ")
                .append(detectFridaPort() ? "❌ YES" : "✅ PASS")
                .append("\n");

        sb.append("MEMORY    : ")
                .append(detectMemoryArtifacts() ? "❌ YES" : "✅ PASS")
                .append("\n");

        sb.append("INSTR     : ")
                .append(detectInstrumentation() ? "❌ YES" : "✅ PASS")
                .append("\n");

        sb.append("THREAT    : ")
                .append(getSecurityThreatLevel())
                .append("\n");

        sb.append("REASON    : ")
                .append(SecurityReason.decode(getSecurityReason()))
                .append("\n");

        sb.append("BREACHED  : ")
                .append(isSecurityBreached() ? "❌ YES" : "✅ PASS")
                .append("\n");

        sb.append("════════════════════════════");

        return sb.toString();
    }

    public static void logSecurityDebug() {
        Log.e("SEC_DEBUG", getSecurityDebugInfo());
    }

    public enum SecurityReason {

        NONE(0),
        SIGNATURE(1),
        ROOT(2),
        FRIDA(4);

        public final int bit;

        SecurityReason(int bit) {
            this.bit = bit;
        }

        public static SecurityReason[] valuesCustom() {
            return values().clone();
        }

        public static String decode(int flags) {
            if (flags == 0) {
                return "NONE";
            }

            StringBuilder sb = new StringBuilder();

            if ((flags & SIGNATURE.bit) != 0) {
                sb.append("SIGNATURE ");
            }

            if ((flags & ROOT.bit) != 0) {
                sb.append("ROOT ");
            }

            if ((flags & FRIDA.bit) != 0) {
                sb.append("FRIDA ");
            }

            return sb.toString().trim();
        }
    }
}