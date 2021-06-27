package com.app.bestbook.view.cropImageView

import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.RectF
import android.util.TypedValue
import com.app.bestbook.R

class CropWindowHandler {
    private val mEdges = RectF()

    /**
     * Rectangle used to return the edges rectangle without ability to change it and without creating
     * new all the time.
     */
    private val mGetEdges = RectF()

    /** Minimum width in pixels that the crop window can get.  */
    private var mMinCropWindowWidth: Float = 0.toFloat()

    /** Minimum height in pixels that the crop window can get.  */
    private var mMinCropWindowHeight: Float = 0.toFloat()

    /** Maximum width in pixels that the crop window can CURRENTLY get.  */
    private var mMaxCropWindowWidth: Float = 0.toFloat()

    /** Maximum height in pixels that the crop window can CURRENTLY get.  */
    private var mMaxCropWindowHeight: Float = 0.toFloat()

    /**
     * Minimum width in pixels that the result of cropping an image can get, affects crop window width
     * adjusted by width scale factor.
     */
    private var mMinCropResultWidth: Float = 0.toFloat()

    /**
     * Minimum height in pixels that the result of cropping an image can get, affects crop window
     * height adjusted by height scale factor.
     */
    private var mMinCropResultHeight: Float = 0.toFloat()

    /**
     * Maximum width in pixels that the result of cropping an image can get, affects crop window width
     * adjusted by width scale factor.
     */
    private var mMaxCropResultWidth: Float = 0.toFloat()

