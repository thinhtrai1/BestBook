package com.app.bestbook.ui.cropImage

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_crop_image, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_rotate -> mBinding.cropImageView.rotateImage(90)
            R.id.menu_flip_vertical -> mBinding.cropImageView.flipImageVertically()
            R.id.menu_flip_horizontal -> mBinding.cropImageView.flipImageHorizontally()
            R.id.menu_crop -> {
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
        return true
    }
}