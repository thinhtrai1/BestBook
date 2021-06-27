package com.app.bestbook.view.cropImageView

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.app.bestbook.R
import java.util.*
import kotlin.math.*

class CropOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mMultiTouchEnabled: Boolean = false
    private val mCropWindowHandler = CropWindowHandler()
    private val mDrawRect = RectF()
    private var mBorderPaint: Paint? = null
    private var mBorderCornerPaint: Paint? = null
    private var mGuidelinePaint: Paint? = null
    private var mBackgroundPaint: Paint? = null
    private val mPath = Path()
    private val mBoundsPoints = FloatArray(8)
    private val mCalcBounds = RectF()
    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0
    private var mBorderCornerOffset: Float = 0f
    private var mBorderCornerLength: Float = 0f
    private var mInitialCropWindowPaddingRatio: Float = 0f
    private var mTouchRadius: Float = 0f
    private var mSnapRadius: Float = 0f
    private var mMoveHandler: CropWindowMoveHandler? = null
    private var mFixAspectRatio: Boolean = false
    private var mAspectRatioX: Int = 0
    private var mAspectRatioY: Int = 0
    private var mTargetAspectRatio = mAspectRatioX.toFloat() / mAspectRatioY
    private var mGuidelines: CropImageView.Guidelines? = null
    private var mCropShape: CropImageView.CropShape? = null
    private val mInitialCropWindowRect = Rect()
    private var initializedCropWindow: Boolean = false
    private var mOriginalLayerType: Int? = null
    private lateinit var onCropWindowChanged: (Boolean) -> Unit

    fun setOnCropWindowChanged(callback: (Boolean) -> Unit) {
        onCropWindowChanged = callback
    }

    /** Get the left/top/right/bottom coordinates of the crop window.  */
    fun getCropWindowRect(): RectF {
        return mCropWindowHandler.getRect()
    }

    /** Set the left/top/right/bottom coordinates of the crop window.  */
    fun setCropWindowRect(rect: RectF) {
        mCropWindowHandler.setRect(rect)
    }

    /** Fix the current crop window rectangle if it is outside of cropping image or view bounds.  */
    fun fixCurrentCropWindowRect() {
        val rect = getCropWindowRect()
        fixCropWindowRectByRules(rect)
        mCropWindowHandler.setRect(rect)
    }

    /**
     * Informs the CropOverlayView of the image's position relative to the ImageView. This is necessary to call in order to draw the crop window.
     *
     * @param boundsPoints the image's bounding points
     * @param viewWidth The bounding image view width.
     * @param viewHeight The bounding image view height.
     */
    fun setBounds(boundsPoints: FloatArray?, viewWidth: Int, viewHeight: Int) {
        if (boundsPoints == null || !Arrays.equals(mBoundsPoints, boundsPoints)) {
            if (boundsPoints == null) {
                Arrays.fill(mBoundsPoints, 0f)
            } else {
                System.arraycopy(boundsPoints, 0, mBoundsPoints, 0, boundsPoints.size)
            }
            mViewWidth = viewWidth
            mViewHeight = viewHeight
            val cropRect = mCropWindowHandler.getRect()
            if (cropRect.width() == 0f || cropRect.height() == 0f) {
                initCropWindow()
            }
        }
    }

    /** Resets the crop overlay view.  */
    fun resetCropOverlayView() {
        if (initializedCropWindow) {
            setCropWindowRect(BitmapUtils.EMPTY_RECT_F)
            initCropWindow()
            invalidate()
        }
    }

    fun setCropShape(cropShape: CropImageView.CropShape) {
        if (mCropShape !== cropShape) {
            mCropShape = cropShape
            if (mCropShape === CropImageView.CropShape.OVAL) {
                mOriginalLayerType = layerType
                if (mOriginalLayerType != LAYER_TYPE_SOFTWARE) {
                    // TURN off hardware acceleration
                    setLayerType(LAYER_TYPE_SOFTWARE, null)
                } else {
                    mOriginalLayerType = null
                }
            } else if (mOriginalLayerType != null) {
                // return hardware acceleration back
                setLayerType(mOriginalLayerType!!, null)
                mOriginalLayerType = null
            }
            invalidate()
        }
    }

    /** Sets the guidelines for the CropOverlayView to be either on, off, or to show when resizing the application. */
    fun setGuidelines(guidelines: CropImageView.Guidelines) {
        if (mGuidelines !== guidelines) {
            mGuidelines = guidelines
            if (initializedCropWindow) {
                invalidate()
            }
        }
    }

    fun isFixAspectRatio(): Boolean {
        return mFixAspectRatio
    }

    fun setFixedAspectRatio(fixAspectRatio: Boolean) {
        if (mFixAspectRatio != fixAspectRatio) {
            mFixAspectRatio = fixAspectRatio
            if (initializedCropWindow) {
                initCropWindow()
                invalidate()
            }
        }
    }

    fun getAspectRatioX(): Int {
        return mAspectRatioX
    }

    fun setAspectRatioX(aspectRatioX: Int) {
        require(aspectRatioX > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        if (mAspectRatioX != aspectRatioX) {
            mAspectRatioX = aspectRatioX
            mTargetAspectRatio = mAspectRatioX.toFloat() / mAspectRatioY

            if (initializedCropWindow) {
                initCropWindow()
                invalidate()
            }
        }
    }

    fun getAspectRatioY(): Int {
        return mAspectRatioY
    }

    fun setAspectRatioY(aspectRatioY: Int) {
        if (aspectRatioY <= 0) {
            throw IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.")
        } else if (mAspectRatioY != aspectRatioY) {
            mAspectRatioY = aspectRatioY
            mTargetAspectRatio = mAspectRatioX.toFloat() / mAspectRatioY
            if (initializedCropWindow) {
                initCropWindow()
                invalidate()
            }
        }
    }

    /**
     * An edge of the crop window will snap to the corresponding edge of a specified bounding box when
     * the crop window edge is less than or equal to this distance (in pixels) away from the bounding box edge. (default: 3)
     */
    fun setSnapRadius(snapRadius: Float) {
        mSnapRadius = snapRadius
    }

    /** Set multi touch functionality to enabled/disabled. */
    fun setMultiTouchEnabled(multiTouchEnabled: Boolean): Boolean {
        if (mMultiTouchEnabled != multiTouchEnabled) {
            mMultiTouchEnabled = multiTouchEnabled
            if (mMultiTouchEnabled && mScaleDetector == null) {
                mScaleDetector = ScaleGestureDetector(context, ScaleListener(mCropWindowHandler, this))
            }
            return true
        }
        return false
    }

    /** The min size the resulting cropping image is allowed to be, affects the cropping window limits (in pixels).<br></br> */
    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        mCropWindowHandler.setMinCropResultSize(minCropResultWidth, minCropResultHeight)
    }

    /** The max size the resulting cropping image is allowed to be, affects the cropping window limits (in pixels).<br></br> */
    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        mCropWindowHandler.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight)
    }

    /** Set the max width/height and scale factor of the shown image to original image to scale the limits appropriately. */
    fun setCropWindowLimits(maxWidth: Float, maxHeight: Float, scaleFactorWidth: Float, scaleFactorHeight: Float) {
        mCropWindowHandler.setCropWindowLimits(maxWidth, maxHeight, scaleFactorWidth, scaleFactorHeight)
    }

    /** Set crop window initial rectangle to be used instead of default. */
    fun setInitialCropWindowRect(rect: Rect?) {
        mInitialCropWindowRect.set(rect ?: BitmapUtils.EMPTY_RECT)
        if (initializedCropWindow) {
            initCropWindow()
            invalidate()
            callOnCropWindowChanged(false)
        }
    }

    /** Reset crop window to initial rectangle. */
    fun resetCropWindowRect() {
        if (initializedCropWindow) {
            initCropWindow()
            invalidate()
            callOnCropWindowChanged(false)
        }
    }

    fun setInitialAttributeValues(ta: TypedArray) {
        mCropWindowHandler.setInitialAttributeValues(ta)
        val dm = Resources.getSystem().displayMetrics

        setCropShape(CropImageView.CropShape.values()[ta.getInt(
            R.styleable.CropImageView_cropShape,
            CropImageView.CropShape.RECTANGLE.ordinal)]
        )
        setSnapRadius(ta.getDimension(
            R.styleable.CropImageView_cropSnapRadius,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        ))
        setGuidelines(
            CropImageView.Guidelines.values()[ta.getInt(
            R.styleable.CropImageView_cropGuidelines, CropImageView.Guidelines.ON_TOUCH.ordinal
        )])
        setFixedAspectRatio(ta.hasValue(R.styleable.CropImageView_cropAspectRatioX)
                && ta.hasValue(R.styleable.CropImageView_cropAspectRatioY)
                && !ta.hasValue(R.styleable.CropImageView_cropFixAspectRatio))
        setAspectRatioX(ta.getInteger(
            R.styleable.CropImageView_cropAspectRatioX,
            1
        ))
        setAspectRatioY( ta.getInteger(
            R.styleable.CropImageView_cropAspectRatioY,
            1
        ))
        setMultiTouchEnabled(ta.getBoolean(
            R.styleable.CropImageView_cropMultiTouchEnabled, 
            false
        ))
        mTouchRadius = ta.getDimension(
            R.styleable.CropImageView_cropTouchRadius,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, dm)
        )
        mInitialCropWindowPaddingRatio = ta.getFloat(
            R.styleable.CropImageView_cropInitialCropWindowPaddingRatio,
            0.1f
        )
        val borderLineThickness = ta.getDimension(
            R.styleable.CropImageView_cropBorderLineThickness,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        )
        require(borderLineThickness >= 0) { "Cannot set line thickness value to a number less than 0." }
        mBorderPaint = getNewPaintOrNull(
            borderLineThickness,
            ta.getInteger(
                R.styleable.CropImageView_cropBorderLineColor,
                Color.argb(170, 255, 255, 255)
            )
        )
        mBorderCornerOffset = ta.getDimension(
            R.styleable.CropImageView_cropBorderCornerOffset,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, dm)
        )
        mBorderCornerLength = ta.getDimension(
            R.styleable.CropImageView_cropBorderCornerLength,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, dm)
        )
        val borderCornerThickness = ta.getDimension(
            R.styleable.CropImageView_cropBorderCornerThickness,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, dm)
        )
        require(borderCornerThickness >= 0) { "Cannot set corner thickness value to a number less than 0." }
        mBorderCornerPaint = getNewPaintOrNull(
            borderCornerThickness,
            ta.getInteger(
                R.styleable.CropImageView_cropBorderCornerColor,
                Color.WHITE
            )
        )
        val guidelinesThickness = ta.getDimension(
            R.styleable.CropImageView_cropGuidelinesThickness,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm)
        )
        require(guidelinesThickness >= 0) { "Cannot set guidelines thickness value to a number less than 0." }
        mGuidelinePaint = getNewPaintOrNull(
            guidelinesThickness,
            ta.getInteger(
                R.styleable.CropImageView_cropGuidelinesColor,
                Color.argb(170, 255, 255, 255)
            )
        )
        mBackgroundPaint = getNewPaint(ta.getInteger(
            R.styleable.CropImageView_cropBackgroundColor,
            Color.argb(119, 0, 0, 0)
        ))
    }

    fun validate() {
        require(mTouchRadius >= 0) { "Cannot set touch radius value to a number <= 0 " }
        require(!(mInitialCropWindowPaddingRatio < 0 || mInitialCropWindowPaddingRatio >= 0.5)) { "Cannot set initial crop window padding value to a number < 0 or >= 0.5" }
        require(mAspectRatioX > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        require(mAspectRatioY > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        mCropWindowHandler.validate()
    }

    /** Set the initial crop window size and position. This is dependent on the size and position of the image being cropped. */
    private fun initCropWindow() {
        val leftLimit = max(BitmapUtils.getRectLeft(mBoundsPoints), 0F)
        val topLimit = max(BitmapUtils.getRectTop(mBoundsPoints), 0F)
        val rightLimit = min(BitmapUtils.getRectRight(mBoundsPoints), width.toFloat())
        val bottomLimit = min(BitmapUtils.getRectBottom(mBoundsPoints), height.toFloat())
        if (rightLimit <= leftLimit || bottomLimit <= topLimit) {
            return
        }
        val rect = RectF()
        // Tells the attribute functions the crop window has already been initialized
        initializedCropWindow = true
        val horizontalPadding = mInitialCropWindowPaddingRatio * (rightLimit - leftLimit)
        val verticalPadding = mInitialCropWindowPaddingRatio * (bottomLimit - topLimit)
        if (mInitialCropWindowRect.width() > 0 && mInitialCropWindowRect.height() > 0) {
            // Get crop window position relative to the displayed image.
            rect.left = leftLimit + mInitialCropWindowRect.left / mCropWindowHandler.getScaleFactorWidth()
            rect.top = topLimit + mInitialCropWindowRect.top / mCropWindowHandler.getScaleFactorHeight()
            rect.right = rect.left + mInitialCropWindowRect.width() / mCropWindowHandler.getScaleFactorWidth()
            rect.bottom = rect.top + mInitialCropWindowRect.height() / mCropWindowHandler.getScaleFactorHeight()
            // Correct for floating point errors. Crop rect boundaries should not exceed the source Bitmap bounds.
            rect.left = max(leftLimit, rect.left)
            rect.top = max(topLimit, rect.top)
            rect.right = min(rightLimit, rect.right)
            rect.bottom = min(bottomLimit, rect.bottom)
        } else if (mFixAspectRatio && rightLimit > leftLimit && bottomLimit > topLimit) {
            // If the image aspect ratio is wider than the crop aspect ratio,
            // then the image height is the determining initial length. Else, vice-versa.
            val bitmapAspectRatio = (rightLimit - leftLimit) / (bottomLimit - topLimit)
            if (bitmapAspectRatio > mTargetAspectRatio) {
                rect.top = topLimit + verticalPadding
                rect.bottom = bottomLimit - verticalPadding
                val centerX = width / 2f
                // dirty fix for wrong crop overlay aspect ratio when using fixed aspect ratio
                mTargetAspectRatio = mAspectRatioX.toFloat() / mAspectRatioY
                // Limits the aspect ratio to no less than 40 wide or 40 tall
                val cropWidth = max(mCropWindowHandler.getMinCropWidth(), rect.height() * mTargetAspectRatio)
                val halfCropWidth = cropWidth / 2f
                rect.left = centerX - halfCropWidth
                rect.right = centerX + halfCropWidth
            } else {
                rect.left = leftLimit + horizontalPadding
                rect.right = rightLimit - horizontalPadding
                val centerY = height / 2f
                // Limits the aspect ratio to no less than 40 wide or 40 tall
                val cropHeight = max(mCropWindowHandler.getMinCropHeight(), rect.width() / mTargetAspectRatio)
                val halfCropHeight = cropHeight / 2f
                rect.top = centerY - halfCropHeight
                rect.bottom = centerY + halfCropHeight
            }
        } else {
            // Initialize crop window to have 10% padding w/ respect to image.
            rect.left = leftLimit + horizontalPadding
            rect.top = topLimit + verticalPadding
            rect.right = rightLimit - horizontalPadding
            rect.bottom = bottomLimit - verticalPadding
        }
        fixCropWindowRectByRules(rect)
        mCropWindowHandler.setRect(rect)
    }

    /** Fix the given rect to fit into bitmap rect and follow min, max and aspect ratio rules. */
    private fun fixCropWindowRectByRules(rect: RectF) {
        if (rect.width() < mCropWindowHandler.getMinCropWidth()) {
            val adj = (mCropWindowHandler.getMinCropWidth() - rect.width()) / 2
            rect.left -= adj
            rect.right += adj
        }
        if (rect.height() < mCropWindowHandler.getMinCropHeight()) {
            val adj = (mCropWindowHandler.getMinCropHeight() - rect.height()) / 2
            rect.top -= adj
            rect.bottom += adj
        }
        if (rect.width() > mCropWindowHandler.getMaxCropWidth()) {
            val adj = (rect.width() - mCropWindowHandler.getMaxCropWidth()) / 2
            rect.left += adj
            rect.right -= adj
        }
        if (rect.height() > mCropWindowHandler.getMaxCropHeight()) {
            val adj = (rect.height() - mCropWindowHandler.getMaxCropHeight()) / 2
            rect.top += adj
            rect.bottom -= adj
        }

        calculateBounds(rect)
        if (mCalcBounds.width() > 0 && mCalcBounds.height() > 0) {
            val leftLimit = max(mCalcBounds.left, 0f)
            val topLimit = max(mCalcBounds.top, 0f)
            val rightLimit = min(mCalcBounds.right, width.toFloat())
            val bottomLimit = min(mCalcBounds.bottom, height.toFloat())
            if (rect.left < leftLimit) {
                rect.left = leftLimit
            }
            if (rect.top < topLimit) {
                rect.top = topLimit
            }
            if (rect.right > rightLimit) {
                rect.right = rightLimit
            }
            if (rect.bottom > bottomLimit) {
                rect.bottom = bottomLimit
            }
        }
        if (mFixAspectRatio && abs(rect.width() - rect.height() * mTargetAspectRatio) > 0.1) {
            if (rect.width() > rect.height() * mTargetAspectRatio) {
                val adj = abs(rect.height() * mTargetAspectRatio - rect.width()) / 2
                rect.left += adj
                rect.right -= adj
            } else {
                val adj = abs(rect.width() / mTargetAspectRatio - rect.height()) / 2
                rect.top += adj
                rect.bottom -= adj
            }
        }
    }

    /** Draw crop overview by drawing background over image not in the cripping area, then borders and guidelines. */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw translucent background for the cropped area.
        drawBackground(canvas)
        if (mCropWindowHandler.showGuidelines()) {
            // Determines whether guidelines should be drawn or not
            if (mGuidelines === CropImageView.Guidelines.ON) {
                drawGuidelines(canvas)
            } else if (mGuidelines === CropImageView.Guidelines.ON_TOUCH && mMoveHandler != null) {
                // Draw only when resizing
                drawGuidelines(canvas)
            }
        }
        drawBorders(canvas)
        drawCorners(canvas)
    }

    /** Draw shadow background over the image not including the crop area.  */
    private fun drawBackground(canvas: Canvas) {
        val rect = mCropWindowHandler.getRect()
        val left = max(BitmapUtils.getRectLeft(mBoundsPoints), 0F)
        val top = max(BitmapUtils.getRectTop(mBoundsPoints), 0F)
        val right = min(BitmapUtils.getRectRight(mBoundsPoints), width.toFloat())
        val bottom = min(BitmapUtils.getRectBottom(mBoundsPoints), height.toFloat())

        if (mCropShape === CropImageView.CropShape.RECTANGLE) {
            if (!isNonStraightAngleRotated()) {
                canvas.drawRect(left, top, right, rect.top, mBackgroundPaint!!)
                canvas.drawRect(left, rect.bottom, right, bottom, mBackgroundPaint!!)
                canvas.drawRect(left, rect.top, rect.left, rect.bottom, mBackgroundPaint!!)
                canvas.drawRect(rect.right, rect.top, right, rect.bottom, mBackgroundPaint!!)
            } else {
                mPath.reset()
                mPath.moveTo(mBoundsPoints[0], mBoundsPoints[1])
                mPath.lineTo(mBoundsPoints[2], mBoundsPoints[3])
                mPath.lineTo(mBoundsPoints[4], mBoundsPoints[5])
                mPath.lineTo(mBoundsPoints[6], mBoundsPoints[7])
                mPath.close()
                canvas.save()
                canvas.clipPath(mPath)
                canvas.clipRect(rect, Region.Op.XOR)
                canvas.drawRect(left, top, right, bottom, mBackgroundPaint!!)
                canvas.restore()
            }
        } else {
            mPath.reset()
            if (mCropShape === CropImageView.CropShape.OVAL) {
                mDrawRect.set(rect.left + 2, rect.top + 2, rect.right - 2, rect.bottom - 2)
            } else {
                mDrawRect.set(rect.left, rect.top, rect.right, rect.bottom)
            }
            mPath.addOval(mDrawRect, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(mPath, Region.Op.XOR)
            canvas.drawRect(left, top, right, bottom, mBackgroundPaint!!)
            canvas.restore()
        }
    }

    /** Draw 2 veritcal and 2 horizontal guidelines inside the cropping area to split it into 9 equal parts. */
    private fun drawGuidelines(canvas: Canvas) {
        if (mGuidelinePaint != null) {
            val sw = if (mBorderPaint != null) mBorderPaint!!.strokeWidth else 0F
            val rect = mCropWindowHandler.getRect()
            rect.inset(sw, sw)
            val oneThirdCropWidth = rect.width() / 3
            val oneThirdCropHeight = rect.height() / 3

            if (mCropShape === CropImageView.CropShape.OVAL) {
                val w = rect.width() / 2 - sw
                val h = rect.height() / 2 - sw

                // Draw vertical guidelines.
                val x1 = rect.left + oneThirdCropWidth
                val x2 = rect.right - oneThirdCropWidth
                val yv =
                    (h * sin(acos(((w - oneThirdCropWidth) / w).toDouble()))).toFloat()
                canvas.drawLine(x1, rect.top + h - yv, x1, rect.bottom - h + yv, mGuidelinePaint!!)
                canvas.drawLine(x2, rect.top + h - yv, x2, rect.bottom - h + yv, mGuidelinePaint!!)

                // Draw horizontal guidelines.
                val y1 = rect.top + oneThirdCropHeight
                val y2 = rect.bottom - oneThirdCropHeight
                val xv =
                    (w * cos(asin(((h - oneThirdCropHeight) / h).toDouble()))).toFloat()
                canvas.drawLine(rect.left + w - xv, y1, rect.right - w + xv, y1, mGuidelinePaint!!)
                canvas.drawLine(rect.left + w - xv, y2, rect.right - w + xv, y2, mGuidelinePaint!!)
            } else {

                // Draw vertical guidelines.
                val x1 = rect.left + oneThirdCropWidth
                val x2 = rect.right - oneThirdCropWidth
                canvas.drawLine(x1, rect.top, x1, rect.bottom, mGuidelinePaint!!)
                canvas.drawLine(x2, rect.top, x2, rect.bottom, mGuidelinePaint!!)

                // Draw horizontal guidelines.
                val y1 = rect.top + oneThirdCropHeight
                val y2 = rect.bottom - oneThirdCropHeight
                canvas.drawLine(rect.left, y1, rect.right, y1, mGuidelinePaint!!)
                canvas.drawLine(rect.left, y2, rect.right, y2, mGuidelinePaint!!)
            }
        }
    }

    /** Draw borders of the crop area. */
    private fun drawBorders(canvas: Canvas) {
        if (mBorderPaint != null) {
            val w = mBorderPaint!!.strokeWidth
            val rect = mCropWindowHandler.getRect()
            rect.inset(w / 2, w / 2)

            if (mCropShape === CropImageView.CropShape.RECTANGLE) {
                // Draw rectangle crop window border.
                canvas.drawRect(rect, mBorderPaint!!)
            } else {
                // Draw circular crop window border
                canvas.drawOval(rect, mBorderPaint!!)
            }
        }
    }

    /** Draw the corner of crop overlay. */
    private fun drawCorners(canvas: Canvas) {
        if (mBorderCornerPaint != null) {
            val lineWidth = if (mBorderPaint != null) mBorderPaint!!.strokeWidth else 0F
            val cornerWidth = mBorderCornerPaint!!.strokeWidth

            // for rectangle crop shape we allow the corners to be offset from the borders
            val w = cornerWidth / 2 + if (mCropShape === CropImageView.CropShape.RECTANGLE) mBorderCornerOffset else 0F
            val rect = mCropWindowHandler.getRect()
            rect.inset(w, w)
            val cornerOffset = (cornerWidth - lineWidth) / 2
            val cornerExtension = cornerWidth / 2 + cornerOffset

            // Top left
            canvas.drawLine(
                rect.left - cornerOffset,
                rect.top - cornerExtension,
                rect.left - cornerOffset,
                rect.top + mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.left - cornerExtension,
                rect.top - cornerOffset,
                rect.left + mBorderCornerLength,
                rect.top - cornerOffset,
                mBorderCornerPaint!!
            )
            // Top right
            canvas.drawLine(
                rect.right + cornerOffset,
                rect.top - cornerExtension,
                rect.right + cornerOffset,
                rect.top + mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.right + cornerExtension,
                rect.top - cornerOffset,
                rect.right - mBorderCornerLength,
                rect.top - cornerOffset,
                mBorderCornerPaint!!
            )
            // Bottom left
            canvas.drawLine(
                rect.left - cornerOffset,
                rect.bottom + cornerExtension,
                rect.left - cornerOffset,
                rect.bottom - mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.left - cornerExtension,
                rect.bottom + cornerOffset,
                rect.left + mBorderCornerLength,
                rect.bottom + cornerOffset,
                mBorderCornerPaint!!
            )
            // Bottom left
            canvas.drawLine(
                rect.right + cornerOffset,
                rect.bottom + cornerExtension,
                rect.right + cornerOffset,
                rect.bottom - mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.right + cornerExtension,
                rect.bottom + cornerOffset,
                rect.right - mBorderCornerLength,
                rect.bottom + cornerOffset,
                mBorderCornerPaint!!
            )
        }
    }

    /** Creates the Paint object for drawing.  */
    private fun getNewPaint(color: Int): Paint {
        val paint = Paint()
        paint.color = color
        return paint
    }

    /** Creates the Paint object for given thickness and color, if thickness < 0 return null.  */
    private fun getNewPaintOrNull(thickness: Float, color: Int): Paint? {
        return if (thickness > 0) {
            val borderPaint = Paint()
            borderPaint.color = color
            borderPaint.strokeWidth = thickness
            borderPaint.style = Paint.Style.STROKE
            borderPaint.isAntiAlias = true
            borderPaint
        } else {
            null
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnabled) {
            if (mMultiTouchEnabled) {
                mScaleDetector!!.onTouchEvent(event)
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDown(event.x, event.y)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                    onActionUp()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    onActionMove(event.x, event.y)
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                else -> return false
            }
        } else {
            return false
        }
    }

    /**
     * On press down start crop window movment depending on the location of the press.<br></br>
     * if press is far from crop window then no move handler is returned (null).
     */
    private fun onActionDown(x: Float, y: Float) {
        mMoveHandler = mCropWindowHandler.getMoveHandler(x, y, mTouchRadius, mCropShape)
        if (mMoveHandler != null) {
            invalidate()
        }
    }

    /** Clear move handler starting in [.onActionDown] if exists. */
    private fun onActionUp() {
        if (mMoveHandler != null) {
            mMoveHandler = null
            invalidate()
            callOnCropWindowChanged(false)
        }
    }

    /**
     * Handle move of crop window using the move handler created in [.onActionDown].<br></br>
     * The move handler will do the proper move/resize of the crop window.
     */
    private fun onActionMove(x: Float, y: Float) {
        if (mMoveHandler != null) {
            var snapRadius = mSnapRadius
            val rect = mCropWindowHandler.getRect()
            if (calculateBounds(rect)) {
                snapRadius = 0f
            }
            mMoveHandler!!.move(
                rect,
                x,
                y,
                mCalcBounds,
                mViewWidth,
                mViewHeight,
                snapRadius,
                mFixAspectRatio,
                mTargetAspectRatio
            )
            mCropWindowHandler.setRect(rect)
            invalidate()
            callOnCropWindowChanged(true)
        }
    }

    /**
     * Calculate the bounding rectangle for current crop window, handle non-straight rotation angles.<br></br>
     * If the rotation angle is straight then the bounds rectangle is the bitmap rectangle, otherwsie
     * we find the max rectangle that is within the image bounds starting from the crop window rectangle.
     *
     * @param rect the crop window rectangle to start finsing bounded rectangle from
     * @return true - non straight rotation in place, false - otherwise.
     */
    private fun calculateBounds(rect: RectF): Boolean {
        var left = BitmapUtils.getRectLeft(mBoundsPoints)
        var top = BitmapUtils.getRectTop(mBoundsPoints)
        var right = BitmapUtils.getRectRight(mBoundsPoints)
        var bottom = BitmapUtils.getRectBottom(mBoundsPoints)

        if (!isNonStraightAngleRotated()) {
            mCalcBounds.set(left, top, right, bottom)
            return false
        } else {
            var x0 = mBoundsPoints[0]
            var y0 = mBoundsPoints[1]
            var x2 = mBoundsPoints[4]
            var y2 = mBoundsPoints[5]
            var x3 = mBoundsPoints[6]
            var y3 = mBoundsPoints[7]

            if (mBoundsPoints[7] < mBoundsPoints[1]) {
                if (mBoundsPoints[1] < mBoundsPoints[3]) {
                    x0 = mBoundsPoints[6]
                    y0 = mBoundsPoints[7]
                    x2 = mBoundsPoints[2]
                    y2 = mBoundsPoints[3]
                    x3 = mBoundsPoints[4]
                    y3 = mBoundsPoints[5]
                } else {
                    x0 = mBoundsPoints[4]
                    y0 = mBoundsPoints[5]
                    x2 = mBoundsPoints[0]
                    y2 = mBoundsPoints[1]
                    x3 = mBoundsPoints[2]
                    y3 = mBoundsPoints[3]
                }
            } else if (mBoundsPoints[1] > mBoundsPoints[3]) {
                x0 = mBoundsPoints[2]
                y0 = mBoundsPoints[3]
                x2 = mBoundsPoints[6]
                y2 = mBoundsPoints[7]
                x3 = mBoundsPoints[0]
                y3 = mBoundsPoints[1]
            }

            val a0 = (y3 - y0) / (x3 - x0)
            val a1 = -1f / a0
            val b0 = y0 - a0 * x0
            val b1 = y0 - a1 * x0
            val b2 = y2 - a0 * x2
            val b3 = y2 - a1 * x2
            val c0 = (rect.centerY() - rect.top) / (rect.centerX() - rect.left)
            val c1 = -c0
            val d0 = rect.top - c0 * rect.left
            val d1 = rect.top - c1 * rect.right

            left = max(
                left,
                if ((d0 - b0) / (a0 - c0) < rect.right) (d0 - b0) / (a0 - c0) else left
            )
            left = max(
                left,
                if ((d0 - b1) / (a1 - c0) < rect.right) (d0 - b1) / (a1 - c0) else left
            )
            left = max(
                left,
                if ((d1 - b3) / (a1 - c1) < rect.right) (d1 - b3) / (a1 - c1) else left
            )
            right = min(
                right,
                if ((d1 - b1) / (a1 - c1) > rect.left) (d1 - b1) / (a1 - c1) else right
            )
            right = min(
                right,
                if ((d1 - b2) / (a0 - c1) > rect.left) (d1 - b2) / (a0 - c1) else right
            )
            right = min(
                right,
                if ((d0 - b2) / (a0 - c0) > rect.left) (d0 - b2) / (a0 - c0) else right
            )
            top = max(
                top,
                max(a0 * left + b0, a1 * right + b1)
            )
            bottom = min(
                bottom,
                min(a1 * left + b3, a0 * right + b2)
            )

            mCalcBounds.left = left
            mCalcBounds.top = top
            mCalcBounds.right = right
            mCalcBounds.bottom = bottom
            return true
        }
    }

    /** Is the cropping image has been rotated by NOT 0,90,180 or 270 degrees.  */
    private fun isNonStraightAngleRotated(): Boolean {
        return mBoundsPoints[0] != mBoundsPoints[6] && mBoundsPoints[1] != mBoundsPoints[7]
    }

    private fun callOnCropWindowChanged(inProgress: Boolean) {
        try {
            onCropWindowChanged(inProgress)
        } catch (e: Exception) {
            Log.e("AIC", "Exception in crop window changed", e)
        }
    }

    private class ScaleListener(private val mCropWindowHandler: CropWindowHandler, private val mCropOverlayView: CropOverlayView) : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val rect = mCropWindowHandler.getRect()
            val x = detector.focusX
            val y = detector.focusY
            val dY = detector.currentSpanY / 2
            val dX = detector.currentSpanX / 2
            val newTop = y - dY
            val newLeft = x - dX
            val newRight = x + dX
            val newBottom = y + dY
            if (newLeft < newRight
                && newTop <= newBottom
                && newLeft >= 0
                && newRight <= mCropWindowHandler.getMaxCropWidth()
                && newTop >= 0
                && newBottom <= mCropWindowHandler.getMaxCropHeight()
            ) {
                rect.set(newLeft, newTop, newRight, newBottom)
                mCropWindowHandler.setRect(rect)
                mCropOverlayView.invalidate()
            }
            return true
        }
    }

    private class CropWindowHandler {
        private val mEdges = RectF()

        /** Rectangle used to return the edges rectangle without ability to change it and without creating new all the time. */
        private val mGetEdges = RectF()

        /** Minimum width in pixels that the crop window can get.  */
        private var mMinCropWindowWidth: Float = 0.toFloat()

        /** Minimum height in pixels that the crop window can get.  */
        private var mMinCropWindowHeight: Float = 0.toFloat()

        /** Maximum width in pixels that the crop window can CURRENTLY get.  */
        private var mMaxCropWindowWidth: Float = 0.toFloat()

        /** Maximum height in pixels that the crop window can CURRENTLY get.  */
        private var mMaxCropWindowHeight: Float = 0.toFloat()

        /** Minimum width in pixels that the result of cropping an image can get, affects crop window width adjusted by width scale factor. */
        private var mMinCropResultWidth: Float = 0.toFloat()

        /** Minimum height in pixels that the result of cropping an image can get, affects crop window height adjusted by height scale factor. */
        private var mMinCropResultHeight: Float = 0.toFloat()

        /** Maximum width in pixels that the result of cropping an image can get, affects crop window width adjusted by width scale factor. */
        private var mMaxCropResultWidth: Float = 0.toFloat()

        /** Maximum height in pixels that the result of cropping an image can get, affects crop window height adjusted by height scale factor. */
        private var mMaxCropResultHeight: Float = 0.toFloat()

        /** The width scale factor of shown image and actual image  */
        private var mScaleFactorWidth = 1f

        /** The height scale factor of shown image and actual image  */
        private var mScaleFactorHeight = 1f
        // endregion

        /** Get the left/top/right/bottom coordinates of the crop window.  */
        fun getRect(): RectF {
            mGetEdges.set(mEdges)
            return mGetEdges
        }

        /** Minimum width in pixels that the crop window can get.  */
        fun getMinCropWidth(): Float {
            return max(mMinCropWindowWidth, mMinCropResultWidth / mScaleFactorWidth)
        }

        /** Minimum height in pixels that the crop window can get.  */
        fun getMinCropHeight(): Float {
            return max(mMinCropWindowHeight, mMinCropResultHeight / mScaleFactorHeight)
        }

        /** Maximum width in pixels that the crop window can get.  */
        fun getMaxCropWidth(): Float {
            return min(mMaxCropWindowWidth, mMaxCropResultWidth / mScaleFactorWidth)
        }

        /** Maximum height in pixels that the crop window can get.  */
        fun getMaxCropHeight(): Float {
            return min(mMaxCropWindowHeight, mMaxCropResultHeight / mScaleFactorHeight)
        }

        /** get the scale factor (on width) of the showen image to original image.  */
        fun getScaleFactorWidth(): Float {
            return mScaleFactorWidth
        }

        /** get the scale factor (on height) of the showen image to original image.  */
        fun getScaleFactorHeight(): Float {
            return mScaleFactorHeight
        }

        /** The min size the resulting cropping image is allowed to be, affects the cropping window limits (in pixels).<br></br> */
        fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
            mMinCropResultWidth = minCropResultWidth.toFloat()
            mMinCropResultHeight = minCropResultHeight.toFloat()
        }

        /** The max size the resulting cropping image is allowed to be, affects the cropping window limits (in pixels).<br></br> */
        fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
            mMaxCropResultWidth = maxCropResultWidth.toFloat()
            mMaxCropResultHeight = maxCropResultHeight.toFloat()
        }

        /** Set the max width/height and scale factor of the showen image to original image to scale the limits appropriately. */
        fun setCropWindowLimits(maxWidth: Float, maxHeight: Float, scaleFactorWidth: Float, scaleFactorHeight: Float) {
            mMaxCropWindowWidth = maxWidth
            mMaxCropWindowHeight = maxHeight
            mScaleFactorWidth = scaleFactorWidth
            mScaleFactorHeight = scaleFactorHeight
        }

        /** Set the variables to be used during crop window handling. */
        fun setInitialAttributeValues(ta: TypedArray) {
            val dm = Resources.getSystem().displayMetrics
            mMinCropWindowWidth = ta.getDimension(
                R.styleable.CropImageView_cropMinCropWindowWidth,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, dm)
            )
            mMinCropWindowHeight = ta.getDimension(
                R.styleable.CropImageView_cropMinCropWindowHeight,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, dm)
            )
            mMinCropResultWidth = ta.getFloat(
                R.styleable.CropImageView_cropMinCropResultWidthPX,
                40f
            )
            mMinCropResultHeight = ta.getFloat(
                R.styleable.CropImageView_cropMinCropResultHeightPX,
                40f
            )
            mMaxCropResultWidth = ta.getFloat(
                R.styleable.CropImageView_cropMaxCropResultWidthPX,
                9999f
            )
            mMaxCropResultHeight = ta.getFloat(
                R.styleable.CropImageView_cropMaxCropResultHeightPX,
                9999f
            )
        }

        fun validate() {
            require(mMinCropWindowHeight >= 0) { "Cannot set min crop window height value to a number < 0 " }
            require(mMinCropResultWidth >= 0) { "Cannot set min crop result width value to a number < 0 " }
            require(mMinCropResultHeight >= 0) { "Cannot set min crop result height value to a number < 0 " }
            require(mMaxCropResultWidth >= mMinCropResultWidth) { "Cannot set max crop result width to smaller value than min crop result width" }
            require(mMaxCropResultHeight >= mMinCropResultHeight) { "Cannot set max crop result height to smaller value than min crop result height" }
        }

        /** Set the left/top/right/bottom coordinates of the crop window.  */
        fun setRect(rect: RectF) {
            mEdges.set(rect)
        }

        /**
         * Indicates whether the crop window is small enough that the guidelines should be shown. Public because this function is also used to determine if the center handle should be focused.
         *
         * @return boolean Whether the guidelines should be shown or not
         */
        fun showGuidelines(): Boolean {
            return !(mEdges.width() < 100 || mEdges.height() < 100)
        }

        /**
         * Determines which, if any, of the handles are pressed given the touch coordinates, the bounding box, and the touch radius.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param targetRadius the target radius in pixels
         * @return the Handle that was pressed; null if no Handle was pressed
         */
        fun getMoveHandler(x: Float, y: Float, targetRadius: Float, cropShape: CropImageView.CropShape?): CropWindowMoveHandler? {
            val type = if (cropShape === CropImageView.CropShape.OVAL)
                getOvalPressedMoveType(x, y)
            else
                getRectanglePressedMoveType(x, y, targetRadius)
            return if (type != null) CropWindowMoveHandler(type, this, x, y) else null
        }

        /**
         * Determines which, if any, of the handles are pressed given the touch coordinates, the bounding box, and the touch radius.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param targetRadius the target radius in pixels
         * @return the Handle that was pressed; null if no Handle was pressed
         */
        private fun getRectanglePressedMoveType(x: Float, y: Float, targetRadius: Float): CropWindowMoveHandler.Type? {
            // Note: corner-handles take precedence, then side-handles, then center.
            return when {
                isInCornerTargetZone(x, y, mEdges.left, mEdges.top, targetRadius) -> CropWindowMoveHandler.Type.TOP_LEFT
                isInCornerTargetZone(x, y, mEdges.right, mEdges.top, targetRadius) -> CropWindowMoveHandler.Type.TOP_RIGHT
                isInCornerTargetZone(x, y, mEdges.left, mEdges.bottom, targetRadius) -> CropWindowMoveHandler.Type.BOTTOM_LEFT
                isInCornerTargetZone(x, y, mEdges.right, mEdges.bottom, targetRadius) -> CropWindowMoveHandler.Type.BOTTOM_RIGHT
                isInCenterTargetZone(x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom) && focusCenter() -> CropWindowMoveHandler.Type.CENTER
                isInHorizontalTargetZone(x, y, mEdges.left, mEdges.right, mEdges.top, targetRadius) -> CropWindowMoveHandler.Type.TOP
                isInHorizontalTargetZone(x, y, mEdges.left, mEdges.right, mEdges.bottom, targetRadius) -> CropWindowMoveHandler.Type.BOTTOM
                isInVerticalTargetZone(x, y, mEdges.left, mEdges.top, mEdges.bottom, targetRadius) -> CropWindowMoveHandler.Type.LEFT
                isInVerticalTargetZone(x, y, mEdges.right, mEdges.top, mEdges.bottom, targetRadius) -> CropWindowMoveHandler.Type.RIGHT
                isInCenterTargetZone(x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom) && !focusCenter() -> CropWindowMoveHandler.Type.CENTER
                else -> null
            }
        }

        /**
         * Determines which, if any, of the handles are pressed given the touch coordinates, the bounding
         * box/oval, and the touch radius.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @return the Handle that was pressed; null if no Handle was pressed
         */
        private fun getOvalPressedMoveType(x: Float, y: Float): CropWindowMoveHandler.Type {
            /*
           Use a 6x6 grid system divided into 9 "handles", with the center the biggest region. While
           this is not perfect, it's a good quick-to-ship approach.
    
           TL T T T T TR
            L C C C C R
            L C C C C R
            L C C C C R
            L C C C C R
           BL B B B B BR
        */
            val cellLength = mEdges.width() / 6
            val leftCenter = mEdges.left + cellLength
            val rightCenter = mEdges.left + 5 * cellLength
            val cellHeight = mEdges.height() / 6
            val topCenter = mEdges.top + cellHeight
            val bottomCenter = mEdges.top + 5 * cellHeight
            return when {
                x < leftCenter -> {
                    when {
                        y < topCenter -> CropWindowMoveHandler.Type.TOP_LEFT
                        y < bottomCenter -> CropWindowMoveHandler.Type.LEFT
                        else -> CropWindowMoveHandler.Type.BOTTOM_LEFT
                    }
                }
                x < rightCenter -> {
                    when {
                        y < topCenter -> CropWindowMoveHandler.Type.TOP
                        y < bottomCenter -> CropWindowMoveHandler.Type.CENTER
                        else -> CropWindowMoveHandler.Type.BOTTOM
                    }
                }
                else -> {
                    when {
                        y < topCenter -> CropWindowMoveHandler.Type.TOP_RIGHT
                        y < bottomCenter -> CropWindowMoveHandler.Type.RIGHT
                        else -> CropWindowMoveHandler.Type.BOTTOM_RIGHT
                    }
                }
            }
        }

        /**
         * Determines if the specified coordinate is in the target touch zone for a corner handle.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param handleX the x-coordinate of the corner handle
         * @param handleY the y-coordinate of the corner handle
         * @param targetRadius the target radius in pixels
         * @return true if the touch point is in the target touch zone; false otherwise
         */
        private fun isInCornerTargetZone(x: Float, y: Float, handleX: Float, handleY: Float, targetRadius: Float): Boolean {
            return abs(x - handleX) <= targetRadius && abs(y - handleY) <= targetRadius
        }

        /**
         * Determines if the specified coordinate is in the target touch zone for a horizontal bar handle.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param handleXStart the left x-coordinate of the horizontal bar handle
         * @param handleXEnd the right x-coordinate of the horizontal bar handle
         * @param handleY the y-coordinate of the horizontal bar handle
         * @param targetRadius the target radius in pixels
         * @return true if the touch point is in the target touch zone; false otherwise
         */
        private fun isInHorizontalTargetZone(
            x: Float,
            y: Float,
            handleXStart: Float,
            handleXEnd: Float,
            handleY: Float,
            targetRadius: Float
        ): Boolean {
            return x > handleXStart && x < handleXEnd && abs(y - handleY) <= targetRadius
        }

        /**
         * Determines if the specified coordinate is in the target touch zone for a vertical bar handle.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param handleX the x-coordinate of the vertical bar handle
         * @param handleYStart the top y-coordinate of the vertical bar handle
         * @param handleYEnd the bottom y-coordinate of the vertical bar handle
         * @param targetRadius the target radius in pixels
         * @return true if the touch point is in the target touch zone; false otherwise
         */
        private fun isInVerticalTargetZone(
            x: Float,
            y: Float,
            handleX: Float,
            handleYStart: Float,
            handleYEnd: Float,
            targetRadius: Float
        ): Boolean {
            return abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd
        }

        /**
         * Determines if the specified coordinate falls anywhere inside the given bounds.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param left the x-coordinate of the left bound
         * @param top the y-coordinate of the top bound
         * @param right the x-coordinate of the right bound
         * @param bottom the y-coordinate of the bottom bound
         * @return true if the touch point is inside the bounding rectangle; false otherwise
         */
        private fun isInCenterTargetZone(x: Float, y: Float, left: Float, top: Float, right: Float, bottom: Float): Boolean {
            return x > left && x < right && y > top && y < bottom
        }

        /**
         * Determines if the cropper should focus on the center handle or the side handles. If it is a
         * small image, focus on the center handle so the user can move it. If it is a large image, focus
         * on the side handles so user can grab them. Corresponds to the appearance of the
         * RuleOfThirdsGuidelines.
         *
         * @return true if it is small enough such that it should focus on the center; less than show_guidelines limit
         */
        private fun focusCenter(): Boolean {
            return !showGuidelines()
        }
    }

    /**
     * @param cropWindowHandler main crop window handle to get and update the crop window edges
     * @param touchX the location of the initial toch possition to measure move distance
     * @param touchY the location of the initial toch possition to measure move distance
     */
    private class CropWindowMoveHandler(type: Type, cropWindowHandler: CropWindowHandler, touchX: Float, touchY: Float) {
        /** Minimum width in pixels that the crop window can get.  */
        private var mMinCropWidth: Float = 0.0f

        /** Minimum width in pixels that the crop window can get.  */
        private var mMinCropHeight: Float = 0.0f

        /** Maximum height in pixels that the crop window can get.  */
        private var mMaxCropWidth: Float = 0.0f

        /** Maximum height in pixels that the crop window can get.  */
        private var mMaxCropHeight: Float = 0.0f

        /** The type of crop window move that is handled.  */
        private var mType: Type = type

        /**
         * Holds the x and y offset between the exact touch location and the exact handle location that is
         * activated. There may be an offset because we allow for some leeway (specified by mHandleRadius)
         * in activating a handle. However, we want to maintain these offset values while the handle is
         * being dragged so that the handle doesn't jump.
         */
        private val mTouchOffset = PointF()

        init {
            mMinCropWidth = cropWindowHandler.getMinCropWidth()
            mMinCropHeight = cropWindowHandler.getMinCropHeight()
            mMaxCropWidth = cropWindowHandler.getMaxCropWidth()
            mMaxCropHeight = cropWindowHandler.getMaxCropHeight()
            calculateTouchOffset(cropWindowHandler.getRect(), touchX, touchY)
        }

        /**
         * Updates the crop window by change in the toch location.<br></br>
         * Move type handled by this instance, as initialized in creation, affects how the change in toch
         * location changes the crop window position and size.<br></br>
         * After the crop window position/size is changed by toch move it may result in values that
         * vialate contraints: outside the bounds of the shown bitmap, smaller/larger than min/max size or
         * missmatch in aspect ratio. So a series of fixes is executed on "secondary" edges to adjust it
         * by the "primary" edge movement.<br></br>
         * Primary is the edge directly affected by move type, secondary is the other edge.<br></br>
         * The crop window is changed by directly setting the Edge coordinates.
         *
         * @param x the new x-coordinate of this handle
         * @param y the new y-coordinate of this handle
         * @param bounds the bounding rectangle of the image
         * @param viewWidth The bounding image view width used to know the crop overlay is at view edges.
         * @param viewHeight The bounding image view height used to know the crop overlay is at view edges.
         * @param snapMargin the maximum distance (in pixels) at which the crop window should snap to the image
         * @param fixedAspectRatio is the aspect ration fixed and 'targetAspectRatio' should be used
         * @param aspectRatio the aspect ratio to maintain
         */
        fun move(
            rect: RectF,
            x: Float,
            y: Float,
            bounds: RectF,
            viewWidth: Int,
            viewHeight: Int,
            snapMargin: Float,
            fixedAspectRatio: Boolean,
            aspectRatio: Float
        ) {
            // Adjust the coordinates for the finger position's offset (i.e. the
            // distance from the initial touch to the precise handle location).
            // We want to maintain the initial touch's distance to the pressed
            // handle so that the crop window size does not "jump".
            val adjX = x + mTouchOffset.x
            val adjY = y + mTouchOffset.y
            if (mType == Type.CENTER) {
                moveCenter(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin)
            } else {
                if (fixedAspectRatio) {
                    moveSizeWithFixedAspectRatio(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin, aspectRatio)
                } else {
                    moveSizeWithFreeAspectRatio(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin)
                }
            }
        }

        /**
         * Calculates the offset of the touch point from the precise location of the specified handle.<br></br>
         * Save these values in a member variable since we want to maintain this offset as we drag the handle.
         */
        private fun calculateTouchOffset(rect: RectF, touchX: Float, touchY: Float) {
            val touchOffsetX: Float
            val touchOffsetY: Float
            // Calculate the offset from the appropriate handle.
            when (mType) {
                Type.TOP_LEFT -> {
                    touchOffsetX = rect.left - touchX
                    touchOffsetY = rect.top - touchY
                }
                Type.TOP_RIGHT -> {
                    touchOffsetX = rect.right - touchX
                    touchOffsetY = rect.top - touchY
                }
                Type.BOTTOM_LEFT -> {
                    touchOffsetX = rect.left - touchX
                    touchOffsetY = rect.bottom - touchY
                }
                Type.BOTTOM_RIGHT -> {
                    touchOffsetX = rect.right - touchX
                    touchOffsetY = rect.bottom - touchY
                }
                Type.LEFT -> {
                    touchOffsetX = rect.left - touchX
                    touchOffsetY = 0f
                }
                Type.TOP -> {
                    touchOffsetX = 0f
                    touchOffsetY = rect.top - touchY
                }
                Type.RIGHT -> {
                    touchOffsetX = rect.right - touchX
                    touchOffsetY = 0f
                }
                Type.BOTTOM -> {
                    touchOffsetX = 0f
                    touchOffsetY = rect.bottom - touchY
                }
                Type.CENTER -> {
                    touchOffsetX = rect.centerX() - touchX
                    touchOffsetY = rect.centerY() - touchY
                }
            }
            mTouchOffset.x = touchOffsetX
            mTouchOffset.y = touchOffsetY
        }

        /** Center move only changes the position of the crop window without changing the size.  */
        private fun moveCenter(
            rect: RectF,
            x: Float,
            y: Float,
            bounds: RectF,
            viewWidth: Int,
            viewHeight: Int,
            snapRadius: Float
        ) {
            var dx = x - rect.centerX()
            var dy = y - rect.centerY()
            if (rect.left + dx < 0 || rect.right + dx > viewWidth || rect.left + dx < bounds.left || rect.right + dx > bounds.right) {
                dx /= 1.05f
                mTouchOffset.x -= dx / 2
            }
            if (rect.top + dy < 0 || rect.bottom + dy > viewHeight || rect.top + dy < bounds.top || rect.bottom + dy > bounds.bottom) {
                dy /= 1.05f
                mTouchOffset.y -= dy / 2
            }
            rect.offset(dx, dy)
            snapEdgesToBounds(rect, bounds, snapRadius)
        }

        /**
         * Change the size of the crop window on the required edge (or edges for corner size move) without
         * affecting "secondary" edges.<br></br>
         * Only the primary edge(s) are fixed to stay within limits.
         */
        private fun moveSizeWithFreeAspectRatio(
            rect: RectF,
            x: Float,
            y: Float,
            bounds: RectF,
            viewWidth: Int,
            viewHeight: Int,
            snapMargin: Float
        ) {
            when (mType) {
                Type.TOP_LEFT -> {
                    adjustTop(rect, y, bounds, snapMargin, 0f, false, false)
                    adjustLeft(rect, x, bounds, snapMargin, 0f, false, false)
                }
                Type.TOP_RIGHT -> {
                    adjustTop(rect, y, bounds, snapMargin, 0f, false, false)
                    adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f, false, false)
                }
                Type.BOTTOM_LEFT -> {
                    adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f, false, false)
                    adjustLeft(rect, x, bounds, snapMargin, 0f, false, false)
                }
                Type.BOTTOM_RIGHT -> {
                    adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f, false, false)
                    adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f, false, false)
                }
                Type.LEFT -> {
                    adjustLeft(rect, x, bounds, snapMargin, 0f, false, false)
                }
                Type.TOP -> {
                    adjustTop(rect, y, bounds, snapMargin, 0f, false, false)
                }
                Type.RIGHT -> {
                    adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f, false, false)
                }
                Type.BOTTOM -> {
                    adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f, false, false)
                }
                else -> {
                }
            }
        }

        /**
         * Change the size of the crop window on the required "primary" edge WITH affect to relevant "secondary" edge via aspect ratio.<br></br>
         * Example: change in the left edge (primary) will affect top and bottom edges (secondary) to preserve the given aspect ratio.
         */
        private fun moveSizeWithFixedAspectRatio(
            rect: RectF,
            x: Float,
            y: Float,
            bounds: RectF,
            viewWidth: Int,
            viewHeight: Int,
            snapMargin: Float,
            aspectRatio: Float
        ) {
            when (mType) {
                Type.TOP_LEFT -> {
                    if (calculateAspectRatio(x, y, rect.right, rect.bottom) < aspectRatio) {
                        adjustTop(rect, y, bounds, snapMargin, aspectRatio, true, false)
                        adjustLeftByAspectRatio(rect, aspectRatio)
                    } else {
                        adjustLeft(rect, x, bounds, snapMargin, aspectRatio, true, false)
                        adjustTopByAspectRatio(rect, aspectRatio)
                    }
                }
                Type.TOP_RIGHT -> {
                    if (calculateAspectRatio(rect.left, y, x, rect.bottom) < aspectRatio) {
                        adjustTop(rect, y, bounds, snapMargin, aspectRatio, false, true)
                        adjustRightByAspectRatio(rect, aspectRatio)
                    } else {
                        adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, true, false)
                        adjustTopByAspectRatio(rect, aspectRatio)
                    }
                }
                Type.BOTTOM_LEFT -> {
                    if (calculateAspectRatio(x, rect.top, rect.right, y) < aspectRatio) {
                        adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, true, false)
                        adjustLeftByAspectRatio(rect, aspectRatio)
                    } else {
                        adjustLeft(rect, x, bounds, snapMargin, aspectRatio, false, true)
                        adjustBottomByAspectRatio(rect, aspectRatio)
                    }
                }
                Type.BOTTOM_RIGHT -> {
                    if (calculateAspectRatio(rect.left, rect.top, x, y) < aspectRatio) {
                        adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, false, true)
                        adjustRightByAspectRatio(rect, aspectRatio)
                    } else {
                        adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, false, true)
                        adjustBottomByAspectRatio(rect, aspectRatio)
                    }
                }
                Type.LEFT -> {
                    adjustLeft(rect, x, bounds, snapMargin, aspectRatio, true, true)
                    adjustTopBottomByAspectRatio(rect, bounds, aspectRatio)
                }
                Type.TOP -> {
                    adjustTop(rect, y, bounds, snapMargin, aspectRatio, true, true)
                    adjustLeftRightByAspectRatio(rect, bounds, aspectRatio)
                }
                Type.RIGHT -> {
                    adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, true, true)
                    adjustTopBottomByAspectRatio(rect, bounds, aspectRatio)
                }
                Type.BOTTOM -> {
                    adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, true, true)
                    adjustLeftRightByAspectRatio(rect, bounds, aspectRatio)
                }
                else -> {
                }
            }
        }

        /** Check if edges have gone out of bounds (including snap margin), and fix if needed.  */
        private fun snapEdgesToBounds(edges: RectF, bounds: RectF, margin: Float) {
            if (edges.left < bounds.left + margin) {
                edges.offset(bounds.left - edges.left, 0f)
            }
            if (edges.top < bounds.top + margin) {
                edges.offset(0f, bounds.top - edges.top)
            }
            if (edges.right > bounds.right - margin) {
                edges.offset(bounds.right - edges.right, 0f)
            }
            if (edges.bottom > bounds.bottom - margin) {
                edges.offset(0f, bounds.bottom - edges.bottom)
            }
        }

        /**
         * Get the resulting x-position of the left edge of the crop window given the handle's position and the image's bounding box and snap radius.
         *
         * @param left the position that the left edge is dragged to
         * @param bounds the bounding box of the image that is being cropped
         * @param snapMargin the snap distance to the image edge (in pixels)
         */
        private fun adjustLeft(
            rect: RectF,
            left: Float,
            bounds: RectF,
            snapMargin: Float,
            aspectRatio: Float,
            topMoves: Boolean,
            bottomMoves: Boolean
        ) {
            var newLeft = left

            if (newLeft < 0) {
                newLeft /= 1.05f
                mTouchOffset.x -= newLeft / 1.1f
            }
            if (newLeft < bounds.left) {
                mTouchOffset.x -= (newLeft - bounds.left) / 2f
            }
            if (newLeft - bounds.left < snapMargin) {
                newLeft = bounds.left
            }
            // Checks if the window is too small horizontally
            if (rect.right - newLeft < mMinCropWidth) {
                newLeft = rect.right - mMinCropWidth
            }
            // Checks if the window is too large horizontally
            if (rect.right - newLeft > mMaxCropWidth) {
                newLeft = rect.right - mMaxCropWidth
            }
            if (newLeft - bounds.left < snapMargin) {
                newLeft = bounds.left
            }
            // check vertical bounds if aspect ratio is in play
            if (aspectRatio > 0) {
                var newHeight = (rect.right - newLeft) / aspectRatio
                // Checks if the window is too small vertically
                if (newHeight < mMinCropHeight) {
                    newLeft = max(bounds.left, rect.right - mMinCropHeight * aspectRatio)
                    newHeight = (rect.right - newLeft) / aspectRatio
                }
                // Checks if the window is too large vertically
                if (newHeight > mMaxCropHeight) {
                    newLeft = max(bounds.left, rect.right - mMaxCropHeight * aspectRatio)
                    newHeight = (rect.right - newLeft) / aspectRatio
                }
                // if top AND bottom edge moves by aspect ratio check that it is within full height bounds
                if (topMoves && bottomMoves) {
                    newLeft = max(
                        newLeft,
                        max(bounds.left, rect.right - bounds.height() * aspectRatio)
                    )
                } else {
                    // if top edge moves by aspect ratio check that it is within bounds
                    if (topMoves && rect.bottom - newHeight < bounds.top) {
                        newLeft = max(
                            bounds.left,
                            rect.right - (rect.bottom - bounds.top) * aspectRatio)
                        newHeight = (rect.right - newLeft) / aspectRatio
                    }
                    // if bottom edge moves by aspect ratio check that it is within bounds
                    if (bottomMoves && rect.top + newHeight > bounds.bottom) {
                        newLeft = max(
                            newLeft,
                            max(bounds.left, rect.right - (bounds.bottom - rect.top) * aspectRatio)
                        )
                    }
                }
            }
            rect.left = newLeft
        }

        /**
         * Get the resulting x-position of the right edge of the crop window given the handle's position and the image's bounding box and snap radius.
         *
         * @param right the position that the right edge is dragged to
         * @param bounds the bounding box of the image that is being cropped
         * @param viewWidth
         * @param snapMargin the snap distance to the image edge (in pixels)
         */
        private fun adjustRight(
            rect: RectF,
            right: Float,
            bounds: RectF,
            viewWidth: Int,
            snapMargin: Float,
            aspectRatio: Float,
            topMoves: Boolean,
            bottomMoves: Boolean
        ) {
            var newRight = right

            if (newRight > viewWidth) {
                newRight = viewWidth + (newRight - viewWidth) / 1.05f
                mTouchOffset.x -= (newRight - viewWidth) / 1.1f
            }
            if (newRight > bounds.right) {
                mTouchOffset.x -= (newRight - bounds.right) / 2f
            }
            // If close to the edge
            if (bounds.right - newRight < snapMargin) {
                newRight = bounds.right
            }
            // Checks if the window is too small horizontally
            if (newRight - rect.left < mMinCropWidth) {
                newRight = rect.left + mMinCropWidth
            }
            // Checks if the window is too large horizontally
            if (newRight - rect.left > mMaxCropWidth) {
                newRight = rect.left + mMaxCropWidth
            }
            // If close to the edge
            if (bounds.right - newRight < snapMargin) {
                newRight = bounds.right
            }
            // check vertical bounds if aspect ratio is in play
            if (aspectRatio > 0) {
                var newHeight = (newRight - rect.left) / aspectRatio
                // Checks if the window is too small vertically
                if (newHeight < mMinCropHeight) {
                    newRight = min(bounds.right, rect.left + mMinCropHeight * aspectRatio)
                    newHeight = (newRight - rect.left) / aspectRatio
                }
                // Checks if the window is too large vertically
                if (newHeight > mMaxCropHeight) {
                    newRight = min(bounds.right, rect.left + mMaxCropHeight * aspectRatio)
                    newHeight = (newRight - rect.left) / aspectRatio
                }
                // if top AND bottom edge moves by aspect ratio check that it is within full height bounds
                if (topMoves && bottomMoves) {
                    newRight = min(
                        newRight,
                        min(bounds.right, rect.left + bounds.height() * aspectRatio)
                    )
                } else {
                    // if top edge moves by aspect ratio check that it is within bounds
                    if (topMoves && rect.bottom - newHeight < bounds.top) {
                        newRight = min(
                            bounds.right,
                            rect.left + (rect.bottom - bounds.top) * aspectRatio)
                        newHeight = (newRight - rect.left) / aspectRatio
                    }
                    // if bottom edge moves by aspect ratio check that it is within bounds
                    if (bottomMoves && rect.top + newHeight > bounds.bottom) {
                        newRight = min(
                            newRight,
                            min(bounds.right, rect.left + (bounds.bottom - rect.top) * aspectRatio)
                        )
                    }
                }
            }
            rect.right = newRight
        }

        /**
         * Get the resulting y-position of the top edge of the crop window given the handle's position and the image's bounding box and snap radius.
         *
         * @param top the x-position that the top edge is dragged to
         * @param bounds the bounding box of the image that is being cropped
         * @param snapMargin the snap distance to the image edge (in pixels)
         */
        private fun adjustTop(
            rect: RectF,
            top: Float,
            bounds: RectF,
            snapMargin: Float,
            aspectRatio: Float,
            leftMoves: Boolean,
            rightMoves: Boolean
        ) {
            var newTop = top

            if (newTop < 0) {
                newTop /= 1.05f
                mTouchOffset.y -= newTop / 1.1f
            }
            if (newTop < bounds.top) {
                mTouchOffset.y -= (newTop - bounds.top) / 2f
            }
            if (newTop - bounds.top < snapMargin) {
                newTop = bounds.top
            }
            // Checks if the window is too small vertically
            if (rect.bottom - newTop < mMinCropHeight) {
                newTop = rect.bottom - mMinCropHeight
            }
            // Checks if the window is too large vertically
            if (rect.bottom - newTop > mMaxCropHeight) {
                newTop = rect.bottom - mMaxCropHeight
            }
            if (newTop - bounds.top < snapMargin) {
                newTop = bounds.top
            }
            // check horizontal bounds if aspect ratio is in play
            if (aspectRatio > 0) {
                var newWidth = (rect.bottom - newTop) * aspectRatio
                // Checks if the crop window is too small horizontally due to aspect ratio adjustment
                if (newWidth < mMinCropWidth) {
                    newTop = max(bounds.top, rect.bottom - mMinCropWidth / aspectRatio)
                    newWidth = (rect.bottom - newTop) * aspectRatio
                }
                // Checks if the crop window is too large horizontally due to aspect ratio adjustment
                if (newWidth > mMaxCropWidth) {
                    newTop = max(bounds.top, rect.bottom - mMaxCropWidth / aspectRatio)
                    newWidth = (rect.bottom - newTop) * aspectRatio
                }
                // if left AND right edge moves by aspect ratio check that it is within full width bounds
                if (leftMoves && rightMoves) {
                    newTop = max(
                        newTop,
                        max(bounds.top, rect.bottom - bounds.width() / aspectRatio)
                    )
                } else {
                    // if left edge moves by aspect ratio check that it is within bounds
                    if (leftMoves && rect.right - newWidth < bounds.left) {
                        newTop = max(
                            bounds.top,
                            rect.bottom - (rect.right - bounds.left) / aspectRatio)
                        newWidth = (rect.bottom - newTop) * aspectRatio
                    }
                    // if right edge moves by aspect ratio check that it is within bounds
                    if (rightMoves && rect.left + newWidth > bounds.right) {
                        newTop = max(
                            newTop,
                            max(bounds.top, rect.bottom - (bounds.right - rect.left) / aspectRatio)
                        )
                    }
                }
            }
            rect.top = newTop
        }

        /**
         * Get the resulting y-position of the bottom edge of the crop window given the handle's position and the image's bounding box and snap radius.
         *
         * @param bottom the position that the bottom edge is dragged to
         * @param bounds the bounding box of the image that is being cropped
         * @param viewHeight
         * @param snapMargin the snap distance to the image edge (in pixels)
         */
        private fun adjustBottom(
            rect: RectF,
            bottom: Float,
            bounds: RectF,
            viewHeight: Int,
            snapMargin: Float,
            aspectRatio: Float,
            leftMoves: Boolean,
            rightMoves: Boolean
        ) {
            var newBottom = bottom

            if (newBottom > viewHeight) {
                newBottom = viewHeight + (newBottom - viewHeight) / 1.05f
                mTouchOffset.y -= (newBottom - viewHeight) / 1.1f
            }
            if (newBottom > bounds.bottom) {
                mTouchOffset.y -= (newBottom - bounds.bottom) / 2f
            }
            if (bounds.bottom - newBottom < snapMargin) {
                newBottom = bounds.bottom
            }
            // Checks if the window is too small vertically
            if (newBottom - rect.top < mMinCropHeight) {
                newBottom = rect.top + mMinCropHeight
            }
            // Checks if the window is too small vertically
            if (newBottom - rect.top > mMaxCropHeight) {
                newBottom = rect.top + mMaxCropHeight
            }
            if (bounds.bottom - newBottom < snapMargin) {
                newBottom = bounds.bottom
            }
            // check horizontal bounds if aspect ratio is in play
            if (aspectRatio > 0) {
                var newWidth = (newBottom - rect.top) * aspectRatio
                // Checks if the window is too small horizontally
                if (newWidth < mMinCropWidth) {
                    newBottom = min(bounds.bottom, rect.top + mMinCropWidth / aspectRatio)
                    newWidth = (newBottom - rect.top) * aspectRatio
                }
                // Checks if the window is too large horizontally
                if (newWidth > mMaxCropWidth) {
                    newBottom = min(bounds.bottom, rect.top + mMaxCropWidth / aspectRatio)
                    newWidth = (newBottom - rect.top) * aspectRatio
                }
                // if left AND right edge moves by aspect ratio check that it is within full width bounds
                if (leftMoves && rightMoves) {
                    newBottom = min(
                        newBottom,
                        min(bounds.bottom, rect.top + bounds.width() / aspectRatio)
                    )
                } else {
                    // if left edge moves by aspect ratio check that it is within bounds
                    if (leftMoves && rect.right - newWidth < bounds.left) {
                        newBottom =
                            min(bounds.bottom, rect.top + (rect.right - bounds.left) / aspectRatio)
                        newWidth = (newBottom - rect.top) * aspectRatio
                    }
                    // if right edge moves by aspect ratio check that it is within bounds
                    if (rightMoves && rect.left + newWidth > bounds.right) {
                        newBottom = min(
                            newBottom,
                            min(bounds.bottom, rect.top + (bounds.right - rect.left) / aspectRatio)
                        )
                    }
                }
            }
            rect.bottom = newBottom
        }

        /** Adjust left edge by current crop window height and the given aspect ratio, the right edge remains in possition while the left adjusts to keep aspect ratio to the height. */
        private fun adjustLeftByAspectRatio(rect: RectF, aspectRatio: Float) {
            rect.left = rect.right - rect.height() * aspectRatio
        }

        /** Adjust top edge by current crop window width and the given aspect ratio, the bottom edge remains in possition while the top adjusts to keep aspect ratio to the width. */
        private fun adjustTopByAspectRatio(rect: RectF, aspectRatio: Float) {
            rect.top = rect.bottom - rect.width() / aspectRatio
        }

        /** Adjust right edge by current crop window height and the given aspect ratio, the left edge remains in possition while the left adjusts to keep aspect ratio to the height. */
        private fun adjustRightByAspectRatio(rect: RectF, aspectRatio: Float) {
            rect.right = rect.left + rect.height() * aspectRatio
        }

        /** Adjust bottom edge by current crop window width and the given aspect ratio, the top edge remains in possition while the top adjusts to keep aspect ratio to the width. */
        private fun adjustBottomByAspectRatio(rect: RectF, aspectRatio: Float) {
            rect.bottom = rect.top + rect.width() / aspectRatio
        }

        /** Adjust left and right edges by current crop window height and the given aspect ratio, both right and left edges adjusts equally relative to center to keep aspect ratio to the height. */
        private fun adjustLeftRightByAspectRatio(rect: RectF, bounds: RectF, aspectRatio: Float) {
            rect.inset((rect.width() - rect.height() * aspectRatio) / 2, 0f)
            if (rect.left < bounds.left) {
                rect.offset(bounds.left - rect.left, 0f)
            }
            if (rect.right > bounds.right) {
                rect.offset(bounds.right - rect.right, 0f)
            }
        }

        /** Adjust top and bottom edges by current crop window width and the given aspect ratio, both top and bottom edges adjusts equally relative to center to keep aspect ratio to the width. */
        private fun adjustTopBottomByAspectRatio(rect: RectF, bounds: RectF, aspectRatio: Float) {
            rect.inset(0f, (rect.height() - rect.width() / aspectRatio) / 2)
            if (rect.top < bounds.top) {
                rect.offset(0f, bounds.top - rect.top)
            }
            if (rect.bottom > bounds.bottom) {
                rect.offset(0f, bounds.bottom - rect.bottom)
            }
        }

        /** Calculates the aspect ratio given a rectangle. */
        private fun calculateAspectRatio(left: Float, top: Float, right: Float, bottom: Float): Float {
            return (right - left) / (bottom - top)
        }

        /** The type of crop window move that is handled. */
        enum class Type {
            TOP_LEFT,
            TOP_RIGHT,
            BOTTOM_LEFT,
            BOTTOM_RIGHT,
            LEFT,
            TOP,
            RIGHT,
            BOTTOM,
            CENTER
        }
    }
}