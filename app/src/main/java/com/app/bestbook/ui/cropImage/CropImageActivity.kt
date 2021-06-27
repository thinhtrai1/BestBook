package com.app.bestbook.ui.cropImage

import android.app.Activity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityCropImageBinding

class CropImageActivity : BaseActivity() {
    private lateinit var mBinding: ActivityCropImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_crop_image)

        val uri = intent?.data ?: return
        showLoading(true)
        mBinding.cropImageView.setAspectRatio(17, 24)
        mBinding.cropImageView.setImageUri(uri) {
            showLoading(false)
        }

        mBinding.btnOk.setOnClickListener {
            showLoading(true)
            mBinding.cropImageView.getCroppedImage {
                showLoading(false)
                it?.let {
                    setResult(Activity.RESULT_OK, intent.setData(it))
                }
                finish()
            }
        }
    }
}