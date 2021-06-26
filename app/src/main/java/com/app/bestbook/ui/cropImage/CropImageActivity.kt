package com.app.bestbook.ui.cropImage

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityCropImageBinding
import java.io.File

class CropImageActivity : BaseActivity() {
    private lateinit var mBinding: ActivityCropImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_crop_image)

        val uri = intent?.data ?: return
        mBinding.cropImageView.setAspectRatio(100, 141)
        mBinding.cropImageView.setSnapRadius(0.25f)
        contentResolver.openInputStream(uri).use {
            mBinding.cropImageView.setImageBitmap(BitmapFactory.decodeStream(it))
        }

        mBinding.btnOk.setOnClickListener {
            mBinding.cropImageView.getCroppedImage {
                it?.let {
                    val file = File.createTempFile("temp_", ".png", cacheDir)
                    file.outputStream().use { outputStream ->
                        it.compress(Bitmap.CompressFormat.PNG, 0, outputStream)
                        it.recycle()
                    }
                    setResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
                }
                finish()
            }
        }
    }
}