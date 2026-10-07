package com.xuan.pokemonz;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedMethodImplementation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CorePatcher5 {

    private static final String AES_KEY_STR   = "kQ7#zX9mLp2!vR4w";
    private static final String ELF_MAGIC     = "\u007FELF";
    private static final String BLOB_MAGIC_V2 = "PGDEX";
    private static final int    BLOB_VERSION  = 4;

    private static final String SO_OUTPUT_NAME   = "xuanDun_wifi";
    private static final String JSON_OUTPUT_NAME = "xuanDun_conf.dex";

    private static final int CHUNK_SIZE      = 1024 * 1024;
    private static final int CHUNK_THRESHOLD = 5 * 1024 * 1024;

    public static int maxMethodsToExtract = Integer.MAX_VALUE;
    public static int minInstructionBytesToExtract = 8;

    public static List<String> ownPackagePrefixes = new ArrayList<>();

    public static String appClassToPatch = null;
    public static String applicationClassToKeep = null;

    public static final String STUB_APPLICATION_DOT = "com.xuan.XuanDunApp";

    // FULL-DEX COVERAGE MODE: only framework/runtime-safe classes stay unencrypted.
    // Third-party library methods (okhttp, retrofit, glide, squareup, reactivex ...)
    // are now extracted and encrypted too, so the on-disk DEX is a hollow shell.
    private static final String[] SKIP_PREFIXES = {
        "Ljava/", "Ljavax/", "Ljdk/", "Landroid/", "Landroidx/", "Ldalvik/",
        "Lkotlin/", "Lkotlinx/", "Lorg/jetbrains/", "Lcom/google/",
        "Lcom/xuan/",
    };

    private static final java.nio.charset.Charset UTF8 = StandardCharsets.UTF_8;
    private final LogCallback logger;

    private int skippedAllowlist = 0;
    private int skippedBlocklist = 0;
    private int skippedClinit = 0;
    private int skippedNoImpl = 0;
    private int skippedTooSmall = 0;
    private int skippedAppClass = 0;

    public interface LogCallback {
        void onLog(String msg);
    }

    public CorePatcher5(LogCallback logger) {
        this.logger = logger;
    }

    public int process(String apkPath, String pkgName, String outDirStr) {
        // Full coverage mode: do NOT restrict to the app's own package.
        // Every class that is not in SKIP_PREFIXES gets its methods encrypted.
        return _process(apkPath, pkgName, outDirStr);
    }

    public static void addOwnPackage(String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) return;
        String p = pkg.trim();
        if (p.startsWith("L")) p = p.substring(1);
        if (p.endsWith(";")) p = p.substring(0, p.length() - 1);
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        p = p.replace('.', '/');
        if (p.isEmpty()) return;
        String slashed = "L" + p + "/";
        if (!ownPackagePrefixes.contains(slashed)) {
            ownPackagePrefixes.add(slashed);
        }
    }

    private static boolean isOwnClass(String className) {
        for (String prefix : SKIP_PREFIXES) {
            if (className.startsWith(prefix)) return false;
        }
        if (ownPackagePrefixes.isEmpty()) return true;
        for (String prefix : ownPackagePrefixes) {
            if (className.startsWith(prefix)) return true;
        }
        return false;
    }

    private static boolean isInBlocklist(String className) {
        for (String prefix : SKIP_PREFIXES) {
            if (className.startsWith(prefix)) return true;
        }
        return false;
    }

    private static boolean isApplicationClassToKeep(String classNameDesc) {
        if (applicationClassToKeep == null || applicationClassToKeep.trim().isEmpty()) return false;
        String target = applicationClassToKeep.trim();
        if (target.startsWith("L")) target = target.substring(1);
        if (target.endsWith(";")) target = target.substring(0, target.length() - 1);
        target = target.replace('/', '.');
        String cn = classNameDesc;
        if (cn.startsWith("L")) cn = cn.substring(1);
        if (cn.endsWith(";")) cn = cn.substring(0, cn.length() - 1);
        cn = cn.replace('/', '.');
        return cn.equals(target);
    }

    private static class MethodInfo {
        final String dexName;
        final String className;
        final String methodName;
        final int    codeOffset;
        final byte[] instructionBytes;

        MethodInfo(String dexName, String className, String methodName,
                   int codeOffset, byte[] instructionBytes) {
            this.dexName = dexName;
            this.className = className;
            this.methodName = methodName;
            this.codeOffset = codeOffset;
            this.instructionBytes = instructionBytes;
        }
    }

    // ================= File helpers =================
    private static void forceMkdir(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Failed to create directory: " + dir);
    }

    private static void copyFile(File src, File dest) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private static void copyInputStreamToFile(InputStream in, File dest) throws IOException {
        try (OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private static byte[] readFileToByteArray(File file) throws IOException {
        try (InputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(data);
            out.flush();
        }
    }

    private static void deleteDirectory(File dir) throws IOException {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else if (!f.delete()) throw new IOException("Failed to delete: " + f);
            }
        }
        if (!dir.delete()) throw new IOException("Failed to delete: " + dir);
    }

    // ================= Main process =================
    private int _process(String apkPath, String pkgName, String outDirStr) {
        File outDir       = new File(outDirStr);
        File dexOutDir    = new File(outDir, "extracted_dex");
        File tempDir      = new File(outDir, "temp");
        File patchedDir   = new File(tempDir, "patched");
        File encryptedDir = new File(tempDir, "encrypted");
        File outBlob      = new File(outDir, SO_OUTPUT_NAME);
        File outJson      = new File(outDir, JSON_OUTPUT_NAME);

        try {
            forceMkdir(outDir);
            forceMkdir(dexOutDir);
            forceMkdir(patchedDir);
            forceMkdir(encryptedDir);

            System.out.println("=== Allowlist packages: " + ownPackagePrefixes + " ===");

            List<File> dexFiles = extractDex(apkPath, dexOutDir);
            if (dexFiles.isEmpty()) return 0;
            int originalDexCount = dexFiles.size();
            System.out.println("Found " + originalDexCount + " DEX files.");

            Map<String, Integer> methodCountByDex = new LinkedHashMap<>();
            Map<String, File>    encFileByDex     = new LinkedHashMap<>();
            int totalMethods = 0;

            for (File dex : dexFiles) {
                File patched = new File(patchedDir, dex.getName());
                copyFile(dex, patched);

                File encFile = new File(encryptedDir, dex.getName() + ".enc");
                int count = extractAndEncryptBatch(patched, pkgName, encFile, maxMethodsToExtract);

                if (count > 0) {
                    methodCountByDex.put(dex.getName(), count);
                    encFileByDex.put(dex.getName(), encFile);
                } else {
                    if (encFile.exists()) encFile.delete();
                }
                totalMethods += count;
                System.out.println("  " + dex.getName() + ": " + count + " methods extracted"
                        + (count == 0 ? " (skipped)" : ""));
            }

            System.out.println("Total: " + totalMethods + " methods");
            System.out.println("   allowlist:" + skippedAllowlist + " blocklist:" + skippedBlocklist
                    + " clinit:" + skippedClinit + " noImpl:" + skippedNoImpl + " tooSmall:" + skippedTooSmall
                    + " appClass:" + skippedAppClass);

            byte[] jsonEncrypted = buildMetadataJson(dexFiles, methodCountByDex, totalMethods, originalDexCount);
            writeByteArrayToFile(outJson, jsonEncrypted);

            packBlobV4(patchedDir, encFileByDex, outBlob);

            try {
                if (dexOutDir.exists()) deleteDirectory(dexOutDir);
                if (tempDir.exists())   deleteDirectory(tempDir);
            } catch (IOException ex) {}

            System.out.println("Done - " + originalDexCount + " DEX, " + totalMethods + " methods");
            return originalDexCount;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int extractAndEncryptBatch(File dexFile, String pkgName, File outputEncFile, int maxRemaining)
            throws Exception {
        List<MethodInfo> methods = new ArrayList<>();

        try {
            DexBackedDexFile dex = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault());

            try (RandomAccessFile raf = new RandomAccessFile(dexFile, "rw")) {
                for (ClassDef cls : dex.getClasses()) {
                    String className = cls.getType();
                    if (!isOwnClass(className)) {
                        if (isInBlocklist(className)) skippedBlocklist++;
                        else skippedAllowlist++;
                        continue;
                    }

                    if (isApplicationClassToKeep(className)) {
                        skippedAppClass++;
                        continue;
                    }

                    for (Method method : cls.getMethods()) {
                        if (methods.size() >= maxRemaining) break;
                        String methodName = method.getName();
                        if ("<clinit>".equals(methodName)) { skippedClinit++; continue; }
                        if (method.getImplementation() == null) { skippedNoImpl++; continue; }
                        if (!(method.getImplementation() instanceof DexBackedMethodImplementation)) {
                            skippedNoImpl++; continue;
                        }
                        DexBackedMethodImplementation impl =
                                (DexBackedMethodImplementation) method.getImplementation();
                        int codeOffset = getCodeOffset(impl);
                        if (codeOffset <= 0) { skippedNoImpl++; continue; }
                        int insnsSize = getInstructionSizeFast(impl);
                        if (insnsSize < minInstructionBytesToExtract) { skippedTooSmall++; continue; }

                        int headerSize = 16;
                        raf.seek(codeOffset);
                        byte[] fullCodeItem = new byte[headerSize + insnsSize];
                        raf.readFully(fullCodeItem);
                        byte[] instructionBytes = new byte[insnsSize];
                        System.arraycopy(fullCodeItem, headerSize, instructionBytes, 0, insnsSize);

                        methods.add(new MethodInfo(
                                dexFile.getName(), className, methodName,
                                codeOffset, whitenInsns(instructionBytes, codeOffset, className, methodName)));

                        raf.seek(codeOffset + headerSize);
                        raf.write(new byte[insnsSize]);
                        raf.seek(codeOffset + headerSize);
                        raf.write(new byte[]{0x0E, 0x00});
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("extract EX: " + e);
        }

        if (methods.isEmpty()) {
            return 0;
        }

        final int extractedCount = methods.size();
        byte[] serialized = serializeMethodList(methods);

        byte[] encrypted;
        if (serialized.length > CHUNK_THRESHOLD) {
            encrypted = elfEncryptChunked(serialized, CHUNK_SIZE);
        } else {
            encrypted = elfEncrypt(serialized);
        }

        writeByteArrayToFile(outputEncFile, encrypted);

        methods.clear();
        serialized = null;
        encrypted = null;
        return extractedCount;
    }

    private byte[] serializeMethodList(List<MethodInfo> methods) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(methods.size() * 256);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(methods.size());

        for (MethodInfo m : methods) {
            byte[] dn = m.dexName.getBytes(UTF8);
            byte[] cn = m.className.getBytes(UTF8);
            byte[] mn = m.methodName.getBytes(UTF8);

            dos.writeShort(dn.length); dos.write(dn);
            dos.writeShort(cn.length); dos.write(cn);
            dos.writeShort(mn.length); dos.write(mn);
            dos.writeInt(m.codeOffset);
            dos.writeInt(m.instructionBytes.length);
            dos.write(m.instructionBytes);
        }

        dos.flush();
        CRC32 crc = new CRC32();
        crc.update(baos.toByteArray());
        dos.writeInt((int) crc.getValue());
        dos.flush();

        byte[] result = baos.toByteArray();
        dos.close();
        return result;
    }

    private static int getInstructionSizeFast(DexBackedMethodImplementation impl) {
        for (String fn : new String[]{"codeItemSize", "codeSize", "insnsSize", "insns_size"}) {
            try {
                java.lang.reflect.Field f = impl.getClass().getDeclaredField(fn);
                f.setAccessible(true);
                int v = f.getInt(impl);
                if (v > 0) return v;
            } catch (Exception ignored) {}
        }
        int codeUnits = 0;
        try {
            for (Instruction instr : impl.getInstructions()) {
                codeUnits += instr.getCodeUnits();
            }
        } catch (Exception ignored) {}
        return codeUnits * 2;
    }

    private static int getCodeOffset(DexBackedMethodImplementation impl) {
        for (String fn : new String[]{"codeOffset", "codeItemOffset", "codeOff"}) {
            try {
                java.lang.reflect.Field f = impl.getClass().getDeclaredField(fn);
                f.setAccessible(true);
                int v = f.getInt(impl);
                if (v > 0) return v;
            } catch (Exception ignored) {}
        }
        return -1;
    }

    private byte[] buildMetadataJson(List<File> dexFiles,
                                     Map<String, Integer> methodCountByDex,
                                     int totalMethods, int originalDexCount) throws Exception {
        StringBuilder json = new StringBuilder(4096 + dexFiles.size() * 128);
        json.append("{\n");
        json.append("  \"magic\": \"xuanDunGuard\",\n");
        json.append("  \"version\": ").append(BLOB_VERSION).append(",\n");
        json.append("  \"dexCount\": ").append(originalDexCount).append(",\n");
        json.append("  \"dexNames\": [");
        for (int i = 0; i < dexFiles.size(); i++) {
            if (i > 0) json.append(", ");
            json.append('"').append(escapeJson(dexFiles.get(i).getName())).append('"');
        }
        json.append("],\n");
        json.append("  \"totalMethods\": ").append(totalMethods).append(",\n");
        json.append("  \"restoreInfos\": []\n");
        json.append('}');
        String jsonStr = json.toString();
        return elfEncrypt(jsonStr.getBytes(UTF8));
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Instruction whitening: XOR layer derived from (codeOffset, class, method).
     * Applied before the AES-GCM container, so even a leaked AES key still
     * leaves the method bodies unusable without this algorithm.
     */
    static byte[] whitenInsns(byte[] insns, int codeOffset, String className, String methodName) {
        int h = codeOffset;
        String sig = className + methodName;
        for (int i = 0; i < sig.length(); i++) {
            h = h * 31 + sig.charAt(i);
        }
        byte[] out = new byte[insns.length];
        for (int i = 0; i < insns.length; i++) {
            out[i] = (byte) (insns[i] ^ (h >>> ((i & 3) * 8)) ^ (i * 0x5B) ^ 0x9E);
        }
        return out;
    }

    /**
     * Assets protection: packs all original assets/ files into one
     * AES-GCM + XOR encrypted container (chunked). The runtime stub
     * decrypts them into a private overlay dir and addAssetPath()s it.
     */
    public static byte[] buildAssetsPack(java.util.List<String> names, java.util.List<byte[]> datas)
            throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
        dos.writeInt(names.size());
        for (int i = 0; i < names.size(); i++) {
            byte[] nb = names.get(i).getBytes(UTF8);
            dos.writeShort(nb.length);
            dos.write(nb);
            byte[] data = datas.get(i);
            dos.writeInt(data.length);
            dos.write(data);
        }
        dos.flush();
        return elfEncryptChunked(baos.toByteArray(), 4 * 1024 * 1024);
    }

    public static byte[] elfEncryptChunked(byte[] input, int chunkSize) throws Exception {
        int totalChunks = (input.length + chunkSize - 1) / chunkSize;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(totalChunks);
        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, input.length);
            int len = end - start;
            byte[] chunk = new byte[len];
            System.arraycopy(input, start, chunk, 0, len);
            byte[] encrypted = elfEncrypt(chunk);
            dos.writeInt(encrypted.length);
            dos.write(encrypted);
        }
        dos.flush();
        byte[] result = baos.toByteArray();
        dos.close();
        return result;
    }

    private void packBlobV4(File patchedDir, Map<String, File> encFileByDex, File outBlob)
            throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outBlob))) {
            dos.writeBytes(ELF_MAGIC);
            dos.writeByte(1); dos.writeByte(1); dos.writeByte(1); dos.writeByte(0);
            dos.writeBytes(BLOB_MAGIC_V2);
            dos.writeInt(BLOB_VERSION);

            File[] arr = patchedDir.listFiles();
            List<File> list = new ArrayList<>();
            if (arr != null) {
                Arrays.sort(arr);
                for (File f : arr) {
                    if (f.isFile() && f.getName().endsWith(".dex")) list.add(f);
                }
            }

            dos.writeInt(list.size());
            for (File f : list) {
                byte[] nb = f.getName().getBytes(UTF8);
                byte[] dd = readFileToByteArray(f);
                dos.writeShort(nb.length); dos.write(nb);
                dos.writeInt(dd.length); dos.write(dd);
            }

            List<Map.Entry<String, File>> validEntries = new ArrayList<>();
            for (Map.Entry<String, File> e : encFileByDex.entrySet()) {
                File f = e.getValue();
                if (f != null && f.exists() && f.length() > 0) {
                    validEntries.add(e);
                }
            }

            dos.writeInt(validEntries.size());
            for (Map.Entry<String, File> e : validEntries) {
                byte[] nb = e.getKey().getBytes(UTF8);
                byte[] bl = readFileToByteArray(e.getValue());
                dos.writeShort(nb.length); dos.write(nb);
                dos.writeInt(bl.length); dos.write(bl);
            }

            dos.writeInt(0xDEADBEEF);
            dos.writeLong(0L);
        }
        System.out.println("V4 blob: " + outBlob.length() + " bytes");
    }

    public static byte[] elfEncrypt(byte[] input) throws Exception {
        byte[] xorKey = dynXorKey(input.length);
        byte[] xorEnc = new byte[input.length];
        for (int i = 0; i < input.length; i++)
            xorEnc[i] = (byte) (input[i] ^ xorKey[i % xorKey.length]);

        byte[] keyBytes = AES_KEY_STR.getBytes(UTF8);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(xorEnc);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(iv.length);
        dos.write(iv);
        dos.writeShort(xorKey.length);
        dos.write(xorKey);
        dos.writeInt(ct.length);
        dos.write(ct);
        dos.flush();
        byte[] result = baos.toByteArray();
        dos.close();
        return result;
    }

    public static byte[] elfDecrypt(byte[] data) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        int ivLen = dis.readUnsignedByte();
        byte[] iv = new byte[ivLen];
        dis.readFully(iv);
        int xkLen = dis.readUnsignedShort();
        byte[] xk = new byte[xkLen];
        dis.readFully(xk);
        int ctLen = dis.readInt();
        byte[] ct = new byte[ctLen];
        dis.readFully(ct);
        dis.close();

        byte[] keyBytes = AES_KEY_STR.getBytes(UTF8);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new GCMParameterSpec(128, iv));
        byte[] dec = cipher.doFinal(ct);
        byte[] res = new byte[dec.length];
        for (int i = 0; i < dec.length; i++)
            res[i] = (byte) (dec[i] ^ xk[i % xk.length]);
        return res;
    }

    private static byte[] dynXorKey(int seed) {
        byte[] k = new byte[32];
        SecureRandom r = new SecureRandom();
        r.setSeed((long) seed ^ 0xDEADBEEFL);
        r.nextBytes(k);
        for (int i = 0; i < k.length; i++)
            k[i] = (byte) (k[i] ^ (i * 0x37));
        return k;
    }

    public static String sha256Hex(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(readFileToByteArray(file));
        StringBuilder sb = new StringBuilder(64);
        for (byte b : hash) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    public static void encryptToFile(String plainText, File outFile) throws Exception {
        String s = (plainText != null) ? plainText.trim() : "";
        writeByteArrayToFile(outFile, elfEncrypt(s.getBytes(UTF8)));
    }

    public static void generateAppSo(File stubDexFile, int protectedDexCount, File outFile)
            throws Exception {
        if (protectedDexCount < 1) protectedDexCount = 1;
        StringBuilder sb = new StringBuilder();
        sb.append("xuanDun_JiaguV2!\n");
        sb.append("Generated by xuanDun Guard\n");
        sb.append("Format: OLLVM\n");
        sb.append("Totall Dex File : 1\n");
        sb.append("Original Dex File : ").append(protectedDexCount).append("\n");
        sb.append("#\n");
        sb.append("classes.dex=").append(sha256Hex(stubDexFile)).append("\n");
        writeByteArrayToFile(outFile, elfEncrypt(sb.toString().getBytes(UTF8)));
    }

    public static void generateAppSo(File stubDexFile, File outFile) throws Exception {
        generateAppSo(stubDexFile, 1, outFile);
    }

    public static void generateAppSo(List<File> stubDexFiles, int protectedDexCount, File outFile)
            throws Exception {
        if (protectedDexCount < 1) protectedDexCount = 1;
        StringBuilder sb = new StringBuilder();
        sb.append("xuanDun_JiaguV2!\n");
        sb.append("Generated by xuanDun Guard\n");
        sb.append("Format: OLLVM\n");
        sb.append("Totall Dex File : 1\n");
        sb.append("Original Dex File : ").append(protectedDexCount).append("\n");
        sb.append("#\n");
        for (File f : stubDexFiles) {
            sb.append(f.getName()).append("=").append(sha256Hex(f)).append("\n");
        }
        writeByteArrayToFile(outFile, elfEncrypt(sb.toString().getBytes(UTF8)));
    }

    private List<File> extractDex(String apkPath, File destDir) throws IOException {
        List<File> list = new ArrayList<>();
        try (ZipFile zip = new ZipFile(apkPath)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.matches("classes\\d*\\.dex")) {
                    File out = new File(destDir, name);
                    copyInputStreamToFile(zip.getInputStream(entry), out);
                    list.add(out);
                }
            }
        }
        list.sort(new Comparator<File>() {
            public int compare(File a, File b) {
                String na = a.getName(), nb = b.getName();
                int ia = na.equals("classes.dex") ? 1
                        : Integer.parseInt(na.replace("classes", "").replace(".dex", ""));
                int ib = nb.equals("classes.dex") ? 1
                        : Integer.parseInt(nb.replace("classes", "").replace(".dex", ""));
                return Integer.compare(ia, ib);
            }
        });
        return list;
    }

    private void log(String msg) {
        if (logger != null) logger.onLog(msg);
        else System.out.println(msg);
    }
}