package com.example.fitnesstracker.ui.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fitnesstracker.database.AppDatabase
import com.example.fitnesstracker.ui.dashboard.DashboardActivity
import com.example.fitnesstracker.ui.plans.PlanSelectionActivity
import com.example.fitnesstracker.ui.registration.RegistrationActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_ACTIVITY_RECOGNITION = 1001
    }

    private lateinit var db: AppDatabase
    private var permissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(this)

        // Check permission and request if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                    REQUEST_CODE_ACTIVITY_RECOGNITION)
            } else {
                permissionGranted = true
                proceedAfterPermission()
            }
        } else {
            // Permissions not required on older versions
            permissionGranted = true
            proceedAfterPermission()
        }
    }

    private fun proceedAfterPermission() {
        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUser()
            runOnUiThread {
                when {
                    user == null -> startActivity(Intent(this@LauncherActivity, RegistrationActivity::class.java))
                    user.planType == "not_selected" -> startActivity(Intent(this@LauncherActivity, PlanSelectionActivity::class.java))
                    else -> {
                        val intent = Intent(this@LauncherActivity, DashboardActivity::class.java)
                        // Pass permission status so Dashboard can start service if allowed
                        intent.putExtra("PERMISSION_GRANTED", permissionGranted)
                        startActivity(intent)
                    }
                }
                finish()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_ACTIVITY_RECOGNITION) {
            permissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            proceedAfterPermission()
        }
    }
}
