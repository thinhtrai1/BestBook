//package com.app.bestbook.view.cropImageView
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.graphics.PorterDuff
//import android.graphics.drawable.Drawable
//import android.net.Uri
//import android.os.Build
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.util.Log
//import android.view.Menu
//import android.view.MenuItem
//import android.widget.Toast
//import androidx.core.content.ContextCompat
//import com.acuval.R
//import com.app.bestbook.R
//import kotlinx.android.synthetic.main.activity_crop_image.*
//import java.io.File
//import java.io.IOException
//
//class CropImageActivity : AppCompatActivity(),
//    CropImageView.OnSetImageUriCompleteListener, CropImageView.OnCropImageCompleteListener {
//
//    private var mCropImageUri: Uri? = null
//    private var mOptions: CropImageOptions? = null
//    private lateinit var mCropImageView: CropImageView
//
//    @SuppressLint("NewApi")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_crop_image)
//
//        val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
//        mCropImageUri = bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
//        mOptions = bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS)
//
//        if (savedInstanceState == null) {
//            if (mCropImageUri == null || mCropImageUri == Uri.EMPTY) {
//                if (CropImage.isExplicitCameraPermissionRequired(this)) {
//                    // request permissions and handle the result in onRequestPermissionsResult()
//                    requestPermissions(
//                        arrayOf(Manifest.permission.CAMERA),
//                        CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE
//                    )
//                } else {
//                    CropImage.startPickImageActivity(this)
//                }
//            } else if (CropImage.isReadExternalStoragePermissionsRequired(this, mCropImageUri!!)) {
//                // request permissions and handle the result in onRequestPermissionsResult()
//                requestPermissions(
//                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                    CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE
//                )
//            } else {
//                // no permissions required or already grunted, can start crop image activity
//                mCropImageView!!.setImageUriAsync(mCropImageUri)
//            }
//        }
//
//        setSupportActionBar(mToolBar)
//        val actionBar = supportActionBar
//        val title = if (mOptions != null) mOptions!!.activityTitle else resources.getString(R.string.crop_image_activity_title)
//        actionBar!!.title = title
////        actionBar.setDisplayHomeAsUpEnabled(true)
//    }
//
//    override fun onStart() {
//        super.onStart()
//        mCropImageView!!.setOnSetImageUriCompleteListener(this)
//        mCropImageView!!.setOnCropImageCompleteListener(this)
//    }
//
//    override fun onStop() {
//        super.onStop()
//        mCropImageView!!.setOnSetImageUriCompleteListener(null)
//        mCropImageView!!.setOnCropImageCompleteListener(null)
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.crop_image_menu, menu)
//
//        if (!mOptions!!.allowRotation) {
//            menu.removeItem(R.id.crop_image_menu_rotate_left)
//            menu.removeItem(R.id.crop_image_menu_rotate_right)
//        } else if (mOptions!!.allowCounterRotation) {
//            menu.findItem(R.id.crop_image_menu_rotate_left).isVisible = true
//        }
//
//        if (!mOptions!!.allowFlipping) {
//            menu.removeItem(R.id.crop_image_menu_flip)
//        }
//
//        if (mOptions!!.cropMenuCropButtonTitle != null) {
//            menu.findItem(R.id.crop_image_menu_crop).title = mOptions!!.cropMenuCropButtonTitle
//        }
//
//        var cropIcon: Drawable? = null
//        try {
//            if (mOptions!!.cropMenuCropButtonIcon != 0) {
//                cropIcon = ContextCompat.getDrawable(this, mOptions!!.cropMenuCropButtonIcon)
//                menu.findItem(R.id.crop_image_menu_crop).icon = cropIcon
//            }
//        } catch (e: Exception) {
//            Log.w("AIC", "Failed to read menu crop drawable", e)
//        }
//
//        if (mOptions!!.activityMenuIconColor != 0) {
//            updateMenuItemIconColor(
//                menu, R.id.crop_image_menu_rotate_left, mOptions!!.activityMenuIconColor
//            )
//            updateMenuItemIconColor(
//                menu, R.id.crop_image_menu_rotate_right, mOptions!!.activityMenuIconColor
//            )
//            updateMenuItemIconColor(
//                menu,
//                R.id.crop_image_menu_flip,
//                mOptions!!.activityMenuIconColor
//            )
//            if (cropIcon != null) {
//                updateMenuItemIconColor(
//                    menu,
//                    R.id.crop_image_menu_crop,
//                    mOptions!!.activityMenuIconColor
//                )
//            }
//        }
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == R.id.crop_image_menu_crop) {
//            cropImage()
//            return true
//        }
//        if (item.itemId == R.id.crop_image_menu_rotate_left) {
//            rotateImage(-mOptions!!.rotationDegrees)
//            return true
//        }
//        if (item.itemId == R.id.crop_image_menu_rotate_right) {
//            rotateImage(mOptions!!.rotationDegrees)
//            return true
//        }
//        if (item.itemId == R.id.crop_image_menu_flip_horizontally) {
//            mCropImageView!!.flipImageHorizontally()
//            return true
//        }
//        if (item.itemId == R.id.crop_image_menu_flip_vertically) {
//            mCropImageView!!.flipImageVertically()
//            return true
//        }
//        if (item.itemId == android.R.id.home) {
//            setResultCancel()
//            return true
//        }
//        return super.onOptionsItemSelected(item)
//    }
//
//    override fun onBackPressed() {
//        super.onBackPressed()
//        setResultCancel()
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        // handle result of pick image chooser
//        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE) {
//            if (resultCode == Activity.RESULT_CANCELED) {
//                // User cancelled the picker. We don't have anything to crop
//                setResultCancel()
//            }
//
//            if (resultCode == Activity.RESULT_OK && data != null) {
//                mCropImageUri = CropImage.getPickImageResultUri(this, data)
//
//                // For API >= 23 we need to check specifically that we have permissions to read external
//                // storage.
//                if (CropImage.isReadExternalStoragePermissionsRequired(this, mCropImageUri!!)) {
//                    // request permissions and handle the result in onRequestPermissionsResult()
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                        requestPermissions(
//                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                            CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE
//                        )
//                    }
//                } else {
//                    // no permissions required or already grunted, can start crop image activity
//                    mCropImageView!!.setImageUriAsync(mCropImageUri)
//                }
//            }
//        }
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        if (requestCode == CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE) {
//            if (mCropImageUri != null
//                && grantResults.isNotEmpty()
//                && grantResults[0] == PackageManager.PERMISSION_GRANTED
//            ) {
//                // required permissions granted, start crop image activity
//                mCropImageView!!.setImageUriAsync(mCropImageUri)
//            } else {
//                Toast.makeText(this, R.string.crop_image_activity_no_permissions, Toast.LENGTH_LONG)
//                    .show()
//                setResultCancel()
//            }
//        }
//
//        if (requestCode == CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE) {
//            // Irrespective of whether camera permission was given or not, we show the picker
//            // The picker will not add the camera intent if permission is not available
//            CropImage.startPickImageActivity(this)
//        }
//    }
//
//    override fun onSetImageUriComplete(view: CropImageView, uri: Uri?, error: Exception?) {
//        if (error == null) {
//            if (mOptions!!.initialCropWindowRectangle != null) {
//                mCropImageView!!.setCropRect(mOptions!!.initialCropWindowRectangle)
//            }
//            if (mOptions!!.initialRotation > -1) {
//                mCropImageView!!.setRotatedDegrees(mOptions!!.initialRotation)
//            }
//        } else {
//            setResult(null, error, 1)
//        }
//    }
//
//    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
//        setResult(result.getUri(), result.getError(), result.getSampleSize())
//    }
//
//    // region: Private methods
//
//    /** Execute crop image and save the result tou output uri.  */
//    private fun cropImage() {
//        if (mOptions!!.noOutputImage) {
//            setResult(null, null, 1)
//        } else {
//            val outputUri = getOutputUri()
//            mCropImageView!!.saveCroppedImageAsync(
//                outputUri,
//                mOptions!!.outputCompressFormat,
//                mOptions!!.outputCompressQuality,
//                mOptions!!.outputRequestWidth,
//                mOptions!!.outputRequestHeight,
//                mOptions!!.outputRequestSizeOptions
//            )
//        }
//    }
//
//    private fun rotateImage(degrees: Int) {
//        mCropImageView!!.rotateImage(degrees)
//    }
//
//    private fun getOutputUri(): Uri? {
//        var outputUri = mOptions!!.outputUri
//        if (outputUri == null || outputUri == Uri.EMPTY) {
//            try {
//                val ext = when (mOptions!!.outputCompressFormat) {
//                    Bitmap.CompressFormat.JPEG -> ".jpg"
//                    Bitmap.CompressFormat.PNG -> ".png"
//                    else -> ".webp"
//                }
//                outputUri = Uri.fromFile(File.createTempFile("cropped", ext, cacheDir))
//            } catch (e: IOException) {
//                throw RuntimeException("Failed to create temp file for output image", e)
//            }
//
//        }
//        return outputUri
//    }
//
//    private fun setResult(uri: Uri?, error: Exception?, sampleSize: Int) {
//        val resultCode =
//            if (error == null) RESULT_OK else CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE
//        setResult(resultCode, getResultIntent(uri, error, sampleSize))
//        finish()
//    }
//
//    private fun setResultCancel() {
//        setResult(RESULT_CANCELED)
//        finish()
//    }
//
//    private fun getResultIntent(uri: Uri?, error: Exception?, sampleSize: Int): Intent {
//        val result = CropImage.Companion.ActivityResult(
//            mCropImageView!!.getImageUri(),
//            uri,
//            error,
//            mCropImageView!!.getCropPoints(),
//            mCropImageView!!.getCropRect(),
//            mCropImageView!!.getRotatedDegrees(),
//            mCropImageView!!.getWholeImageRect(),
//            sampleSize
//        )
//        val intent = Intent()
//        intent.putExtras(getIntent())
//        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_RESULT, result)
//        return intent
//    }
//
//    private fun updateMenuItemIconColor(menu: Menu, itemId: Int, color: Int) {
//        val menuItem = menu.findItem(itemId)
//        if (menuItem != null) {
//            val menuItemIcon = menuItem.icon
//            if (menuItemIcon != null) {
//                try {
//                    menuItemIcon.mutate()
//                    menuItemIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
//                    menuItem.icon = menuItemIcon
//                } catch (e: Exception) {
//                    Log.w("AIC", "Failed to update menu item color", e)
//                }
//            }
//        }
//    }
//
//    override fun startActivity(intent: Intent) {
//        super.startActivity(intent)
//        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
//    }
//
//    override fun finish() {
//        super.finish()
//        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
//    }
//}
