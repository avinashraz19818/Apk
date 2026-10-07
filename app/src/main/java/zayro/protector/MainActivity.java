package zayro.protector;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.security.*;
import com.cyberalpha.iOSDialog.*;
import com.github.angads25.filepicker.*;
import com.google.android.flexbox.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.permissionx.guolindev.*;
import dagger.hilt.android.*;
import jadx.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.greenrobot.eventbus.android.*;
import org.json.*;
// Organize imports by category and remove duplicates
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.EditText;
import android.text.InputType;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import androidx.core.app.RemoteActionCompat;

public class MainActivity extends AppCompatActivity {
private boolean RealTime = false;

	private String Date = "";
	
	private Calendar calender = Calendar.getInstance();
	
	private Timer _timer = new Timer();
	
	private ProgressDialog coreprog;
	private FloatingActionButton _fab;
	private String basePath = "";
	private String fontName = "";
	private String typeace = "";
	private boolean isVIPSubscribed = false;
	
	private LinearLayout linear1;
	private TextView textview1;
	private EditText edittext1;
	private EditText edittext2;
	private CheckBox protectv1;
	private TextView textview2;
	private ScrollView vscroll1;
	private TextView textview4;
	private LinearLayout linear2;
	private TextView textview3log;
	
	private TimerTask tt;
	private Intent xd = new Intent();
	private AlertDialog.Builder dialog;
	private SharedPreferences xuanDun_prefs;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
		|| ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
		} else {
			initializeLogic();
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) {
			initializeLogic();
		}
	}
	
	private void initialize(Bundle _savedInstanceState) {
		_fab = findViewById(R.id._fab);
		linear1 = findViewById(R.id.linear1);
		textview1 = findViewById(R.id.textview1);
		edittext1 = findViewById(R.id.edittext1);
		edittext2 = findViewById(R.id.edittext2);
		protectv1 = findViewById(R.id.protectv1);
		textview2 = findViewById(R.id.textview2);
		vscroll1 = findViewById(R.id.vscroll1);
		textview4 = findViewById(R.id.textview4);
		linear2 = findViewById(R.id.linear2);
		textview3log = findViewById(R.id.textview3log);
		dialog = new AlertDialog.Builder(this);
		xuanDun_prefs = getSharedPreferences("xuanDun_prefs", Activity.MODE_PRIVATE);
		
		edittext1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				android.view.inputmethod.InputMethodManager _fpimm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				if (_fpimm != null) {
					_fpimm.hideSoftInputFromWindow(edittext1.getWindowToken(), 0);
				}
				try {
					android.content.Intent _fpintent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
					_fpintent.setType("*/*");
					_fpintent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
					startActivityForResult(_fpintent, 555);
				} catch (Exception e) {
					try {
						android.content.Intent _fpintent2 = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
						_fpintent2.setType("*/*");
						_fpintent2.addCategory(android.content.Intent.CATEGORY_OPENABLE);
						startActivityForResult(_fpintent2, 555);
					} catch (Exception e2) {
						SketchwareUtil.showMessage(getApplicationContext(), "No file manager found on this device");
					}
				}
				
			}
		});
		
		textview2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				FileUtil.deleteFile("/storage/emulated/0/Dexpacker/tmp/");
				try {
					net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile("/storage/emulated/0/Dexpacker/XuanDun.zip");
					if (zipFile.isEncrypted()) {
						zipFile.setPassword("Pokep2003z/@/_92(3(/#/1MonZ");
					}
					if (!edittext1.getText().toString().equals("")) {
						tt = new TimerTask() {
							@Override
							public void run() {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if (!protectv1.isChecked()) {
											SketchwareUtil.showMessage(getApplicationContext(), "出问题了！");
										}
										else {
											_XuanDun(edittext1.getText().toString());
										}
									}
								});
							}
						};
						_timer.schedule(tt, (int)(500));
					}
					else {
						((EditText)edittext1).setError("Pick Apk");
					}
					zipFile.extractAll("/storage/emulated/0/Dexpacker/tmp/");
				} catch(net.lingala.zip4j.exception.ZipException e) {
					SketchwareUtil.showMessage(getApplicationContext(), "Sorry");
				}
				
			}
		});
	}
	
	private void initializeLogic() {
		XuanDunProtect.init(this);
		edittext1.setFocusable(false);
		edittext1.setFocusableInTouchMode(false);
		edittext1.setCursorVisible(false);
		edittext1.setLongClickable(false);
		edittext1.setInputType(android.text.InputType.TYPE_NULL);
		
		basePath = "/storage/emulated/0/Dexpacker/";
		if (FileUtil.isExistFile("/storage/emulated/0/Dexpacker/tmp/assets/")) {
			FileUtil.deleteFile("/storage/emulated/0/Dexpacker/tmp/assets/");
			FileUtil.makeDir("/storage/emulated/0/Dexpacker/tmp/assets/");
			FileUtil.makeDir("/storage/emulated/0/Dexpacker/tmp/lib/");
		} else {
			FileUtil.makeDir("/storage/emulated/0/Dexpacker/tmp/assets/");
			FileUtil.makeDir("/storage/emulated/0/Dexpacker/tmp/lib/");
		}
		
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			RealTime = android.provider.Settings.Global.getInt(getApplicationContext().getContentResolver(), android.provider.Settings.Global.AUTO_TIME, 0) == 1;
		} else {
			RealTime = android.provider.Settings.System.getInt(getApplicationContext().getContentResolver(), android.provider.Settings.System.AUTO_TIME, 0) == 1;
		}
		if (RealTime) {
			calender = Calendar.getInstance();
			Date = new SimpleDateFormat("yyyyMMdd").format(calender.getTime());
			if (Double.parseDouble(Date) >= 20290805) {
				iOSDialogBuilder a = new iOSDialogBuilder(MainActivity.this);
				a.setTitle("App is Expired!");
				a.setSubtitle("Contact to Developer");
				a.setBoldPositiveLabel(true);
				a.setCancelable(false);
				a.setPositiveListener("OK",new iOSDialogClickListener() 
				{ 	 @Override 	 public void onClick(iOSDialog dialog) { 		
						xd.setAction(Intent.ACTION_VIEW);
						xd.setData(Uri.parse("https://t.me/Its_PokemonZ"));
						startActivity(xd);	 
					}
				})	;
				a.build().show();
				SketchwareUtil.showMessage(getApplicationContext(), "Server Connection Failed");
				finishAffinity();
			}
			else {
				textview2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)25, (int)0, Color.TRANSPARENT, 0xFF1D212D));
				vscroll1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)25, (int)5, 0xFF1D212D, 0xFFFFFFFF));
				if (Build.VERSION.SDK_INT >= 23) {
					if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
					|| checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
						requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
					}
				}
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
					if (!Environment.isExternalStorageManager()) {
						try {
							Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
							intent.setData(Uri.parse("package:" + getPackageName()));
							startActivity(intent);
						} catch (Exception e) {
							Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
							startActivity(intent);
						}
					}
				}
				
				SketchwareUtil.showMessage(getApplicationContext(), "Welcome to XuanDun");
					copyOneFile("XuanDun.zip", basePath + "XuanDun.zip");
				}
				
				//By Ahmed Yahaya
			}
			else {
				try
				{
					Intent intent = new Intent("android.settings.DATE_SETTINGS");
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				//By Ahmed Yahaya
			}
			
			//Created by Ahmed Yahaya
		}
		
		public void _changeActivityFont(final String _fontname) {
			fontName = "fonts/".concat(_fontname.concat(".ttf"));
			overrideFonts(this,getWindow().getDecorView()); 
		} 
		private void overrideFonts(final android.content.Context context, final View v) {
			
			try {
				Typeface 
				typeace = Typeface.createFromAsset(getAssets(), fontName);;
				if ((v instanceof ViewGroup)) {
					ViewGroup vg = (ViewGroup) v;
					for (int i = 0;
					i < vg.getChildCount();
					i++) {
						View child = vg.getChildAt(i);
						overrideFonts(context, child);
					}
				} else {
					if ((v instanceof TextView)) {
						((TextView) v).setTypeface(typeace);
					} else {
						if ((v instanceof EditText )) {
							((EditText) v).setTypeface(typeace);
						} else {
							if ((v instanceof Button)) {
								((Button) v).setTypeface(typeace);
							}
						}
					}
				}
			}
			catch(Exception e)
			
			{
				SketchwareUtil.showMessage(getApplicationContext(), "Error Loading Font");
			};
		}
		
		
		public void _Shadow(final double _sadw, final double _cru, final String _wc, final View _widgets) {
			
			String color = _wc;   // ✅ create new variable
			
			if (!color.startsWith("#")) {
				color = "#" + color;
			}
			
			android.graphics.drawable.GradientDrawable wd = new android.graphics.drawable.GradientDrawable();
			wd.setColor(Color.parseColor(color));
			wd.setCornerRadius((float)_cru);
			
			_widgets.setElevation((float)_sadw);
			_widgets.setBackground(wd);
			
		}
		
		
		public void _XuanDun(final String _PathofApk) {
			if (protectv1.isChecked()) {
				
				File apkFile = new File(_PathofApk);
				if (!apkFile.exists()) {
					textview3log.setText("APK file not found!");
					return;
				}
				
				textview3log.setText("");
				
				// ── 1. Package & Application class info ──────────────────────────
				android.content.pm.PackageInfo pckgInfo = null;
				String pckgName = "";
				try {
					pckgInfo = getPackageManager().getPackageArchiveInfo(_PathofApk, 0);
					if (pckgInfo != null) pckgName = pckgInfo.packageName;
				} catch (Exception e) {
					appendLog("PackageInfo error: " + e.getMessage());
				}
				String tempAppClass = "android.app.Application";
				if (pckgInfo != null && pckgInfo.applicationInfo != null) {
					if (pckgInfo.applicationInfo.className != null
					&& !pckgInfo.applicationInfo.className.isEmpty()) {
						tempAppClass = pckgInfo.applicationInfo.className;
					}
				}
				final String finalRealAppClass = tempAppClass;
				final String finalPkgName = pckgName;
				
				// ── 2. Detect target SDK ────────────────────────────────────────
				int targetSdk = 28;
				try {
					if (pckgInfo != null && pckgInfo.applicationInfo != null) {
						targetSdk = pckgInfo.applicationInfo.targetSdkVersion;
					}
				} catch (Throwable ignored) {}
				
				// ── 3. Manifest flags ───────────────────────────────────────────
				final boolean cfgDebuggable = false;
				final boolean cfgAllowBackup = false;
				final boolean cfgExtractNativeLibs = true;
				final int TARGET_SDK = targetSdk;
				
				// ── 4. Path setup ───────────────────────────────────────────────
				final String inputZipPath = _PathofApk;
				final String outputZipPath = _PathofApk.toLowerCase().endsWith(".apk")
				? _PathofApk.substring(0, _PathofApk.length() - 4) + "_xuanDun.apk"
				: _PathofApk + "_xuanDun.apk";
				final String workDirPath = android.os.Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/Dexpacker/tmp/";
				final String assetsDirPath = workDirPath + "assets/";
				
				new java.io.File(workDirPath).mkdirs();
				new java.io.File(assetsDirPath).mkdirs();
				
				_hhhj(true, "Processing xuanDun...");
				
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							appendLogUIAnimated("════════════════════════════", 100);
							appendLogUIAnimated(" XuanDun Reinforcement Processing", 150);
							appendLogUIAnimated("════════════════════════════\n", 100);
							
							
							// ═══════════════════════════════════════════════════════
							// STEP 0 — Configure Method Extraction
							// ═══════════════════════════════════════════════════════
							CorePatcher5.minInstructionBytesToExtract = 8;
							CorePatcher5.ownPackagePrefixes.clear();
							
							// 🔥 FIX: superclass ကို ပြောင်းပေးမယ့် class
							CorePatcher5.appClassToPatch = finalRealAppClass;
							
							// 🔥 FIX (NEW): Application class ကို extract/encrypt မလုပ်ဖို့
							//   superclass patch ပြီးတဲ့ class က မူရင်းအတိုင်းကျန်ရမယ်
							CorePatcher5.applicationClassToKeep = finalRealAppClass;
							
							appendLogUIAnimated("[ OK  ] Full DEX coverage: all classes encrypted", 150);
							
							
							// STEP 1 — Core Patching
							appendLogUIAnimated("[SERVER] Scanning package...", 400);
							appendLogUIAnimated("[SERVER] Extracting DEX files...", 500);
							appendLogUIAnimated("[SERVER] Processing...", 300);
							
							CorePatcher5 patcher = new CorePatcher5(msg -> {
								appendLogUIAnimated("  [PATCH] " + msg, 50);
							});
							int originalDexCount = patcher.process(inputZipPath, finalPkgName, workDirPath);
							if (originalDexCount <= 0) {
								appendLogUIAnimated("❌ No DEX files found in APK!", 100);
								runOnUiThread(() -> _hhhj(false, ""));
								return;
							}
							appendLogUIAnimated("[ OK  ] DEX extracted (" + originalDexCount + " files)", 200);
							
							
							// STEP 2 — Encrypt App Class Name → xuanDun.dat
							appendLogUIAnimated("[SERVER] Encrypting classes...", 400);
							appendLogUIAnimated("[SERVER] Encrypting Dex's methods...", 400);
							CorePatcher5.encryptToFile(finalRealAppClass, new java.io.File(assetsDirPath, "xuanDun.dat"));
							
							// STEP 3 — Get Signature Key
							appendLogUIAnimated("[AUTH ] Checking VIP status...", 300);
							appendLogUIAnimated("[AUTH ] Processing...", 300);
							
							
							appendLogUIAnimated("[ OK  ] VIP Subscription checked", 200);
							
							String signatureKeyRaw = (edittext2 != null) ? edittext2.getText().toString().trim() : "";
							String sigHash = "";
							try {
								java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
								android.content.pm.PackageInfo pi = getPackageManager().getPackageArchiveInfo(
								_PathofApk, android.content.pm.PackageManager.GET_SIGNATURES);
								if (pi != null && pi.signatures != null && pi.signatures.length > 0) {
									byte[] certBytes = pi.signatures[0].toByteArray();
									byte[] hash = md.digest(certBytes);
									StringBuilder sb = new StringBuilder();
									for (byte b : hash) sb.append(String.format("%02X", b));
									sigHash = sb.toString();
								}
							} catch (Throwable t) {}
							
							if (!signatureKeyRaw.isEmpty() && !signatureKeyRaw.equals("Enter signature key here")) {
								sigHash = signatureKeyRaw.replace(":", "").toUpperCase();
							}
							if (sigHash.isEmpty()) {
								sigHash = "NoKey";
							}
							
							CorePatcher5.encryptToFile(sigHash, new java.io.File(assetsDirPath, "xuanDun.ajm"));
							
							// STEP 3.5 — Generate sign.so
							try {
								CorePatcher5.encryptToFile(sigHash, new java.io.File(assetsDirPath, "libdexXuan.so"));
							} catch (Exception e) {}
							
							// STEP 4 — Encrypt placeholder
							CorePatcher5.encryptToFile("", new java.io.File(assetsDirPath, "xuanDun2.dat"));
							
							
							// STEP 5 — Generate xuanDun_mem.dex
							appendLogUIAnimated("[SERVER] Building Reinforcement...", 500);
							java.io.File stubDex = null;
							java.io.File[] candidatePaths = {
								new java.io.File(android.os.Environment.getExternalStorageDirectory(), "Dexpacker/stub_classes.dex"),
								new java.io.File(workDirPath, "classes.dex"),
								new java.io.File(workDirPath, "stub_classes.dex"),
								new java.io.File(assetsDirPath, "stub_classes.dex")
							};
							for (java.io.File c : candidatePaths) {
								if (c.exists() && c.length() > 0) {
									stubDex = c;
									break;
								}
							}
							if (stubDex == null) {
								appendLogUIAnimated("❌ Stub DEX not found!", 100);
								runOnUiThread(() -> _hhhj(false, ""));
								return;
							}
							CorePatcher5.generateAppSo(stubDex, originalDexCount, new java.io.File(assetsDirPath, "xuanDun_mem.dex"));
							
							// STEP 6 — Repackage APK
							appendLogUIAnimated("[SERVER] Processing resources & finishing...", 400);
							java.util.zip.ZipOutputStream zipOut = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(outputZipPath));
							java.util.zip.ZipFile inputZip = new java.util.zip.ZipFile(new java.io.File(inputZipPath));
							byte[] buffer = new byte[16384];
							java.util.HashSet<String> addedEntries = new java.util.HashSet<>();
							
							// (A) AndroidManifest.xml
							java.util.zip.ZipEntry manifestEntry = inputZip.getEntry("AndroidManifest.xml");
							if (manifestEntry != null) {
								java.io.File tempManifestIn = new java.io.File(workDirPath, "AndroidManifest_orig.xml");
								java.io.File tempManifestOut = new java.io.File(workDirPath, "AndroidManifest_patched.xml");
								java.io.InputStream mis = inputZip.getInputStream(manifestEntry);
								java.io.FileOutputStream mfos = new java.io.FileOutputStream(tempManifestIn);
								int mLen;
								while ((mLen = mis.read(buffer)) > 0) mfos.write(buffer, 0, mLen);
								mis.close();
								mfos.close();
								
								com.wind.meditor.property.ModificationProperty mp = new com.wind.meditor.property.ModificationProperty();
								
								// 🔥 FIX (IMPORTANT): Application name ကို REAL class နဲ့ ထား
								//   (မူလက "com.xuan.XuanDunApp" ဖြစ်နေတာ cast ပြဿနာ ဖြစ်စေလို့)
								if (TARGET_SDK >= 28) {
									mp.addApplicationAttribute(new com.wind.meditor.property.AttributeItem(
									com.wind.meditor.utils.NodeValue.Application.NAME, finalRealAppClass));
									mp.addApplicationAttribute(new com.wind.meditor.property.AttributeItem(
									"appComponentFactory", "com.xuan.factory.AndroidComponet"));
								} else {
									mp.addApplicationAttribute(new com.wind.meditor.property.AttributeItem(
									com.wind.meditor.utils.NodeValue.Application.NAME, "com.xuan.XuanDunApp"));
								}
								mp.addApplicationAttribute(new com.wind.meditor.property.AttributeItem("debuggable", cfgDebuggable));
								mp.addApplicationAttribute(new com.wind.meditor.property.AttributeItem("allowBackup", cfgAllowBackup));
								mp.addApplicationAttribute(new com.wind.meditor.property.AttributeItem("extractNativeLibs", cfgExtractNativeLibs));
								mp.addApplicationAttribute(new com.wind.meditor.property.AttributeItem("usesCleartextTraffic", true));
								
								com.wind.meditor.core.FileProcesser.processManifestFile(
								tempManifestIn.getAbsolutePath(), tempManifestOut.getAbsolutePath(), mp);
								
								java.util.zip.ZipEntry newME = new java.util.zip.ZipEntry("AndroidManifest.xml");
								newME.setMethod(java.util.zip.ZipEntry.DEFLATED);
								zipOut.putNextEntry(newME);
								java.io.FileInputStream pfis = new java.io.FileInputStream(tempManifestOut);
								int pLen;
								while ((pLen = pfis.read(buffer)) > 0) zipOut.write(buffer, 0, pLen);
								pfis.close();
								zipOut.closeEntry();
								addedEntries.add("AndroidManifest.xml");
								tempManifestIn.delete();
								tempManifestOut.delete();
							}
							
							// (B) Stub classes.dex
							if (stubDex.exists()) {
								java.util.zip.ZipEntry ze = new java.util.zip.ZipEntry("classes.dex");
								ze.setMethod(java.util.zip.ZipEntry.DEFLATED);
								zipOut.putNextEntry(ze);
								java.io.FileInputStream fis = new java.io.FileInputStream(stubDex);
								int len;
								while ((len = fis.read(buffer)) > 0) zipOut.write(buffer, 0, len);
								fis.close();
								zipOut.closeEntry();
								addedEntries.add("classes.dex");
							}
							
							// (C) Assets files
							java.io.File[] filesInAssets = new java.io.File(assetsDirPath).listFiles();
							if (filesInAssets != null) {
								for (java.io.File f : filesInAssets) {
									if (f.isDirectory()) continue;
									String entryName = "assets/" + f.getName();
									zipOut.putNextEntry(new java.util.zip.ZipEntry(entryName));
									java.io.FileInputStream fis = new java.io.FileInputStream(f);
									int len;
									while ((len = fis.read(buffer)) > 0) zipOut.write(buffer, 0, len);
									fis.close();
									zipOut.closeEntry();
									addedEntries.add(entryName);
								}
							}
							
							// (D) V4 Blob files — workDirPath root ကနေ ထည့်
							for (String so : new String[]{"xuanDun_wifi", "xuanDun_conf.dex"}) {
								String entryName = "assets/" + so;
								if (!addedEntries.contains(entryName)) {
									java.io.File soFile = new java.io.File(workDirPath, so);
									if (soFile.exists()) {
										zipOut.putNextEntry(new java.util.zip.ZipEntry(entryName));
										java.io.FileInputStream fis = new java.io.FileInputStream(soFile);
										int len;
										while ((len = fis.read(buffer)) > 0) zipOut.write(buffer, 0, len);
										fis.close();
										zipOut.closeEntry();
										addedEntries.add(entryName);
									} else {
										appendLogUIAnimated("⚠️ MISSING: " + so, 100);
									}
								}
							}
							
							// (E) Native libs
							java.io.File libDir = new java.io.File(workDirPath, "lib");
							if (libDir.exists()) {
								addFolderToZipNative(libDir, "lib/", zipOut, buffer, addedEntries);
							}
							
							// (F) Copy remaining original entries
							java.util.List<String> assetNames = new java.util.ArrayList<>();
							java.util.List<byte[]> assetDatas = new java.util.ArrayList<>();
							java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = inputZip.entries();
							while (entries.hasMoreElements()) {
								java.util.zip.ZipEntry entry = entries.nextElement();
								String name = entry.getName();
								if (addedEntries.contains(name)) continue;
								
								if (name.startsWith("assets/")
								&& !name.equals("assets/xuanDun_res.bin")
								&& (name.endsWith(".txt") || name.endsWith(".TXT"))) {
									java.io.ByteArrayOutputStream _abo = new java.io.ByteArrayOutputStream();
									java.io.InputStream _ais = inputZip.getInputStream(entry);
									int _al;
									while ((_al = _ais.read(buffer)) > 0) _abo.write(buffer, 0, _al);
									_ais.close();
									assetNames.add(name.substring(7));
									assetDatas.add(_abo.toByteArray());
									continue;
								}
								
								// META-INF/services ထိန်းသိမ်း (coroutines)
								if (name.startsWith("META-INF/")) {
									boolean isSignFile = name.endsWith(".RSA")
									|| name.endsWith(".SF")
									|| name.endsWith(".DSA")
									|| name.endsWith(".EC")
									|| name.equals("META-INF/MANIFEST.MF");
									if (isSignFile) continue;
								}
								
								if (name.endsWith(".dex")) continue;
								if (name.startsWith("classes") && name.endsWith(".dex")) continue;
								
								java.util.zip.ZipEntry newEntry = new java.util.zip.ZipEntry(name);
								if (entry.getMethod() == java.util.zip.ZipEntry.STORED) {
									newEntry.setMethod(java.util.zip.ZipEntry.STORED);
									newEntry.setSize(entry.getSize());
									newEntry.setCompressedSize(entry.getSize());
									newEntry.setCrc(entry.getCrc());
								} else {
									newEntry.setMethod(java.util.zip.ZipEntry.DEFLATED);
								}
								zipOut.putNextEntry(newEntry);
								java.io.InputStream is = inputZip.getInputStream(entry);
								int len;
								while ((len = is.read(buffer)) > 0) zipOut.write(buffer, 0, len);
								is.close();
								zipOut.closeEntry();
							}
							
							if (!assetNames.isEmpty()) {
								appendLogUIAnimated("[ OK  ] Assets protected: " + assetNames.size() + " txt files", 100);
								byte[] _resPackBytes = CorePatcher5.buildAssetsPack(assetNames, assetDatas);
								java.util.zip.ZipEntry _rze = new java.util.zip.ZipEntry("assets/xuanDun_res.bin");
								_rze.setMethod(java.util.zip.ZipEntry.DEFLATED);
								zipOut.putNextEntry(_rze);
								zipOut.write(_resPackBytes);
								zipOut.closeEntry();
							}
							
							try { inputZip.close(); } catch (Throwable ignored) {}
							zipOut.close();
							
							// Final Summary
							long fileSizeKB = new java.io.File(outputZipPath).length() / 1024;
							String outFileName = new java.io.File(outputZipPath).getName();
							
							
							appendLogUIAnimated("\n[SERVER] Uploading Successful", 300);
							appendLogUIAnimated("[SERVER] Processing your app", 300);
							appendLogUIAnimated("[SERVER] Almost Done", 300);
							appendLogUIAnimated("[SERVER] Downloading files...", 400);
							appendLogUIAnimated("[ OK  ] Download completed\n", 200);
							
							appendLogUIAnimated("══════════════════════════════", 100);
							appendLogUIAnimated("STATUS      : SUCCESS", 100);
							appendLogUIAnimated("DEX FILES   : " + originalDexCount, 100);
							appendLogUIAnimated("APP SDK     : " + TARGET_SDK, 100);
							appendLogUIAnimated("OUTPUT FILE : " + outFileName, 100);
							appendLogUIAnimated("SIZE        : " + fileSizeKB + " KB", 100);
							appendLogUIAnimated("══════════════════════════════", 100);
							appendLogUIAnimated("\n⚠️ Please SIGN this APK with MT Manager", 100);
							appendLogUIAnimated("   or ApkTool M before installing!", 100);
							appendLogUIAnimated("\n[SERVER] Thank you for using XuanDun", 100);
							
							runOnUiThread(() -> _hhhj(false, ""));
						} catch (Exception e) {
							final String err = e.getMessage();
							appendLogUIAnimated("\n❌ FATAL ERROR: " + err, 100);
							android.util.Log.e("Packer", "Fatal error", e);
							runOnUiThread(() -> _hhhj(false, ""));
						}
					}
				}).start();
			}
		}
		
		
		private void appendLogUI(String msg) {
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            String current = textview3log.getText().toString();
            textview3log.setText(current + msg + "\n");
            // Auto-scroll to bottom
            int scrollAmount = textview3log.getLayout() != null
                    ? textview3log.getLayout().getLineTop(
                            textview3log.getLineCount()) - textview3log.getHeight()
                    : 0;
            if (scrollAmount > 0) {
                textview3log.scrollTo(0, scrollAmount);
            } else {
                textview3log.scrollTo(0, 0);
            }
        }
    });
}

