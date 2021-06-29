package com.app.bestbook.ui.splash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.app.bestbook.R
import com.app.bestbook.ui.home.HomeActivity
import com.app.bestbook.util.Constant
import com.app.bestbook.util.showToast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var isValid = false

        Firebase.database(Constant.FIREBASE_DATABASE).reference.child("token").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                showToast(getString(R.string.an_error_occurred_please_try_again))
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == "test1") {
                    if (isValid) {
                        startHome()
                    } else {
                        isValid = true
                    }
                } else {
                    showToast("The app has been blocked by the developer!")
                }
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            if (isValid) {
                startHome()
            } else {
                isValid = true
            }
        }, 500)
    }

    override fun onPause() {
        overridePendingTransition(0, 0)
        super.onPause()
    }

    private fun startHome() {
        startActivity(
            Intent(this, HomeActivity::class.java)
                .putExtra("uri", intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        )
        finish()
    }
}