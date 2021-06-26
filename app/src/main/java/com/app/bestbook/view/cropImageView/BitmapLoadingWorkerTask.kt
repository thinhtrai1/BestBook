//package com.app.bestbook.view.cropImageView
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.net.Uri
//import android.os.AsyncTask
//
//import java.lang.ref.WeakReference
//
//class BitmapLoadingWorkerTask(cropImageView: CropImageView, uri: Uri) : AsyncTask<Void, Void, BitmapLoadingWorkerTask.Result>() {
//
//    /** Use a WeakReference to ensure the ImageView can be garbage collected  */
//    private val mCropImageViewReference: WeakReference<CropImageView> = WeakReference<CropImageView>(cropImageView)
//
//    /** The context of the crop image view widget used for loading of bitmap by Android URI  */
//    private val mContext: Context = cropImageView.context
//
//    private var mUri: Uri? = uri
//
//    /** required width of the cropping image after density adjustment  */
//    private val mWidth: Int
//
//    /** required height of the cropping image after density adjustment  */
//    private val mHeight: Int
//
//    init {
//        val metrics = cropImageView.resources.displayMetrics
//        val densityAdj = (if (metrics.density > 1) 1 / metrics.density else 1f)
//        mWidth = (metrics.widthPixels * densityAdj).toInt()
//        mHeight = (metrics.heightPixels * densityAdj).toInt()
//    }
//
//    fun getUri(): Uri? {
//        return mUri
//    }
//
//    override fun doInBackground(vararg params: Void): Result? {
//        try {
//            if (!isCancelled) {
//
//                val decodeResult = BitmapUtils.decodeSampledBitmap(mContext, mUri!!, mWidth, mHeight)
//
//                if (!isCancelled && decodeResult.bitmap != null) {
//
//                    val rotateResult =
//                        BitmapUtils.rotateBitmapByExif(decodeResult.bitmap, mContext, mUri!!)
//
//                    return Result(
//                        mUri, rotateResult.bitmap, decodeResult.sampleSize, rotateResult.degrees
//                    )
//                }
//            }
//            return null
//        } catch (e: Exception) {
//            return Result(mUri, e)
//        }
//
//    }
//
//    /**
//     * Once complete, see if ImageView is still around and set bitmap.
//     *
//     * @param result the result of bitmap loading
//     */
//    override fun onPostExecute(result: Result?) {
//        if (result != null) {
//            var completeCalled = false
//            if (!isCancelled) {
//                val cropImageView = mCropImageViewReference.get()
//                if (cropImageView != null) {
//                    completeCalled = true
//                }
//            }
//            if (!completeCalled && result.bitmap != null) {
//                // fast release of unused bitmap
//                result.bitmap.recycle()
//            }
//        }
//    }
//
//    // region: Inner class: Result
//
//    /** The result of BitmapLoadingWorkerTask async loading.  */
//    class Result {
//
//        /** The Android URI of the image to load  */
//        val uri: Uri?
//
//        /** The loaded bitmap  */
//        val bitmap: Bitmap?
//
//        /** The sample size used to load the given bitmap  */
//        val loadSampleSize: Int
//
//        /** The degrees the image was rotated  */
//        val degreesRotated: Int
//
//        /** The error that occurred during async bitmap loading.  */
//        val error: Exception?
//
//        internal constructor(uri: Uri?, bitmap: Bitmap, loadSampleSize: Int, degreesRotated: Int) {
//            this.uri = uri
//            this.bitmap = bitmap
//            this.loadSampleSize = loadSampleSize
//            this.degreesRotated = degreesRotated
//            this.error = null
//        }
//
//        internal constructor(uri: Uri?, error: Exception) {
//            this.uri = uri
//            this.bitmap = null
//            this.loadSampleSize = 0
//            this.degreesRotated = 0
//            this.error = error
//        }
//    }
//}
