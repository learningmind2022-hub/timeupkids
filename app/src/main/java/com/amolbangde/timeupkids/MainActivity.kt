package com.amolbangde.timeupkids

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

class MainActivity : AppCompatActivity() {

    companion object {
        const val APP_NAME = "TimeUp Kids"
        const val DEVELOPER = "Amol Bangde"

        // TEST banner ad unit. Release se pehle apna AdMob Banner Ad Unit ID yahan daalein.
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

        val TIMER_OPTIONS = listOf(5, 10, 15, 20)   // minutes
    }

    private val admin by lazy { ComponentName(this, AdminReceiver::class.java) }
    private val dpm by lazy { getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = APP_NAME
            textSize = 26f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Choose screen time for kids"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        TIMER_OPTIONS.forEach { min ->
            root.addView(Button(this).apply {
                text = "$min minutes"
                setOnClickListener { startTimer(min) }
            })
        }
        root.addView(Button(this).apply {
            text = "Cancel timer"
            setOnClickListener {
                startService(Intent(this@MainActivity, TimerService::class.java)
                    .setAction(TimerService.ACTION_STOP))
            }
        })

        // Banner ad: sirf parent wali home screen par (countdown/lock par nahi)
        setupAds(root)

        root.addView(TextView(this).apply {
            text = "Developed by $DEVELOPER"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        })
        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun setupAds(root: LinearLayout) {
        // Bachhon ke liye: child-directed treatment + sirf G-rated ads
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(
                    RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build()
        )
        MobileAds.initialize(this) {}

        val ad = AdView(this).apply {
            setAdSize(AdSize.BANNER)          // 320x50
            adUnitId = BANNER_AD_UNIT_ID
        }
        root.addView(ad, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 32 })
        ad.loadAd(AdRequest.Builder().build())
        adView = ad
    }

    private fun startTimer(minutes: Int) {
        // 1) Overlay permission (countdown dikhane ke liye)
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow \"Display over other apps\"", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            return
        }
        // 2) Device admin (phone lock karne ke liye)
        if (!dpm.isAdminActive(admin)) {
            startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Needed to lock the phone when the timer ends.")
            })
            return
        }
        val i = Intent(this, TimerService::class.java).putExtra(TimerService.EXTRA_MIN, minutes)
        startForegroundService(i)
        Toast.makeText(this, "$minutes-minute timer started", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }

    override fun onPause() { adView?.pause(); super.onPause() }
    override fun onResume() { super.onResume(); adView?.resume() }
    override fun onDestroy() { adView?.destroy(); super.onDestroy() }
}