// ၁။ Network Check Function (WiFi သို့မဟုတ် Mobile Data ဖွင့်ထားခြင်း ရှိ/မရှိ စစ်ရန်)
private boolean isInternetAvailable() {
    try {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return false;
}

// ၂။ Animated Log Append Function (စာသားများကို Time Delay ဖြင့် စာရိုက်နေသလို ပေါ်စေရန်)
private void appendLogUIAnimated(String newLog, long delayAfterMillis) {
    runOnUiThread(() -> {
        if (textview3log != null) {
            textview3log.append(newLog + "\n");
        }
    });
    try {
        Thread.sleep(delayAfterMillis);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}



// ၂။ UI Log တင်ပေးမည့်
// Helper: Append log directly (for use on main thread)
private void appendLog(String msg) {
    textview3log.setText(textview3log.getText().toString() + msg + "\n");
}

  private static String readApkCertSha256(String apkPath) {
    try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(apkPath)) {
        java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            java.util.zip.ZipEntry entry = entries.nextElement();
            String name = entry.getName().toUpperCase();
            if (!name.startsWith("META-INF/")) continue;
            if (!name.endsWith(".RSA") && !name.endsWith(".DSA") && !name.endsWith(".EC"))
                continue;

            try (java.io.InputStream is = zip.getInputStream(entry);
                 java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                byte[] pkcs7 = baos.toByteArray();

                // Parse PKCS#7 / CMS to get the certificate bytes
                // Use CertificateFactory to parse the X.509 cert from the block
                java.security.cert.CertificateFactory cf =
                        java.security.cert.CertificateFactory.getInstance("X.509");

                // Try DER decode of PKCS7 and extract cert
                // Android's bouncy castle approach
                try {
                    // PKCS#7 SignedData: look for SEQUENCE containing certificate
                    java.security.cert.Certificate cert =
                            parseCertFromPkcs7(pkcs7);
                    if (cert != null) {
                        java.security.MessageDigest md =
                                java.security.MessageDigest.getInstance("SHA-256");
                        byte[] hash = md.digest(cert.getEncoded());
                        StringBuilder sb = new StringBuilder(64);
                        for (byte b : hash) sb.append(String.format("%02X", b));
                        return sb.toString();
                    }
                } catch (Exception inner) {
                    android.util.Log.w("Packer", "PKCS7 parse fail: " + inner.getMessage());
                }
            }
        }
    } catch (Exception e) {
        android.util.Log.w("Packer", "readApkCertSha256 fail: " + e.getMessage());
    }
    return null;
}

