package com.app.bestbook.view.cropImageView

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.app.bestbook.R
import java.util.*

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
     * Informs the CropOverlayView of the image's position relative to the ImageView. This is
     * necessary to call in order to draw the crop window.
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
                System.arraycopy(boundsPoints!!, 0, mBoundsPoints, 0, boundsPoints!!.size)
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
                if (mOriginalLayerType != View.LAYER_TYPE_SOFTWARE) {
                    // TURN off hardware acceleration
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
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

    /**
     * Sets the guidelines for the CropOverlayView to be either on, off, or to show when resizing the
     * application.
     */
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
            throw IllegalArgumentException(
                "Cannot set aspect ratio value to a number less than or equal to 0."
            )
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
     * the crop window edge is less than or equal to this distance (in pixels) away from the bounding
     * box edge. (default: 3)
     */
    fun setSnapRadius(snapRadius: Float) {
        mSnapRadius = snapRadius
    }

    /** Set multi touch functionality to enabled/disabled.  */
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

    /**
     * the min size the resulting cropping image is allowed to be, affects the cropping window limits
     * (in pixels).<br></br>
     */
    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        mCropWindowHandler.setMinCropResultSize(minCropResultWidth, minCropResultHeight)
    }

    /**
     * the max size the resulting cropping image is allowed to be, affects the cropping window limits
     * (in pixels).<br></br>
     */
    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        mCropWindowHandler.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight)
    }

    /**
     * set the max width/height and scale factor of the shown image to original image to scale the
     * limits appropriately.
     */
    fun setCropWindowLimits(maxWidth: Float, maxHeight: Float, scaleFactorWidth: Float, scaleFactorHeight: Float) {
        mCropWindowHandler.setCropWindowLimits(
            maxWidth, maxHeight, scaleFactorWidth, scaleFactorHeight
        )
    }

    /** Set crop window initial rectangle to be used instead of default.  */
    fun setInitialCropWindowRect(rect: Rect?) {
        mInitialCropWindowRect.set(rect ?: BitmapUtils.EMPTY_RECT)
        if (initializedCropWindow) {
            initCropWindow()
            invalidate()
            callOnCropWindowChanged(false)
        }
    }

    /** Reset crop window to initial rectangle.  */
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

        setCropShape(CropImageView.CropShape.values()[ta.getInt(R.styleable.CropImageView_cropShape, CropImageView.CropShape.RECTANGLE.ordinal)])
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
            R.styleable.CropImageView_cropMultiTouchEnabled, false
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
                R.styleable.CropImageView_cropBorderCornerColor, Color.WHITE
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

    /**
     * Set the initial crop window size and position. This is dependent on the size and position of
     * the image being cropped.
     */
    private fun initCropWindow() {
        val leftLimit = Math.max(BitmapUtils.getRectLeft(mBoundsPoints), 0F)
        val topLimit = Math.max(BitmapUtils.getRectTop(mBoundsPoints), 0F)
        val rightLimit = Math.min(BitmapUtils.getRectRight(mBoundsPoints), width.toFloat())
        val bottomLimit = Math.min(BitmapUtils.getRectBottom(mBoundsPoints), height.toFloat())

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
            rect.left = Math.max(leftLimit, rect.left)
            rect.top = Math.max(topLimit, rect.top)
            rect.right = Math.min(rightLimit, rect.right)
            rect.bottom = Math.min(bottomLimit, rect.bottom)
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
                val cropWidth = Math.max(mCropWindowHandler.getMinCropWidth(), rect.height() * mTargetAspectRatio)
                val halfCropWidth = cropWidth / 2f
                rect.left = centerX - halfCropWidth
                rect.right = centerX + halfCropWidth
            } else {
                rect.left = leftLimit + horizontalPadding
                rect.right = rightLimit - horizontalPadding
                val centerY = height / 2f
                // Limits the aspect ratio to no less than 40 wide or 40 tall
                val cropHeight = Math.max(mCropWindowHandler.getMinCropHeight(), rect.width() / mTargetAspectRatio)
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

    /** Fix the given rect to fit into bitmap rect and follow min, max and aspect ratio rules.  */
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
            val leftLimit = Math.max(mCalcBounds.left, 0f)
            val topLimit = Math.max(mCalcBounds.top, 0f)
            val rightLimit = Math.min(mCalcBounds.right, width.toFloat())
            val bottomLimit = Math.min(mCalcBounds.bottom, height.toFloat())
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
        if (mFixAspectRatio && Math.abs(rect.width() - rect.height() * mTargetAspectRatio) > 0.1) {
            if (rect.width() > rect.height() * mTargetAspectRatio) {
                val adj = Math.abs(rect.height() * mTargetAspectRatio - rect.width()) / 2
                rect.left += adj
                rect.right -= adj
            } else {
                val adj = Math.abs(rect.width() / mTargetAspectRatio - rect.height()) / 2
                rect.top += adj
                rect.bottom -= adj
            }
        }
    }

    /**
     * Draw crop overview by drawing background over image not in the cripping area, then borders and
     * guidelines.
     */
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
        val left = Math.max(BitmapUtils.getRectLeft(mBoundsPoints), 0F)
        val top = Math.max(BitmapUtils.getRectTop(mBoundsPoints), 0F)
        val right = Math.min(BitmapUtils.getRectRight(mBoundsPoints), width.toFloat())
        val bottom = Math.min(BitmapUtils.getRectBottom(mBoundsPoints), height.toFloat())

        if (mCropShape === CropImageView.CropShape.RECTANGLE) {
            if (!isNonStraightAngleRotated() || Build.VERSION.SDK_INT <= 17) {
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
                canvas.clipPath(mPath, Region.Op.INTERSECT)
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

    /**
     * Draw 2 veritcal and 2 horizontal guidelines inside the cropping area to split it into 9 equal
     * parts.
     */
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
                    (h * Math.sin(Math.acos(((w - oneThirdCropWidth) / w).toDouble()))).toFloat()
                canvas.drawLine(x1, rect.top + h - yv, x1, rect.bottom - h + yv, mGuidelinePaint!!)
                canvas.drawLine(x2, rect.top + h - yv, x2, rect.bottom - h + yv, mGuidelinePaint!!)

                // Draw horizontal guidelines.
                val y1 = rect.top + oneThirdCropHeight
                val y2 = rect.bottom - oneThirdCropHeight
                val xv =
                    (w * Math.cos(Math.asin(((h - oneThirdCropHeight) / h).toDouble()))).toFloat()
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

    /** Draw borders of the crop area.  */
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

    /** Draw the corner of crop overlay.  */
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

    /** Clear move handler starting in [.onActionDown] if exists.  */
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
     * Calculate the bounding rectangle for current crop window, handle non-straight rotation angles.
     * <br></br>
     * If the rotation angle is straight then the bounds rectangle is the bitmap rectangle, otherwsie
     * we find the max rectangle that is within the image bounds starting from the crop window
     * rectangle.
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

            left = Math.max(
                left,
                if ((d0 - b0) / (a0 - c0) < rect.right) (d0 - b0) / (a0 - c0) else left
            )
            left = Math.max(
                left,
                if ((d0 - b1) / (a1 - c0) < rect.right) (d0 - b1) / (a1 - c0) else left
            )
            left = Math.max(
                left,
                if ((d1 - b3) / (a1 - c1) < rect.right) (d1 - b3) / (a1 - c1) else left
            )
            right = Math.min(
                right,
                if ((d1 - b1) / (a1 - c1) > rect.left) (d1 - b1) / (a1 - c1) else right
            )
            right = Math.min(
                right,
                if ((d1 - b2) / (a0 - c1) > rect.left) (d1 - b2) / (a0 - c1) else right
            )
            right = Math.min(
                right,
                if ((d0 - b2) / (a0 - c0) > rect.left) (d0 - b2) / (a0 - c0) else right
            )

            top = Math.max(top, Math.max(a0 * left + b0, a1 * right + b1))
            bottom = Math.min(bottom, Math.min(a1 * left + b3, a0 * right + b2))

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
}