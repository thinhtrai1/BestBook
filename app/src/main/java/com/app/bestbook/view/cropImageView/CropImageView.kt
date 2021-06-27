package com.app.bestbook.view.cropImageView

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.FrameLayout
import android.widget.ImageView
import com.app.bestbook.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.*
import kotlin.math.*

class CropImageView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val mImageView: ImageView
    private val mCropOverlayView: CropOverlayView
    private val mImageMatrix = Matrix()
    private val mImageInverseMatrix = Matrix()
    private val mImagePoints = FloatArray(8)
    private val mScaleImagePoints = FloatArray(8)
    private var mAnimation: CropImageAnimation? = null
    private var mBitmap: Bitmap? = null
    private var mInitialDegreesRotated = 0
    private var mDegreesRotated = 0
    private var mFlipHorizontally = false
    private var mFlipVertically = false
    private var mLayoutWidth = 0
    private var mLayoutHeight = 0
    private var mScaleType: ScaleType? = null
    private var mAutoZoomEnabled = true
    private var mMaxZoom = 4
    private var mZoom = 1f
    private var mZoomOffsetX = 0f
    private var mZoomOffsetY = 0f
    private var mRestoreCropWindowRect: RectF? = null
    private var mRestoreDegreesRotated = 0
    private var mSizeChanged = false
    private var mLoadedImageUri: Uri? = null
    private var mLoadedSampleSize = 1

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, 0, 0)
        mScaleType = ScaleType.values()[ta.getInt(R.styleable.CropImageView_cropScaleType, ScaleType.FIT_CENTER.ordinal)]
        mAutoZoomEnabled = ta.getBoolean(R.styleable.CropImageView_cropAutoZoomEnabled, true)
        val v = LayoutInflater.from(context).inflate(R.layout.crop_image_view, this, true)
        mImageView = v.findViewById(R.id.ImageView_image)
        mImageView.scaleType = ImageView.ScaleType.MATRIX
        mCropOverlayView = v.findViewById(R.id.CropOverlayView)
        mCropOverlayView.setInitialAttributeValues(ta)
        mCropOverlayView.setOnCropWindowChanged {
            handleCropWindowChanged(it, true)
        }
        ta.recycle()
        validate()
    }

    private fun validate() {
        require(mMaxZoom >= 0) { "Cannot set max zoom to a number < 1" }
        mCropOverlayView.validate()
    }

    fun setImageBitmap(bitmap: Bitmap, onDisplay: (() -> Unit)? = null) {
        mCropOverlayView.setInitialCropWindowRect(null)
        GlobalScope.launch(Dispatchers.IO) {
            val file = File.createTempFile("temp_", ".jpg", context.cacheDir)
            file.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                bitmap.recycle()
            }
            mLoadedImageUri = Uri.fromFile(file)
        }
        GlobalScope.launch(Dispatchers.IO) {
            val result = BitmapUtils.decodeSampledBitmap(context, bitmap)
            mLoadedSampleSize = result.sampleSize
            GlobalScope.launch(Dispatchers.Main) {
                result.bitmap?.let {
                    setBitmap(it, 0)
                }
                onDisplay?.invoke()
            }
        }
    }

    fun setImageUri(uri: Uri, onDisplay: (() -> Unit)? = null) {
        mCropOverlayView.setInitialCropWindowRect(null)
        mLoadedImageUri = uri
        GlobalScope.launch(Dispatchers.IO) {
            val result = BitmapUtils.decodeSampledBitmap(context, uri)
            mLoadedSampleSize = result.sampleSize
            val bitmap = result.bitmap?.let {
                BitmapUtils.rotateBitmapByExif(it, context, uri)
            }
            GlobalScope.launch(Dispatchers.Main) {
                bitmap?.let {
                    setBitmap(it.bitmap, it.degrees)
                }
                onDisplay?.invoke()
            }
        }
    }

    private fun setBitmap(bitmap: Bitmap, degreesRotated: Int) {
        if (mBitmap == null || mBitmap != bitmap) {
            mImageView.clearAnimation()
            clearImageInt()
            mBitmap = bitmap
            mImageView.setImageBitmap(bitmap)
            mDegreesRotated = degreesRotated
            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
            mCropOverlayView.resetCropOverlayView()
        }
    }

    fun getCroppedImage(callback: (Uri?) -> Unit) {
        getCroppedImage(0, 0, RequestSizeOptions.NONE, callback)
    }

    fun getCroppedImage(reqWidth: Int, reqHeight: Int, callback: (Uri?) -> Unit) {
        getCroppedImage(reqWidth, reqHeight, RequestSizeOptions.RESIZE_INSIDE, callback)
    }

    fun getCroppedImage(reqWidth: Int, reqHeight: Int, options: RequestSizeOptions, callback: (Uri?) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val uri = if (mBitmap != null && mLoadedImageUri != null) {
                var croppedBitmap = if (mLoadedSampleSize > 1 || options == RequestSizeOptions.SAMPLING) {
                    val width = if (options != RequestSizeOptions.NONE) reqWidth else 0
                    val height = if (options != RequestSizeOptions.NONE) reqHeight else 0
                    val orgWidth = mBitmap!!.width * mLoadedSampleSize
                    val orgHeight = mBitmap!!.height * mLoadedSampleSize
                    BitmapUtils.cropBitmap(
                        context,
                        mLoadedImageUri!!,
                        getCropPoints(),
                        mDegreesRotated,
                        orgWidth,
                        orgHeight,
                        mCropOverlayView.isFixAspectRatio(),
                        mCropOverlayView.getAspectRatioX(),
                        mCropOverlayView.getAspectRatioY(),
                        width,
                        height,
                        mFlipHorizontally,
                        mFlipVertically
                    ).bitmap!!
                } else {
                    BitmapUtils.cropBitmapObjectHandleOOM(
                        mBitmap!!,
                        getCropPoints(),
                        mDegreesRotated,
                        mCropOverlayView.isFixAspectRatio(),
                        mCropOverlayView.getAspectRatioX(),
                        mCropOverlayView.getAspectRatioY(),
                        mFlipHorizontally,
                        mFlipVertically
                    ).bitmap!!
                }
                croppedBitmap = BitmapUtils.resizeBitmap(croppedBitmap, width, height, options)
                val file = File.createTempFile("temp_", ".jpg", context.cacheDir)
                file.outputStream().use { outputStream ->
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    croppedBitmap.recycle()
                }
                Uri.fromFile(file)
            } else {
                null
            }
            GlobalScope.launch(Dispatchers.Main) {
                mImageView.clearAnimation()
                callback(uri)
            }
        }
    }

    fun setScaleType(scaleType: ScaleType) {
        if (scaleType != mScaleType) {
            mScaleType = scaleType
            mZoom = 1F
            mZoomOffsetY = 0F
            mZoomOffsetX = mZoomOffsetY
            mCropOverlayView.resetCropOverlayView()
            requestLayout()
        }
    }

    fun setCropShape(cropShape: CropShape) {
        mCropOverlayView.setCropShape(cropShape)
    }

    fun setAutoZoomEnabled(autoZoomEnabled: Boolean) {
        if (mAutoZoomEnabled != autoZoomEnabled) {
            mAutoZoomEnabled = autoZoomEnabled
            handleCropWindowChanged(false, false)
            mCropOverlayView.invalidate()
        }
    }

    fun setMultiTouchEnabled(multiTouchEnabled: Boolean) {
        if (mCropOverlayView.setMultiTouchEnabled(multiTouchEnabled)) {
            handleCropWindowChanged(false, false)
            mCropOverlayView.invalidate()
        }
    }

    fun setMaxZoom(maxZoom: Int) {
        if (mMaxZoom != maxZoom && maxZoom > 0) {
            mMaxZoom = maxZoom
            handleCropWindowChanged(false, false)
            mCropOverlayView.invalidate()
        }
    }

    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        mCropOverlayView.setMinCropResultSize(minCropResultWidth, minCropResultHeight)
    }

    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        mCropOverlayView.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight)
    }

    fun setRotatedDegrees(degrees: Int) {
        if (mDegreesRotated != degrees) {
            rotateImage(degrees - mDegreesRotated)
        }
    }

    fun setFixedAspectRatio(fixAspectRatio: Boolean) {
        mCropOverlayView.setFixedAspectRatio(fixAspectRatio)
    }

    fun setFlippedHorizontally(flipHorizontally: Boolean) {
        if (mFlipHorizontally != flipHorizontally) {
            mFlipHorizontally = flipHorizontally
            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
        }
    }

    fun setFlippedVertically(flipVertically: Boolean) {
        if (mFlipVertically != flipVertically) {
            mFlipVertically = flipVertically
            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
        }
    }

    fun setGuidelines(guidelines: Guidelines) {
        mCropOverlayView.setGuidelines(guidelines)
    }

    fun setAspectRatio(aspectRatioX: Int, aspectRatioY: Int) {
        mCropOverlayView.setAspectRatioX(aspectRatioX)
        mCropOverlayView.setAspectRatioY(aspectRatioY)
        setFixedAspectRatio(true)
    }

    fun clearAspectRatio() {
        mCropOverlayView.setAspectRatioX(1)
        mCropOverlayView.setAspectRatioY(1)
        setFixedAspectRatio(false)
    }

    /**
     * An edge of the crop window will snap to the corresponding edge of a specified bounding box when
     * the crop window edge is less than or equal to this distance (in pixels) away from the bounding
     * box edge. (default: 3dp)
     */
    fun setSnapRadius(snapRadius: Float) {
        if (snapRadius >= 0) {
            mCropOverlayView.setSnapRadius(snapRadius)
        }
    }

    /**
     * Gets the source Bitmap's dimensions. This represents the largest possible crop rectangle.
     *
     * @return a Rect instance dimensions of the source Bitmap
     */
    fun getWholeImageRect(): Rect? {
        mBitmap ?: return null
        return Rect(0, 0, mBitmap!!.width, mBitmap!!.height)
    }

    /**
     * Gets the crop window's position relative to the source Bitmap (not the image displayed in the
     * CropImageView) using the original image rotation.
     *
     * @return a Rect instance containing cropped area boundaries of the source Bitmap
     */
    fun getCropRect(): Rect? {
        mBitmap ?: return null
        return BitmapUtils.getRectFromPoints(
            getCropPoints(),
            mBitmap!!.width,
            mBitmap!!.height,
            mCropOverlayView.isFixAspectRatio(),
            mCropOverlayView.getAspectRatioX(),
            mCropOverlayView.getAspectRatioY()
        )
    }

    /**
     * Gets the 4 points of crop window's position relative to the source Bitmap (not the image
     * displayed in the CropImageView) using the original image rotation.<br></br>
     * Note: the 4 points may not be a rectangle if the image was rotates to NOT stright angle (!=
     * 90/180/270).
     *
     * @return 4 points (x0,y0,x1,y1,x2,y2,x3,y3) of cropped area boundaries
     */
    fun getCropPoints(): FloatArray {
        // Get crop window position relative to the displayed image.
        val cropWindowRect = mCropOverlayView.getCropWindowRect()
        val points = floatArrayOf(
            cropWindowRect.left,
            cropWindowRect.top,
            cropWindowRect.right,
            cropWindowRect.top,
            cropWindowRect.right,
            cropWindowRect.bottom,
            cropWindowRect.left,
            cropWindowRect.bottom
        )
        mImageMatrix.invert(mImageInverseMatrix)
        mImageInverseMatrix.mapPoints(points)
        for (i in points.indices) {
            points[i] = points[i] * mLoadedSampleSize
        }
        return points
    }

    /**
     * Set the crop window position and size to the given rectangle.<br></br>
     * Image to crop must be first set before invoking this, for async - after complete callback.
     *
     * @param rect window rectangle (position and size) relative to source bitmap
     */
    fun setCropRect(rect: Rect?) {
        mCropOverlayView.setInitialCropWindowRect(rect)
    }

    /** Reset crop window to initial rectangle.  */
    fun resetCropRect() {
        mZoom = 1F
        mZoomOffsetX = 0F
        mZoomOffsetY = 0F
        mDegreesRotated = mInitialDegreesRotated
        mFlipHorizontally = false
        mFlipVertically = false
        applyImageMatrix(width.toFloat(), height.toFloat(), false, false)
        mCropOverlayView.resetCropWindowRect()
    }

    fun clearImage() {
        clearImageInt()
        mLoadedImageUri = null
        mCropOverlayView.setInitialCropWindowRect(null)
    }

    fun rotateImage(degrees: Int) {
        if (mBitmap != null) {
            // Force degrees to be a non-zero value between 0 and 360 (inclusive)
            val deg = if (degrees < 0) {
                degrees % 360 + 360
            } else {
                degrees % 360
            }
            val flipAxes = !mCropOverlayView.isFixAspectRatio() && (deg in 46..134 || deg in 216..304)
            BitmapUtils.RECT.set(mCropOverlayView.getCropWindowRect())
            var halfWidth = (if (flipAxes) BitmapUtils.RECT.height() else BitmapUtils.RECT.width()) / 2f
            var halfHeight = (if (flipAxes) BitmapUtils.RECT.width() else BitmapUtils.RECT.height()) / 2f
            if (flipAxes) {
                val isFlippedHorizontally = mFlipHorizontally
                mFlipHorizontally = mFlipVertically
                mFlipVertically = isFlippedHorizontally
            }
            mImageMatrix.invert(mImageInverseMatrix)
            BitmapUtils.POINTS[0] = BitmapUtils.RECT.centerX()
            BitmapUtils.POINTS[1] = BitmapUtils.RECT.centerY()
            BitmapUtils.POINTS[2] = 0f
            BitmapUtils.POINTS[3] = 0f
            BitmapUtils.POINTS[4] = 1f
            BitmapUtils.POINTS[5] = 0f
            mImageInverseMatrix.mapPoints(BitmapUtils.POINTS)
            // This is valid because degrees is not negative.
            mDegreesRotated = (mDegreesRotated + deg) % 360

            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
            // adjust the zoom so the crop window size remains the same even after image scale change
            mImageMatrix.mapPoints(BitmapUtils.POINTS2, BitmapUtils.POINTS)
            mZoom /= sqrt(
                (BitmapUtils.POINTS2[4] - BitmapUtils.POINTS2[2]).toDouble().pow(2.0) + (BitmapUtils.POINTS2[5] - BitmapUtils.POINTS2[3]).toDouble().pow(2.0)
            ).toFloat()
            mZoom = max(mZoom, 1f)

            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
            mImageMatrix.mapPoints(BitmapUtils.POINTS2, BitmapUtils.POINTS)

            // adjust the width/height by the changes in scaling to the image
            val change = sqrt(
                (BitmapUtils.POINTS2[4] - BitmapUtils.POINTS2[2]).toDouble().pow(2.0) + (BitmapUtils.POINTS2[5] - BitmapUtils.POINTS2[3]).toDouble().pow(2.0)
            )
            halfWidth *= change.toFloat()
            halfHeight *= change.toFloat()

            // calculate the new crop window rectangle to center in the same location and have proper
            // width/height
            BitmapUtils.RECT.set(
                BitmapUtils.POINTS2[0] - halfWidth,
                BitmapUtils.POINTS2[1] - halfHeight,
                BitmapUtils.POINTS2[0] + halfWidth,
                BitmapUtils.POINTS2[1] + halfHeight
            )
            mCropOverlayView.resetCropOverlayView()
            mCropOverlayView.setCropWindowRect(BitmapUtils.RECT)
            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
            handleCropWindowChanged(false, false)
            // make sure the crop window rectangle is within the cropping image bounds after all the
            // changes
            mCropOverlayView.fixCurrentCropWindowRect()
        }
    }

    /** Flips the image horizontally.  */
    fun flipImageHorizontally() {
        mFlipHorizontally = !mFlipHorizontally
        applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
    }

    /** Flips the image vertically.  */
    fun flipImageVertically() {
        mFlipVertically = !mFlipVertically
        applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
    }

    /**
     * Clear the current image set for cropping.<br></br>
     * Full clear will also clear the data of the set image like Uri or Resource id while partial
     * clear will only clear the bitmap and recycle if required.
     */
    private fun clearImageInt() {
        // if we allocated the bitmap, release it as fast as possible
        if (mBitmap != null) {
            mBitmap!!.recycle()
        }
        mBitmap = null

        // clean the loaded image flags for new image
        mDegreesRotated = 0
        mZoom = 1F
        mZoomOffsetX = 0F
        mZoomOffsetY = 0F
        mImageMatrix.reset()
        mImageView.setImageBitmap(null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (mBitmap == null) {
            setMeasuredDimension(widthSize, heightSize)
        } else mBitmap!!.let {
            // Bypasses a baffling bug when used within a ScrollView, where heightSize is set to 0.
            if (heightSize == 0) {
                heightSize = it.height
            }
            val desiredWidth: Int
            val desiredHeight: Int
            var viewToBitmapWidthRatio = java.lang.Double.POSITIVE_INFINITY
            var viewToBitmapHeightRatio = java.lang.Double.POSITIVE_INFINITY
            // Checks if either width or height needs to be fixed
            if (widthSize < it.width) {
                viewToBitmapWidthRatio = widthSize.toDouble() / it.width.toDouble()
            }
            if (heightSize < it.height) {
                viewToBitmapHeightRatio = heightSize.toDouble() / it.height.toDouble()
            }
            // If either needs to be fixed, choose smallest ratio and calculate from there
            if (viewToBitmapWidthRatio != java.lang.Double.POSITIVE_INFINITY || viewToBitmapHeightRatio != java.lang.Double.POSITIVE_INFINITY) {
                if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
                    desiredWidth = widthSize
                    desiredHeight = (it.height * viewToBitmapWidthRatio).toInt()
                } else {
                    desiredHeight = heightSize
                    desiredWidth = (it.width * viewToBitmapHeightRatio).toInt()
                }
            } else {
                // Otherwise, the picture is within frame layout bounds. Desired width is simply picture size
                desiredWidth = it.width
                desiredHeight = it.height
            }
            mLayoutWidth = getOnMeasureSpec(widthMode, widthSize, desiredWidth)
            mLayoutHeight = getOnMeasureSpec(heightMode, heightSize, desiredHeight)
            setMeasuredDimension(mLayoutWidth, mLayoutHeight)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (mLayoutWidth > 0 && mLayoutHeight > 0) {
            layoutParams = layoutParams.apply {
                width = mLayoutWidth
                height = mLayoutHeight
            }
            if (mBitmap != null) {
                applyImageMatrix((r - l).toFloat(), (b - t).toFloat(), true, false)
                // after state restore we want to restore the window crop, possible only after widget size is known
                if (mRestoreCropWindowRect != null) {
                    if (mRestoreDegreesRotated != mInitialDegreesRotated) {
                        mDegreesRotated = mRestoreDegreesRotated
                        applyImageMatrix((r - l).toFloat(), (b - t).toFloat(), true, false)
                    }
                    mImageMatrix.mapRect(mRestoreCropWindowRect)
                    mCropOverlayView.setCropWindowRect(mRestoreCropWindowRect!!)
                    handleCropWindowChanged(false, false)
                    mCropOverlayView.fixCurrentCropWindowRect()
                    mRestoreCropWindowRect = null
                } else if (mSizeChanged) {
                    mSizeChanged = false
                    handleCropWindowChanged(false, false)
                }
            } else {
                updateImageBounds(true)
            }
        } else {
            updateImageBounds(true)
        }
    }

    /**
     * Detect size change to handle auto-zoom using [.handleCropWindowChanged]
     * in [.layout].
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mSizeChanged = oldw > 0 && oldh > 0
    }

    /**
     * Handle crop window change to:<br></br>
     * 1. Execute auto-zoom-in/out depending on the area covered of cropping window relative to the
     * available view area.<br></br>
     * 2. Slide the zoomed sub-area if the cropping window is outside of the visible view sub-area.
     * <br></br>
     *
     * @param inProgress is the crop window change is still in progress by the user
     * @param animate if to animate the change to the image matrix, or set it directly
     */
    private fun handleCropWindowChanged(inProgress: Boolean, animate: Boolean) {
        val width = width
        val height = height
        if (mBitmap != null && width > 0 && height > 0) {
            val cropRect = mCropOverlayView.getCropWindowRect()
            if (inProgress) {
                if (cropRect.left < 0 || cropRect.top < 0 || cropRect.right > width || cropRect.bottom > height) {
                    applyImageMatrix(width.toFloat(), height.toFloat(), false, false)
                }
            } else if (mAutoZoomEnabled || mZoom > 1) {
                var newZoom = 0f
                // keep the cropping window covered area to 50%-65% of zoomed sub-area
                if (mZoom < mMaxZoom && cropRect.width() < width * 0.5f && cropRect.height() < height * 0.5f) {
                    newZoom = min(
                        mMaxZoom.toFloat(),
                        min(
                            width / (cropRect.width() / mZoom / 0.64f), height / (cropRect.height() / mZoom / 0.64f)
                        )
                    )
                }
                if (mZoom > 1 && (cropRect.width() > width * 0.65f || cropRect.height() > height * 0.65f)) {
                    newZoom = max(
                        1f,
                        min(
                            width / (cropRect.width() / mZoom / 0.51f), height / (cropRect.height() / mZoom / 0.51f)
                        )
                    )
                }
                if (!mAutoZoomEnabled) {
                    newZoom = 1f
                }
                if (newZoom > 0 && newZoom != mZoom) {
                    if (animate) {
                        if (mAnimation == null) {
                            // lazy create animation single instance
                            mAnimation = CropImageAnimation(mImageView, mCropOverlayView)
                        }
                        // set the state for animation to start from
                        mAnimation!!.setStartState(mImagePoints, mImageMatrix)
                    }
                    mZoom = newZoom
                    applyImageMatrix(width.toFloat(), height.toFloat(), true, animate)
                }
            }
        }
    }

    /**
     * Apply matrix to handle the image inside the image view.
     *
     * @param width the width of the image view
     * @param height the height of the image view
     */
    private fun applyImageMatrix(width: Float, height: Float, center: Boolean, animate: Boolean) {
        if (mBitmap != null && width > 0 && height > 0) {
            mImageMatrix.invert(mImageInverseMatrix)
            val cropRect = mCropOverlayView.getCropWindowRect()
            mImageInverseMatrix.mapRect(cropRect)
            mImageMatrix.reset()
            // move the image to the center of the image view first so we can manipulate it from there
            mImageMatrix.postTranslate((width - mBitmap!!.width) / 2, (height - mBitmap!!.height) / 2)
            mapImagePointsByImageMatrix()
            // rotate the image the required degrees from center of image
            if (mDegreesRotated > 0) {
                mImageMatrix.postRotate(
                    mDegreesRotated.toFloat(),
                    BitmapUtils.getRectCenterX(mImagePoints),
                    BitmapUtils.getRectCenterY(mImagePoints)
                )
                mapImagePointsByImageMatrix()
            }
            // scale the image to the image view, image rect transformed to know new width/height
            val scale = min(
                width / BitmapUtils.getRectWidth(mImagePoints),
                height / BitmapUtils.getRectHeight(mImagePoints)
            )
            if (mScaleType == ScaleType.FIT_CENTER || mScaleType == ScaleType.CENTER_INSIDE && scale < 1 || scale > 1 && mAutoZoomEnabled) {
                mImageMatrix.postScale(
                    scale,
                    scale,
                    BitmapUtils.getRectCenterX(mImagePoints),
                    BitmapUtils.getRectCenterY(mImagePoints)
                )
                mapImagePointsByImageMatrix()
            }
            // scale by the current zoom level
            val scaleX = if (mFlipHorizontally) -mZoom else mZoom
            val scaleY = if (mFlipVertically) -mZoom else mZoom
            mImageMatrix.postScale(
                scaleX,
                scaleY,
                BitmapUtils.getRectCenterX(mImagePoints),
                BitmapUtils.getRectCenterY(mImagePoints)
            )
            mapImagePointsByImageMatrix()
            mImageMatrix.mapRect(cropRect)
            if (center) {
                // set the zoomed area to be as to the center of cropping window as possible
                mZoomOffsetX = if (width > BitmapUtils.getRectWidth(mImagePoints))
                    0F
                else
                    max(
                        min(
                            width / 2 - cropRect.centerX(), -BitmapUtils.getRectLeft(mImagePoints)
                        ),
                        getWidth() - BitmapUtils.getRectRight(mImagePoints)
                    ) / scaleX
                mZoomOffsetY = if (height > BitmapUtils.getRectHeight(mImagePoints))
                    0F
                else
                    (max(
                        min(
                            height / 2 - cropRect.centerY(), -BitmapUtils.getRectTop(mImagePoints)
                        ),
                        getHeight() - BitmapUtils.getRectBottom(mImagePoints)
                    ) / scaleY)
            } else {
                // adjust the zoomed area so the crop window rectangle will be inside the area in case it was moved outside
                mZoomOffsetX = min(
                    max(mZoomOffsetX * scaleX, -cropRect.left),
                    -cropRect.right + width
                ) / scaleX
                mZoomOffsetY = min(
                    max(mZoomOffsetY * scaleY, -cropRect.top),
                    -cropRect.bottom + height
                ) / scaleY
            }

            // apply to zoom offset translate and update the crop rectangle to offset correctly
            mImageMatrix.postTranslate(mZoomOffsetX * scaleX, mZoomOffsetY * scaleY)
            cropRect.offset(mZoomOffsetX * scaleX, mZoomOffsetY * scaleY)
            mCropOverlayView.setCropWindowRect(cropRect)
            mapImagePointsByImageMatrix()
            mCropOverlayView.invalidate()

            // set matrix to apply
            if (animate) {
                // set the state for animation to end in, start animation now
                mAnimation?.setEndState(mImagePoints, mImageMatrix)
                mImageView.startAnimation(mAnimation)
            } else {
                mImageView.imageMatrix = mImageMatrix
            }
            // update the image rectangle in the crop overlay
            updateImageBounds(false)
        }
    }

    /**
     * Adjust the given image rectangle by image transformation matrix to know the final rectangle of
     * the image.<br></br>
     * To get the proper rectangle it must be first reset to original image rectangle.
     */
    private fun mapImagePointsByImageMatrix() {
        mImagePoints[0] = 0F
        mImagePoints[1] = 0F
        mImagePoints[2] = mBitmap!!.width.toFloat()
        mImagePoints[3] = 0F
        mImagePoints[4] = mBitmap!!.width.toFloat()
        mImagePoints[5] = mBitmap!!.height.toFloat()
        mImagePoints[6] = 0F
        mImagePoints[7] = mBitmap!!.height.toFloat()
        mImageMatrix.mapPoints(mImagePoints)
        mScaleImagePoints[0] = 0F
        mScaleImagePoints[1] = 0F
        mScaleImagePoints[2] = 100F
        mScaleImagePoints[3] = 0F
        mScaleImagePoints[4] = 100F
        mScaleImagePoints[5] = 100F
        mScaleImagePoints[6] = 0F
        mScaleImagePoints[7] = 100F
        mImageMatrix.mapPoints(mScaleImagePoints)
    }

    /**
     * Determines the specs for the onMeasure function. Calculates the width or height depending on
     * the mode.
     *
     * @param measureSpecMode The mode of the measured width or height.
     * @param measureSpecSize The size of the measured width or height.
     * @param desiredSize The desired size of the measured width or height.
     * @return The final size of the width or height.
     */
    private fun getOnMeasureSpec(measureSpecMode: Int, measureSpecSize: Int, desiredSize: Int): Int {
        // Measure Width
        return when (measureSpecMode) {
            MeasureSpec.EXACTLY -> {
                // Must be this size
                measureSpecSize
            }
            MeasureSpec.AT_MOST -> {
                // Can't be bigger than...; match_parent value
                min(desiredSize, measureSpecSize)
            }
            else -> {
                // Be whatever you want; wrap_content
                desiredSize
            }
        }
    }

    /** Update the scale factor between the actual image bitmap and the shown image.<br></br>  */
    private fun updateImageBounds(clear: Boolean) {
        if (mBitmap != null && !clear) {
            // Get the scale factor between the actual Bitmap dimensions and the displayed dimensions for width/height.
            val scaleFactorWidth = 100f / BitmapUtils.getRectWidth(mScaleImagePoints)
            val scaleFactorHeight = 100f / BitmapUtils.getRectHeight(mScaleImagePoints)
            mCropOverlayView.setCropWindowLimits(
                width.toFloat(), height.toFloat(), scaleFactorWidth, scaleFactorHeight
            )
        }
        // set the bitmap rectangle and update the crop window after scale factor is set
        mCropOverlayView.setBounds(if (clear) null else mImagePoints, width, height)
    }

    private class CropImageAnimation(private val mImageView: ImageView, private val mCropOverlayView: CropOverlayView) : Animation(), Animation.AnimationListener {
        private val mStartBoundPoints = FloatArray(8)
        private val mEndBoundPoints = FloatArray(8)
        private val mStartCropWindowRect = RectF()
        private val mEndCropWindowRect = RectF()
        private val mStartImageMatrix = FloatArray(9)
        private val mEndImageMatrix = FloatArray(9)
        private val mAnimRect = RectF()
        private val mAnimPoints = FloatArray(8)
        private val mAnimMatrix = FloatArray(9)

        init {
            duration = 300
            fillAfter = true
            interpolator = AccelerateDecelerateInterpolator()
            setAnimationListener(this)
        }

        fun setStartState(boundPoints: FloatArray, imageMatrix: Matrix) {
            reset()
            System.arraycopy(boundPoints, 0, mStartBoundPoints, 0, 8)
            mStartCropWindowRect.set(mCropOverlayView.getCropWindowRect())
            imageMatrix.getValues(mStartImageMatrix)
        }

        fun setEndState(boundPoints: FloatArray, imageMatrix: Matrix) {
            System.arraycopy(boundPoints, 0, mEndBoundPoints, 0, 8)
            mEndCropWindowRect.set(mCropOverlayView.getCropWindowRect())
            imageMatrix.getValues(mEndImageMatrix)
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            mAnimRect.left = mStartCropWindowRect.left + (mEndCropWindowRect.left - mStartCropWindowRect.left) * interpolatedTime
            mAnimRect.top = mStartCropWindowRect.top + (mEndCropWindowRect.top - mStartCropWindowRect.top) * interpolatedTime
            mAnimRect.right = mStartCropWindowRect.right + (mEndCropWindowRect.right - mStartCropWindowRect.right) * interpolatedTime
            mAnimRect.bottom = mStartCropWindowRect.bottom + (mEndCropWindowRect.bottom - mStartCropWindowRect.bottom) * interpolatedTime
            mCropOverlayView.setCropWindowRect(mAnimRect)
            for (i in mAnimPoints.indices) {
                mAnimPoints[i] =
                    mStartBoundPoints[i] + (mEndBoundPoints[i] - mStartBoundPoints[i]) * interpolatedTime
            }
            mCropOverlayView.setBounds(mAnimPoints, mImageView.width, mImageView.height)

            for (i in mAnimMatrix.indices) {
                mAnimMatrix[i] = mStartImageMatrix[i] + (mEndImageMatrix[i] - mStartImageMatrix[i]) * interpolatedTime
            }
            val m = mImageView.imageMatrix
            m.setValues(mAnimMatrix)
            mImageView.imageMatrix = m
            mImageView.invalidate()
            mCropOverlayView.invalidate()
        }

        override fun onAnimationStart(animation: Animation) {}

        override fun onAnimationEnd(animation: Animation) {
            mImageView.clearAnimation()
        }

        override fun onAnimationRepeat(animation: Animation) {}
    }

    enum class CropShape {
        RECTANGLE,
        OVAL
    }

    enum class ScaleType {
        FIT_CENTER,
        CENTER,
        CENTER_CROP,
        CENTER_INSIDE
    }

    /** The possible guidelines showing types.  */
    enum class Guidelines {
        /** Never show  */
        OFF,

        /** Show when crop move action is live  */
        ON_TOUCH,

        /** Always show  */
        ON
    }

    /** Possible options for handling requested width/height for cropping.  */
    enum class RequestSizeOptions {

        /** No resize/sampling is done unless required for memory management (OOM).  */
        NONE,

        /**
         * Only sample the image during loading (if image set using URI) so the smallest of the image
         * dimensions will be between the requested size and x2 requested size.<br></br>
         * NOTE: resulting image will not be exactly requested width/height see: [Loading
 * Large Bitmaps Efficiently](http://developer.android.com/training/displaying-bitmaps/load-bitmap.html).
         */
        SAMPLING,

        /**
         * Resize the image uniformly (maintain the image's aspect ratio) so that both dimensions (width
         * and height) of the image will be equal to or **less** than the corresponding requested
         * dimension.<br></br>
         * If the image is smaller than the requested size it will NOT change.
         */
        RESIZE_INSIDE,

        /**
         * Resize the image uniformly (maintain the image's aspect ratio) to fit in the given
         * width/height.<br></br>
         * The largest dimension will be equals to the requested and the second dimension will be
         * smaller.<br></br>
         * If the image is smaller than the requested size it will enlarge it.
         */
        RESIZE_FIT,

        /**
         * Resize the image to fit exactly in the given width/height.<br></br>
         * This resize method does NOT preserve aspect ratio.<br></br>
         * If the image is smaller than the requested size it will enlarge it.
         */
        RESIZE_EXACT
    }
}