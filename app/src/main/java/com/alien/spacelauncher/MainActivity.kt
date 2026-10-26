package com.alien.spacelauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var tts: TextToSpeech? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }

        webView = WebView(this)
        setContentView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class AndroidBridge {

        @JavascriptInterface
        fun launchApp(packageName: String) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }

        @JavascriptInterface
        fun openDialer() {
            startActivity(Intent(Intent.ACTION_DIAL))
        }

        @JavascriptInterface
        fun openCamera() {
            startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        }

        @JavascriptInterface
        fun speakText(text: String) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ALIEN_VOICE")
        }

        @JavascriptInterface
        fun getInstalledAppsJson(): String {
            val pm: PackageManager = packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val arr = JSONArray()
            for (info in resolveInfos) {
                val obj = JSONObject()
                obj.put("name", info.loadLabel(pm).toString())
                obj.put("pkg", info.activityInfo.packageName)
                obj.put("url", "")
                obj.put("icon", "📱")
                arr.put(obj)
            }
            return arr.toString()
        }

        @JavascriptInterface
        fun getContactsJson(): String {
            val arr = JSONArray()
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null
            )
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                var count = 0
                while (it.moveToNext() && count < 100) {
                    val obj = JSONObject()
                    obj.put("name", it.getString(nameIdx))
                    obj.put("number", it.getString(numIdx))
                    arr.put(obj)
                    count++
                }
            }
            return arr.toString()
        }
    }

    override fun onBackPressed() {
        // Launcher stays on Home Screen when Back is pressed
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