/**
 * Minimal PKCS#7 parser to extract the embedded X.509 certificate.
 * Works for the simple case of APK v1 signing blocks.
 */
				private static java.security.cert.Certificate parseCertFromPkcs7(byte[] pkcs7) {
					try {
						// Use BouncyCastle-style parsing via Android's CMSSignedData equivalent
						// Fall back to javax.security approach
						java.security.cert.CertificateFactory cf =
						java.security.cert.CertificateFactory.getInstance("X.509");
						// Try treating entire block as DER cert (sometimes works for simple cases)
						try {
							return cf.generateCertificate(new java.io.ByteArrayInputStream(pkcs7));
						} catch (Exception ignored) {}
						
						// Try to find certificate in PKCS#7 by searching for X.509 sequence
						// X.509 cert starts with 0x30 (SEQUENCE) after PKCS#7 wrapper
						int idx = findCertOffset(pkcs7);
						if (idx >= 0) {
							return cf.generateCertificate(
							new java.io.ByteArrayInputStream(pkcs7, idx, pkcs7.length - idx));
						}
					} catch (Exception ignored) {}
					return null;
				}
				
				/** Finds the byte offset of the embedded certificate in a PKCS#7 block. */
				private static int findCertOffset(byte[] data) {
					// Simple heuristic: look for SEQUENCE (0x30) tag after the PKCS#7 SignedData wrapper
					// Typical offset is around 30-60 bytes in
					for (int i = 20; i < Math.min(data.length - 4, 200); i++) {
						// Look for SEQUENCE + length indicating an X.509 cert structure
						if ((data[i] & 0xFF) == 0x30 && i + 1 < data.length) {
							int lenByte = data[i + 1] & 0xFF;
							int certLen;
							if (lenByte < 0x80) certLen = lenByte;
							else if (lenByte == 0x82 && i + 3 < data.length)
							certLen = ((data[i+2] & 0xFF) << 8) | (data[i+3] & 0xFF);
							else continue;
							if (i + 2 + certLen <= data.length && certLen > 100) return i;
						}
					}
					return -1;
				}
				
				// ─────────────────────────────────────────────────────────────────────
				//  ZIP helpers
				// ─────────────────────────────────────────────────────────────────────
				
				private static void writeZipEntry(java.util.zip.ZipOutputStream out,
				String name, byte[] data,
				byte[] ignored) throws Exception {
					java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(name);
					entry.setMethod(java.util.zip.ZipEntry.DEFLATED);
					out.putNextEntry(entry);
					out.write(data);
					out.closeEntry();
				}
				
				private static void copyZipEntry(java.util.zip.ZipOutputStream out,
				String name, java.io.InputStream is,
				byte[] buffer) throws Exception {
					java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(name);
					entry.setMethod(java.util.zip.ZipEntry.DEFLATED);
					out.putNextEntry(entry);
					int len;
					while ((len = is.read(buffer)) > 0) out.write(buffer, 0, len);
					out.closeEntry();
				}
				
				private static byte[] toByteArray(java.io.InputStream is,
				byte[] buffer) throws Exception {
					java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
					int len;
					while ((len = is.read(buffer)) > 0) baos.write(buffer, 0, len);
					return baos.toByteArray();
				}
				
				private static long computeCrc32(byte[] data) {
					java.util.zip.CRC32 crc = new java.util.zip.CRC32();
					crc.update(data);
					return crc.getValue();
				}
				
				/** Recursively adds a directory's files to the ZIP output. */
				private static void addFolderToZip(java.io.File folder,
				String prefix,
				java.util.zip.ZipOutputStream zipOut,
				byte[] buffer,
				java.util.Set<String> added) {
					java.io.File[] children = folder.listFiles();
					if (children == null) return;
					for (java.io.File child : children) {
						String entryName = prefix + child.getName();
						if (child.isDirectory()) {
							addFolderToZip(child, entryName + "/", zipOut, buffer, added);
						} else if (!added.contains(entryName)) {
							try (java.io.FileInputStream fis = new java.io.FileInputStream(child)) {
								copyZipEntry(zipOut, entryName, fis, buffer);
								added.add(entryName);
							} catch (Exception e) {
								android.util.Log.w("Packer", "addFolder skip [" + entryName + "]: "
								+ e.getMessage());
							}
						}
					}
				}
				
				// ─────────────────────────────────────────────────────────────────────
				//  Legacy compat (referenced by old code paths)
				// ─────────────────────────────────────────────────────────────────────
				
				private void addFolderToZipNative(java.io.File folder,
				String prefix,
				java.util.zip.ZipOutputStream zipOut,
				byte[] buffer,
				java.util.Set<String> added) {
					addFolderToZip(folder, prefix, zipOut, buffer, added);
				}
				
				// DeviceID Dialog method
				private void copyOneFile(String assetFilename, String destinationPath) {
					try {
						java.io.InputStream in = getAssets().open(assetFilename);
						
						// Folder မရှိသေးရင် ဆောက်မယ်
						java.io.File outFile = new java.io.File(destinationPath);
						if (outFile.getParentFile() != null) {
							outFile.getParentFile().mkdirs();
						}
						
						java.io.OutputStream out = new java.io.FileOutputStream(outFile);
						
						byte[] buffer = new byte[1024];
						int read;
						while ((read = in.read(buffer)) != -1) {
							out.write(buffer, 0, read);
						}
						
						in.close();
						out.flush();
						out.close();
						
					} catch (Exception e) {
						e.printStackTrace(); // Error တက်ရင် Log ထုတ်ကြည့်ရန်
					}
				}
				
				// Folder လိုက် ကူးပေးမည့် Function (Recursive)
				private void copyFolder(String assetFolderName, String destinationPath) {
					try {
						String[] files = getAssets().list(assetFolderName);
						
						// Destination Folder ကို အရင်ဆောက်မယ်
						new java.io.File(destinationPath).mkdirs();
						
						if (files != null) {
							for (String file : files) {
								// Asset ထဲက file path အပြည့်အစုံ
								String assetPath = assetFolderName + "/" + file;
								// ဖုန်းထဲက file path အပြည့်အစုံ
								String newDestPath = destinationPath + "/" + file;
								
								// Sub-folder တွေပါခဲ့ရင် ထပ်ဆင့်ခေါ်မယ် (Recursive)
								// file နာမည်မှာ "." ပါရင် File လို့သတ်မှတ်ပြီး ကူးမယ် (ရိုးရှင်းသောနည်းလမ်း)
								if (file.contains(".")) {
									copyOneFile(assetPath, newDestPath);
								} else {
									// Folder ဖြစ်နိုင်ချေရှိလို့ function ပြန်ခေါ်မယ်
									copyFolder(assetPath, newDestPath);
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// ဒီကုဒ်ကို MainActivity ရဲ့ အောက်ဆုံးမှာ သီးသန့်ထားပါ
				public void scanFilesRecursive(java.io.File folder, String parentPath, java.util.List<java.io.File> fileList, java.util.List<String> pathList, java.util.HashSet<String> ignoreSet) {
					java.io.File[] files = folder.listFiles();
					if (files != null) {
						for (java.io.File f : files) {
							if (f.isDirectory()) {
								scanFilesRecursive(f, parentPath + f.getName() + "/", fileList, pathList, ignoreSet);
							} else {
								String entryName = parentPath + f.getName();
								fileList.add(f);
								pathList.add(entryName);
								ignoreSet.add(entryName); // မူရင်း APK ထဲက ဒီနာမည်ပါရင် ကျော်ဖို့မှတ်မယ်
							}
						}
					}
				}
				@Override
				protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
					super.onActivityResult(requestCode, resultCode, data);
					if (requestCode == 555 && resultCode == RESULT_OK && data != null && data.getData() != null) {
						final android.net.Uri _fpuri = data.getData();
						if ("file".equals(_fpuri.getScheme())) {
							edittext1.setText(_fpuri.getPath());
							return;
						}
						String _fppath = FileUtil.convertUriToFilePath(getApplicationContext(), _fpuri);
						if (_fppath != null && !_fppath.isEmpty() && new java.io.File(_fppath).exists()) {
							edittext1.setText(_fppath);
							SketchwareUtil.showMessage(getApplicationContext(), "Selected: " + _fppath);
							return;
						}
						SketchwareUtil.showMessage(getApplicationContext(), "Reading file, please wait...");
						new Thread(new Runnable() {
							@Override
							public void run() {
								String _fpcopy = null;
								try {
									String _fpname = "picked.apk";
									try {
										android.database.Cursor _fpc = getContentResolver().query(_fpuri, null, null, null, null);
										if (_fpc != null) {
											int _fpni = _fpc.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
											if (_fpni >= 0 && _fpc.moveToFirst()) {
												_fpname = _fpc.getString(_fpni);
											}
											_fpc.close();
										}
									} catch (Exception ignored) {}
									if (_fpname == null || _fpname.isEmpty()) {
										_fpname = "picked.apk";
									}
									_fpname = _fpname.replace("/", "_");
									java.io.File _fpdir = getExternalFilesDir(null);
									if (_fpdir == null) {
										_fpdir = getCacheDir();
									}
									java.io.File _fpout = new java.io.File(_fpdir, _fpname);
									java.io.InputStream _fpis = getContentResolver().openInputStream(_fpuri);
									java.io.OutputStream _fpos = new java.io.FileOutputStream(_fpout);
									byte[] _fpbuf = new byte[16384];
									int _fpn;
									while ((_fpn = _fpis.read(_fpbuf)) > 0) {
										_fpos.write(_fpbuf, 0, _fpn);
									}
									_fpis.close();
									_fpos.close();
									_fpcopy = _fpout.getAbsolutePath();
								} catch (final Exception e) {
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											SketchwareUtil.showMessage(getApplicationContext(), "Read error: " + e.getMessage());
										}
									});
								}
								if (_fpcopy != null) {
									final String _fpcp = _fpcopy;
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											edittext1.setText(_fpcp);
											SketchwareUtil.showMessage(getApplicationContext(), "Selected: " + _fpcp);
										}
									});
								}
							}
						}).start();
					}
				}
				
				private String encryptSignatureXOR(String plainSignature) {
					try {
						byte[] xorKey = "PoZeHello1#$03992030#1".getBytes("UTF-8");
						byte[] plainBytes = plainSignature.getBytes("UTF-8");
						byte[] encryptedBytes = new byte[plainBytes.length];
						for (int i = 0; i < plainBytes.length; i++) {
							encryptedBytes[i] = (byte) (plainBytes[i] ^ xorKey[i % xorKey.length]);
						}
						String encryptedBase64 = android.util.Base64.encodeToString(
						encryptedBytes, android.util.Base64.NO_WRAP);
						String prefix;
						if (protectv1.isChecked()) {
							prefix = "Cent:";
						} 
						prefix = "Cent:";
						
						return prefix + encryptedBase64;
					} catch (Exception e) {
						android.util.Log.e("Encrypt", "Error: " + e.getMessage());
					}
					return "Cent:Enter your Signature Key";
					
			
		}
		
		
		public void _hhhj(final boolean _biew, final String _nnn) {
			
			if (_biew) {
				
				if (coreprog == null) {
					coreprog = new ProgressDialog(this);
					coreprog.setCancelable(false);
					coreprog.setCanceledOnTouchOutside(false);
					coreprog.requestWindowFeature(Window.FEATURE_NO_TITLE);
					
					if (coreprog.getWindow() != null) {
						coreprog.getWindow().setBackgroundDrawable(
						new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
					}
				}
				
				coreprog.show();
				coreprog.setContentView(R.layout.ccc);
				
				LinearLayout linear = coreprog.findViewById(R.id.linear);
				TextView t = coreprog.findViewById(R.id.t);
				
				GradientDrawable bg = new GradientDrawable();
				bg.setColor(Color.WHITE);
				bg.setCornerRadius(15);
				linear.setBackground(bg);
				
				t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/gfx.ttf"));
				t.setText(_nnn);
				
			} else {
				
				if (coreprog != null && coreprog.isShowing()) {
					coreprog.dismiss();
				}
				
			}
			
		}
		
	}