package com.app.bestbook.base

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.app.bestbook.R

abstract class BaseActivity : AppCompatActivity() {
    private lateinit var mProgressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProgressDialog = Dialog(this).apply {
            setContentView(R.layout.progress_dialog)
            setCancelable(false)
            window!!.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    protected fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            mProgressDialog.show()
        } else {
            mProgressDialog.dismiss()
        }
    }

    protected fun <T> MutableLiveData<T>.observe(action: (T) -> Unit) {
        observe(this@BaseActivity, Observer {
            action(it)
        })
    }

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        overridePendingTransition(R.anim.scale_right_to_left_in, R.anim.scale_right_to_left_out)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.scale_left_to_right_in, R.anim.scale_left_to_right_out)
    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int, options: Bundle?) {
        super.startActivityForResult(intent, requestCode, options)
        overridePendingTransition(R.anim.scale_right_to_left_in, R.anim.scale_right_to_left_out)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        overridePendingTransition(R.anim.scale_left_to_right_in, R.anim.scale_left_to_right_out)
    }
}