    /**
     * Maximum height in pixels that the result of cropping an image can get, affects crop window
     * height adjusted by height scale factor.
     */
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
        return Math.max(mMinCropWindowWidth, mMinCropResultWidth / mScaleFactorWidth)
    }

    /** Minimum height in pixels that the crop window can get.  */
    fun getMinCropHeight(): Float {
        return Math.max(mMinCropWindowHeight, mMinCropResultHeight / mScaleFactorHeight)
    }

    /** Maximum width in pixels that the crop window can get.  */
    fun getMaxCropWidth(): Float {
        return Math.min(mMaxCropWindowWidth, mMaxCropResultWidth / mScaleFactorWidth)
    }

    /** Maximum height in pixels that the crop window can get.  */
    fun getMaxCropHeight(): Float {
        return Math.min(mMaxCropWindowHeight, mMaxCropResultHeight / mScaleFactorHeight)
    }

    /** get the scale factor (on width) of the showen image to original image.  */
    fun getScaleFactorWidth(): Float {
        return mScaleFactorWidth
    }

    /** get the scale factor (on height) of the showen image to original image.  */
    fun getScaleFactorHeight(): Float {
        return mScaleFactorHeight
    }

    /**
     * the min size the resulting cropping image is allowed to be, affects the cropping window limits
     * (in pixels).<br></br>
     */
    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        mMinCropResultWidth = minCropResultWidth.toFloat()
        mMinCropResultHeight = minCropResultHeight.toFloat()
    }

    /**
     * the max size the resulting cropping image is allowed to be, affects the cropping window limits
     * (in pixels).<br></br>
     */
    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        mMaxCropResultWidth = maxCropResultWidth.toFloat()
        mMaxCropResultHeight = maxCropResultHeight.toFloat()
    }

    /**
     * set the max width/height and scale factor of the showen image to original image to scale the
     * limits appropriately.
     */
    fun setCropWindowLimits(
        maxWidth: Float, maxHeight: Float, scaleFactorWidth: Float, scaleFactorHeight: Float
    ) {
        mMaxCropWindowWidth = maxWidth
        mMaxCropWindowHeight = maxHeight
        mScaleFactorWidth = scaleFactorWidth
        mScaleFactorHeight = scaleFactorHeight
    }

    /** Set the variables to be used during crop window handling.  */
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
     * Indicates whether the crop window is small enough that the guidelines should be shown. Public
     * because this function is also used to determine if the center handle should be focused.
     *
     * @return boolean Whether the guidelines should be shown or not
     */
    fun showGuidelines(): Boolean {
        return !(mEdges.width() < 100 || mEdges.height() < 100)
    }

    /**
     * Determines which, if any, of the handles are pressed given the touch coordinates, the bounding
     * box, and the touch radius.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param targetRadius the target radius in pixels
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    fun getMoveHandler(
        x: Float, y: Float, targetRadius: Float, cropShape: CropImageView.CropShape?
    ): CropWindowMoveHandler? {
        val type = if (cropShape === CropImageView.CropShape.OVAL)
            getOvalPressedMoveType(x, y)
        else
            getRectanglePressedMoveType(x, y, targetRadius)
        return if (type != null) CropWindowMoveHandler(type, this, x, y) else null
    }

    // region: Private methods

    /**
     * Determines which, if any, of the handles are pressed given the touch coordinates, the bounding
     * box, and the touch radius.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param targetRadius the target radius in pixels
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    private fun getRectanglePressedMoveType(
        x: Float, y: Float, targetRadius: Float
    ): CropWindowMoveHandler.Type? {
        var moveType: CropWindowMoveHandler.Type? = null

        // Note: corner-handles take precedence, then side-handles, then center.
        if (isInCornerTargetZone(x, y, mEdges.left, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP_LEFT
        } else if (isInCornerTargetZone(
                x, y, mEdges.right, mEdges.top, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.TOP_RIGHT
        } else if (isInCornerTargetZone(
                x, y, mEdges.left, mEdges.bottom, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_LEFT
        } else if (isInCornerTargetZone(
                x, y, mEdges.right, mEdges.bottom, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_RIGHT
        } else if (isInCenterTargetZone(
                x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom
            ) && focusCenter()
        ) {
            moveType = CropWindowMoveHandler.Type.CENTER
        } else if (isInHorizontalTargetZone(
                x, y, mEdges.left, mEdges.right, mEdges.top, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.TOP
        } else if (isInHorizontalTargetZone(
                x, y, mEdges.left, mEdges.right, mEdges.bottom, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.BOTTOM
        } else if (isInVerticalTargetZone(
                x, y, mEdges.left, mEdges.top, mEdges.bottom, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.LEFT
        } else if (isInVerticalTargetZone(
                x, y, mEdges.right, mEdges.top, mEdges.bottom, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.RIGHT
        } else if (isInCenterTargetZone(
                x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom
            ) && !focusCenter()
        ) {
            moveType = CropWindowMoveHandler.Type.CENTER
        }

        return moveType
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

        val moveType: CropWindowMoveHandler.Type
        if (x < leftCenter) {
            if (y < topCenter) {
                moveType = CropWindowMoveHandler.Type.TOP_LEFT
            } else if (y < bottomCenter) {
                moveType = CropWindowMoveHandler.Type.LEFT
            } else {
                moveType = CropWindowMoveHandler.Type.BOTTOM_LEFT
            }
        } else if (x < rightCenter) {
            if (y < topCenter) {
                moveType = CropWindowMoveHandler.Type.TOP
            } else if (y < bottomCenter) {
                moveType = CropWindowMoveHandler.Type.CENTER
            } else {
                moveType = CropWindowMoveHandler.Type.BOTTOM
            }
        } else {
            if (y < topCenter) {
                moveType = CropWindowMoveHandler.Type.TOP_RIGHT
            } else if (y < bottomCenter) {
                moveType = CropWindowMoveHandler.Type.RIGHT
            } else {
                moveType = CropWindowMoveHandler.Type.BOTTOM_RIGHT
            }
        }

        return moveType
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
    private fun isInCornerTargetZone(
        x: Float, y: Float, handleX: Float, handleY: Float, targetRadius: Float
    ): Boolean {
        return Math.abs(x - handleX) <= targetRadius && Math.abs(y - handleY) <= targetRadius
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
        return x > handleXStart && x < handleXEnd && Math.abs(y - handleY) <= targetRadius
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
        return Math.abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd
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
    private fun isInCenterTargetZone(
        x: Float, y: Float, left: Float, top: Float, right: Float, bottom: Float
    ): Boolean {
        return x > left && x < right && y > top && y < bottom
    }

    /**
     * Determines if the cropper should focus on the center handle or the side handles. If it is a
     * small image, focus on the center handle so the user can move it. If it is a large image, focus
     * on the side handles so user can grab them. Corresponds to the appearance of the
     * RuleOfThirdsGuidelines.
     *
     * @return true if it is small enough such that it should focus on the center; less than
     * show_guidelines limit
     */
    private fun focusCenter(): Boolean {
        return !showGuidelines()
    }
}