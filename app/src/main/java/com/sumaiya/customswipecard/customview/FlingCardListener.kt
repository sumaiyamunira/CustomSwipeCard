package com.sumaiya.customswipecard.customview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator

class FlingCardListener(
    frame: View,
    itemAtPosition: Any?,
    rotation_degrees: Float,
    flingListener: FlingCardListener.FlingListener
) : OnTouchListener {
    private val objectX: Float
    private val objectY: Float
    private val objectH: Int
    private val objectW: Int
    private val parentWidth: Int
    private val parentHeight: Int
    private val mFlingListener: FlingCardListener.FlingListener
    private val dataObject: Any?
    private val halfWidth: Float
    private val halfHeight: Float
    private var BASE_ROTATION_DEGREES: Float
    private var aPosX = 0f
    private var aPosY = 0f
    private var aDownTouchX = 0f
    private var aDownTouchY = 0f

    // The active pointer is the one currently moving our object.
    private var mActivePointerId =
        INVALID_POINTER_ID
    private var frame: View? = null
    private val TOUCH_ABOVE = 0
    private val TOUCH_BELOW = 1
    private var touchPosition = 0
    private val obj = Any()
    private var isAnimationRunning = false
    private val MAX_COS =
        Math.cos(Math.toRadians(45.0)).toFloat()

    constructor(
        frame: View,
        itemAtPosition: Any?,
        flingListener: FlingCardListener.FlingListener
    ) : this(frame, itemAtPosition, 15f, flingListener) {
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {

                // from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Save the ID of this pointer
                mActivePointerId = event.getPointerId(0)
                var x = 0f
                var y = 0f
                var success = false
                try {
                    x = event.getX(mActivePointerId)
                    y = event.getY(mActivePointerId)
                    success = true
                } catch (e: IllegalArgumentException) {
                    Log.w(
                        TAG,
                        "Exception in onTouch(view, event) : $mActivePointerId",
                        e
                    )
                }
                if (success) {
                    // Remember where we started
                    aDownTouchX = x
                    aDownTouchY = y
                    //to prevent an initial jump of the magnifier, aposX and aPosY must
                    //have the values from the magnifier frame
                    if (aPosX == 0f) {
                        aPosX = frame!!.x
                    }
                    if (aPosY == 0f) {
                        aPosY = frame!!.y
                    }
                    touchPosition = if (y < objectH / 2) {
                        TOUCH_ABOVE
                    } else {
                        TOUCH_BELOW
                    }
                }
                view.parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP -> {
                mActivePointerId =
                    INVALID_POINTER_ID
                resetCardViewOnStack()
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // Extract the index of the pointer that left the touch sensor
                val pointerIndex = event.action and
                        MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mActivePointerId = event.getPointerId(newPointerIndex)
                }
            }
            MotionEvent.ACTION_MOVE -> {

                // Find the index of the active pointer and fetch its position
                val pointerIndexMove = event.findPointerIndex(mActivePointerId)
                val xMove = event.getX(pointerIndexMove)
                val yMove = event.getY(pointerIndexMove)

                //from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Calculate the distance moved
                val dx = xMove - aDownTouchX
                val dy = yMove - aDownTouchY


                // Move the frame
                aPosX += dx
                aPosY += dy

                // calculate the rotation degrees
                val distobjectX = aPosX - objectX
                var rotation = BASE_ROTATION_DEGREES * 2f * distobjectX / parentWidth
                if (touchPosition == TOUCH_BELOW) {
                    rotation = -rotation
                }

                //in this area would be code for doing something with the view as the frame moves.
                frame!!.x = aPosX
                frame!!.y = aPosY
                frame!!.rotation = rotation
                mFlingListener.onScroll(scrollProgressPercent)
            }
            MotionEvent.ACTION_CANCEL -> {
                mActivePointerId =
                    INVALID_POINTER_ID
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private val scrollProgressPercent: Float
        private get() = if (movedBeyondLeftBorder()) {
            -1f
        } else if (movedBeyondRightBorder()) {
            1f
        } else {
            val zeroToOneValue =
                (aPosX + halfWidth - leftBorder()) / (rightBorder() - leftBorder())
            zeroToOneValue * 2f - 1f
        }

    private fun resetCardViewOnStack(): Boolean {
        if (movedBeyondLeftBorder()) {
            // Left Swipe
            onSelected(true, getExitPoint(-objectW), 100)
            mFlingListener.onScroll(-1.0f)
        } else if (movedBeyondRightBorder()) {
            // Right Swipe
            onSelected(false, getExitPoint(parentWidth), 100)
            mFlingListener.onScroll(1.0f)
        } else if (movedBeyondBottomBorder()) {
            val abslMoveDistance = Math.abs(aPosY - objectY)
            Log.e("move distance ", "" + abslMoveDistance)
            if (abslMoveDistance > 20.0) {
                onBottomSkip(objectX, 100)
            }
        } else {
            val abslMoveDistance = Math.abs(aPosX - objectX)
            Log.e("move distance ", "" + abslMoveDistance)
            aPosX = 0f
            aPosY = 0f
            aDownTouchX = 0f
            aDownTouchY = 0f
            frame!!.animate()
                .setDuration(200)
                .setInterpolator(OvershootInterpolator(1.5f))
                .x(objectX)
                .y(objectY)
                .rotation(0f)
            mFlingListener.onScroll(0.0f)
            if (abslMoveDistance < 4.0) {
                mFlingListener.onClick(dataObject)
            }
        }
        return false
    }

    private fun movedBeyondLeftBorder(): Boolean {
        return aPosX + halfWidth < leftBorder()
    }

    private fun movedBeyondRightBorder(): Boolean {
        return aPosX + halfWidth > rightBorder()
    }

    private fun movedBeyondBottomBorder(): Boolean {
        return aPosY + halfHeight > bottomBorder()
    }

    fun leftBorder(): Float {
        return parentWidth / 4f
    }

    fun rightBorder(): Float {
        return 3 * parentWidth / 4f
    }

    fun bottomBorder(): Float {
        return 3 * parentHeight / 4f
    }

    fun onSelected(
        isLeft: Boolean,
        exitY: Float, duration: Long
    ) {
        isAnimationRunning = true
        val exitX: Float
        exitX = if (isLeft) {
            -objectW - rotationWidthOffset
        } else {
            parentWidth + rotationWidthOffset
        }
        frame!!.animate()
            .setDuration(duration)
            .setInterpolator(AccelerateInterpolator())
            .x(exitX)
            .y(exitY)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (isLeft) {
                        mFlingListener.onCardExited()
                        mFlingListener.leftExit(dataObject)
                    } else {
                        mFlingListener.onCardExited()
                        mFlingListener.rightExit(dataObject)
                    }
                    isAnimationRunning = false
                }
            })
            .rotation(getExitRotation(isLeft))
    }

    fun onBottomSkip(exitX: Float, duration: Long) {
        isAnimationRunning = true
        var exitY = getExitPointHeight(parentHeight)
        if (exitY < 0) {
            exitY = Math.abs(exitY)
        }
        frame!!.animate()
            .setDuration(duration)
            .setInterpolator(AccelerateInterpolator())
            .x(exitX)
            .y(exitY)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mFlingListener.onCardExited()
                    mFlingListener.bottomExit(dataObject)
                    isAnimationRunning = false
                }
            })
            .rotation(skipRotation)
    }

    /**
     * Starts a default left exit animation.
     */
    fun selectLeft() {
        if (!isAnimationRunning) onSelected(true, objectY, 200)
    }

    /**
     * Starts a default right exit animation.
     */
    fun selectRight() {
        if (!isAnimationRunning) onSelected(false, objectY, 200)
    }

    /**
     * Starts a default bottom exit animation.
     */
    fun selectBottom() {
        if (!isAnimationRunning) onBottomSkip(objectX, 200)
    }

    private fun getExitPoint(exitXPoint: Int): Float {
        val x = FloatArray(2)
        x[0] = objectX
        x[1] = aPosX
        val y = FloatArray(2)
        y[0] = objectY
        y[1] = aPosY
        val regression = LinearRegression(x, y)

        //Your typical y = ax+b linear regression
        return regression.slope().toFloat() * exitXPoint + regression.intercept().toFloat()
    }

    private fun getExitPointHeight(exitYPoint: Int): Float {
        val x = FloatArray(2)
        x[0] = objectX
        x[1] = aPosX
        val y = FloatArray(2)
        y[0] = objectY
        y[1] = aPosY
        val regression = LinearRegression(x, y)

        //Your typical y = ax+b linear regression
        return regression.slope().toFloat() * exitYPoint + regression.intercept().toFloat()
    }

    private fun getExitRotation(isLeft: Boolean): Float {
        var rotation =
            BASE_ROTATION_DEGREES * 2f * (parentWidth - objectX) / parentWidth
        if (touchPosition == TOUCH_BELOW) {
            rotation = -rotation
        }
        if (isLeft) {
            rotation = -rotation
        }
        return rotation
    }

    //        if (isLeft) {
