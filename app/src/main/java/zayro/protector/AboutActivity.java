package zayro.protector;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.security.*;
import com.cyberalpha.iOSDialog.*;
import com.github.angads25.filepicker.*;
import com.google.android.flexbox.*;
import com.google.android.material.appbar.AppBarLayout;
import com.permissionx.guolindev.*;
import dagger.hilt.android.*;
import jadx.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.greenrobot.eventbus.android.*;
import org.json.*;

public class AboutActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private ScrollView vscroll1;
	private LinearLayout linear1;
	private TextView textview1;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.about);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		_app_bar = findViewById(R.id._app_bar);
		_coordinator = findViewById(R.id._coordinator);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
		vscroll1 = findViewById(R.id.vscroll1);
		linear1 = findViewById(R.id.linear1);
		textview1 = findViewById(R.id.textview1);
	}
	
	private void initializeLogic() {
		textview1.setText("╔══════════════════════════╗\n║        XuanDun Reinforcement v2.0   \n║        \"XuanDun加固  — 玄盾加固\"     \n╚══════════════════════════╝\n\n📌 What is XuanDun?\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\nXuanDun (玄盾 - \"XuanDun\") is a powerful Android APK\nprotection & reinforcement system designed to protect your\napplications from:\n\n  ❌ Reverse Engineering (jadx, apktool, dex2jar)\n  ❌ Signature bypass\n  ❌ Frida/Xposed/LSPosed Hooking\n  ❌ Memory Dumping (Epic, Echo, Layout Inspect, etc...)\n  ❌ Cloning & Dual-Space Apps\n  ❌ VM/Emulator Execution\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n🛡️ PROTECTION LAYERS\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n🔐 ENCRYPTION\n  • AES-256/GCM + XOR dual-layer encryption\n  • Dual XOR key generation\n  • All method instructions encrypted at rest\n  • In-memory at runtime\n\n🧬 DEX PROTECTION\n  • Method instruction extraction + encryption\n  • Original instructions replaced with NOPs\n  • V4 Binary Cython encrypted data\n  • InMemoryClassLoader\n\n🔍 SECURITY SCANNER (Native C++)\n  ✓ APK Signature Verification (Sha256-based)\n  ✓ Frida Detection (Base)\n  ✓ Ptrace Anti-Debugging\n  ✓ SVC Hook Detection\n  ✓ Root / Magisk Detection\n\n🖥️ VM / EMULATOR DETECTION (20+ checks)\n  • TracerPid monitoring\n  • Genymotion/VMOS/Redroid detection\n\n👥 CLONE / DUAL-SPACE DETECTION\n  • Anti Clone detection\n  • Virtual detection (/999/)\n  • Clone AtoZ Detection\n  • Dual-space Detection\n  • Parallel Space / System Clone detection\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n⚡ PERFORMANCE\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n  ✅ Improved processing dex\n  ✅ Improved server respond\n  ✅ No hex-string overhead\n  ✅ All method limit (OOM safe)\n  ✅ Smart instruction size detection\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n🏗️ ARCHITECTURE\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n  [Runtime]\n  XuanDunApp.java → Load → Restore → Security → Verify\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n📦 TECH STACK\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n  • Java + c++ + AES/GCM\n  • C++ Native (ARM/ARM64/x86/x86_64)\n  • Raw SVC Syscalls (anti-hook)\n  • Dual Layer Hook Detection \n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n📊 VERSION\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n  v2.0 — Optimized Edition\n  • Updated Signature Verification\n  • Memory: -70% usage\n  • Encryption: 5x faster\n  • Added: Native Cross-Check System\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n⚠️ DISCLAIMER\n━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n XuanDun Reinforcement may not work with every Android application. In some cases, apps protected with XuanDun may experience crashes or compatibility issues, especially if they already contain other protection mechanisms.\n\nXuanDun is designed to make DEX dumping more difficult and provide an additional layer of protection. However, no protection solution can guarantee 100% security.\n\nSince XuanDun is developed with Sketchware, it does not support C++ (Native Library/JNI). As a result, advanced native protection techniques such as Dex2C and VMP (Virtual Machine Protection) are not implemented.\n\n XuanDun is for protecting YOUR OWN applications.\n\nUnauthorized modification of third-party apps is illegal and violates copyright laws.\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n  Made with Sketchware | XuanDun Protection v2.0\n  \"玄盾加固 — Protecting Your Apk\"\n\n\" XuanDun Application Made by Telegram@Its_PokemonZ 🇲🇲\"\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
		setTitle("XuanDun : About the App");
	}
	
}