//            rotation = -rotation;
//        }
    private val skipRotation: Float
        private get() {
            var rotation =
                BASE_ROTATION_DEGREES * 2f * (parentHeight + objectY) / parentHeight
            if (touchPosition == TOUCH_BELOW) {
                rotation = -rotation
            }
            //        if (isLeft) {
//            rotation = -rotation;
//        }
            return rotation
        }

    /**
     * When the object rotates it's width becomes bigger.
     * The maximum width is at 45 degrees.
     *
     *
     * The below method calculates the width offset of the rotation.
     */
    private val rotationWidthOffset: Float
        private get() = objectW / MAX_COS - objectW

    fun setRotationDegrees(degrees: Float) {
        BASE_ROTATION_DEGREES = degrees
    }


    fun isTouching(): Boolean {
        return mActivePointerId != INVALID_POINTER_ID
    }

    fun getLastPoint(): PointF? {
        return PointF(aPosX, aPosY)
    }

    interface FlingListener {
        fun onCardExited()
        fun leftExit(dataObject: Any?)
        fun bottomExit(dataObject: Any?)
        fun rightExit(dataObject: Any?)
        fun onClick(dataObject: Any?)
        fun onScroll(scrollProgressPercent: Float)
    }

    companion object {
        private val TAG =
            FlingCardListener::class.java.simpleName
        private const val INVALID_POINTER_ID = -1
    }

    init {
        this.frame = frame
        objectX = frame.x
        objectY = frame.y
        objectH = frame.height
        objectW = frame.width
        halfWidth = objectW / 2f
        halfHeight = objectH / 2f
        dataObject = itemAtPosition
        parentWidth = (frame.parent as ViewGroup).width
        parentHeight = (frame.parent as ViewGroup).height
        BASE_ROTATION_DEGREES = rotation_degrees
        mFlingListener = flingListener
    }
}