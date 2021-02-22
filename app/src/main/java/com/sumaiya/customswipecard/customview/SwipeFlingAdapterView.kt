package com.sumaiya.customswipecard.customview

import android.annotation.TargetApi
import android.content.Context
import android.database.DataSetObserver
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.FrameLayout
import com.sumaiya.customswipecard.R

class SwipeFlingAdapterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R
        .attr.SwipeFlingStyle
) : BaseFlingAdapterView(context, attrs, defStyle) {
    private var MAX_VISIBLE = 4
    private var MIN_ADAPTER_STACK = 6
    private var ROTATION_DEGREES = 15f
    private var mAdapter: Adapter? = null
    private var LAST_OBJECT_IN_STACK = 0
    private var mFlingListener: onFlingListener? = null
    private var mDataSetObserver: AdapterDataSetObserver? = null
    private var mInLayout = false
    private var mActiveCard: View? = null
    private var mOnItemClickListener: OnItemClickListener? =
        null
    private var flingCardListener: FlingCardListener? = null
    private var mLastTouchPoint: PointF? = null

    /**
     * A shortcut method to set both the listeners and the adapter.
     *
     * @param context The activity context which extends onFlingListener, OnItemClickListener or both
     * @param mAdapter The adapter you have to set.
     */
    fun init(context: Context?, mAdapter: Adapter) {
        mFlingListener = if (context is onFlingListener) {
            context
        } else {
            throw RuntimeException("Activity does not implement SwipeFlingAdapterView.onFlingListener")
        }
        if (context is OnItemClickListener) {
            mOnItemClickListener =
                context
        }
        adapter = mAdapter
    }

    override fun getSelectedView(): View {
        return mActiveCard!!
    }

    override fun requestLayout() {
        if (!mInLayout) {
            super.requestLayout()
        }
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        // if we don't have an adapter, we don't need to do anything
        if (mAdapter == null) {
            return
        }
        mInLayout = true
        val adapterCount = mAdapter!!.count
        if (adapterCount == 0) {
            removeAllViewsInLayout()
        } else {
            val topCard = getChildAt(LAST_OBJECT_IN_STACK)
            if (mActiveCard != null && topCard != null && topCard === mActiveCard) {
                if (flingCardListener!!.isTouching()) {
                    val lastPoint: PointF? = flingCardListener!!.getLastPoint()
                    if (mLastTouchPoint == null || mLastTouchPoint != lastPoint) {
                        mLastTouchPoint = lastPoint
                        removeViewsInLayout(0, LAST_OBJECT_IN_STACK)
                        layoutChildren(1, adapterCount)
                    }
                }
            } else {
                // Reset the UI and set top view listener
                removeAllViewsInLayout()
                layoutChildren(0, adapterCount)
                setTopView()
            }
        }
        mInLayout = false
        if (adapterCount <= MIN_ADAPTER_STACK) mFlingListener!!.onAdapterAboutToEmpty(adapterCount)
    }

    private fun layoutChildren(startingIndex: Int, adapterCount: Int) {
        var startingIndex = startingIndex
        while (startingIndex < Math.min(adapterCount, MAX_VISIBLE)) {
            val newUnderChild = mAdapter!!.getView(startingIndex, null, this)
            if (newUnderChild.visibility != View.GONE) {
                makeAndAddView(newUnderChild)
                LAST_OBJECT_IN_STACK = startingIndex
            }
            startingIndex++
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun makeAndAddView(child: View) {
        val lp = child.layoutParams as FrameLayout.LayoutParams
        addViewInLayout(child, 0, lp, true)
        val needToMeasure = child.isLayoutRequested
        if (needToMeasure) {
            val childWidthSpec = ViewGroup.getChildMeasureSpec(
                widthMeasureSpec,
                paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin,
                lp.width
            )
            val childHeightSpec = ViewGroup.getChildMeasureSpec(
                heightMeasureSpec,
                paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin,
                lp.height
            )
            child.measure(childWidthSpec, childHeightSpec)
        } else {
            cleanupLayoutState(child)
        }
        val w = child.measuredWidth
        val h = child.measuredHeight
        var gravity = lp.gravity
        if (gravity == -1) {
            gravity = Gravity.TOP or Gravity.START
        }
        val layoutDirection = layoutDirection
        val absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
        val verticalGravity = gravity and Gravity.VERTICAL_GRAVITY_MASK
        val childLeft: Int
        val childTop: Int
        childLeft = when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.CENTER_HORIZONTAL -> (width + paddingLeft - paddingRight - w) / 2 +
                    lp.leftMargin - lp.rightMargin
            Gravity.END -> width + paddingRight - w - lp.rightMargin
            Gravity.START -> paddingLeft + lp.leftMargin
            else -> paddingLeft + lp.leftMargin
        }
        childTop = when (verticalGravity) {
            Gravity.CENTER_VERTICAL -> (height + paddingTop - paddingBottom - h) / 2 +
                    lp.topMargin - lp.bottomMargin
            Gravity.BOTTOM -> height - paddingBottom - h - lp.bottomMargin
            Gravity.TOP -> paddingTop + lp.topMargin
            else -> paddingTop + lp.topMargin
        }
        child.layout(childLeft, childTop, childLeft + w, childTop + h)
    }

    /**
     * Set the top view and add the fling listener
     */
    private fun setTopView() {
        if (childCount > 0) {
            mActiveCard = getChildAt(LAST_OBJECT_IN_STACK)
            if (mActiveCard != null) {
                flingCardListener = FlingCardListener(
                    mActiveCard!!,
                    mAdapter!!.getItem(0),
                    ROTATION_DEGREES,
                    object :
                        FlingCardListener.FlingListener {
                        override fun onCardExited() {
                            mActiveCard = null
                            mFlingListener!!.removeFirstObjectInAdapter()
                        }

                        override fun leftExit(dataObject: Any?) {
                            mFlingListener!!.onLeftCardExit(dataObject)
                        }

                        override fun bottomExit(dataObject: Any?) {
                            mFlingListener!!.onBottomCardExit(dataObject)
                        }

                        override fun rightExit(dataObject: Any?) {
                            mFlingListener!!.onRightCardExit(dataObject)
                        }

                        override fun onClick(dataObject: Any?) {
                            if (mOnItemClickListener != null) mOnItemClickListener!!.onItemClicked(
                                0,
                                dataObject
                            )
                        }

                        override fun onScroll(scrollProgressPercent: Float) {
                            mFlingListener!!.onScroll(scrollProgressPercent)
                        }
                    })
                mActiveCard!!.setOnTouchListener(flingCardListener)
            }
        }
    }

    @get:Throws(NullPointerException::class)
    val topCardListener: FlingCardListener
        get() {
            if (flingCardListener == null) {
                throw NullPointerException()
            }
            return flingCardListener as FlingCardListener
        }

    fun setMaxVisible(MAX_VISIBLE: Int) {
        this.MAX_VISIBLE = MAX_VISIBLE
    }

    fun setMinStackInAdapter(MIN_ADAPTER_STACK: Int) {
        this.MIN_ADAPTER_STACK = MIN_ADAPTER_STACK
    }

    override fun setAdapter(adapter: Adapter?) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter!!.unregisterDataSetObserver(mDataSetObserver)
            mDataSetObserver = null
        }
        mAdapter = adapter
        if (mAdapter != null && mDataSetObserver == null) {
            mDataSetObserver = AdapterDataSetObserver()
            mAdapter!!.registerDataSetObserver(mDataSetObserver)
        }
    }

    override fun getAdapter(): Adapter {
        return mAdapter!!
    }

    fun setFlingListener(onFlingListener: onFlingListener?) {
        mFlingListener = onFlingListener
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        mOnItemClickListener = onItemClickListener
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return FrameLayout.LayoutParams(context, attrs)
    }

    private inner class AdapterDataSetObserver : DataSetObserver() {
        override fun onChanged() {
            requestLayout()
        }

        override fun onInvalidated() {
            requestLayout()
        }
    }

    interface OnItemClickListener {
        fun onItemClicked(itemPosition: Int, dataObject: Any?)
    }

    interface onFlingListener {
        fun removeFirstObjectInAdapter()
        fun onLeftCardExit(dataObject: Any?)
        fun onBottomCardExit(dataObject: Any?)
        fun onRightCardExit(dataObject: Any?)
        fun onAdapterAboutToEmpty(itemsInAdapter: Int)
        fun onScroll(scrollProgressPercent: Float)
    }

    init {
        val a =
            context.obtainStyledAttributes(attrs, R.styleable.SwipeFlingAdapterView, defStyle, 0)
        MAX_VISIBLE = a.getInt(R.styleable.SwipeFlingAdapterView_max_visible, MAX_VISIBLE)
        MIN_ADAPTER_STACK =
            a.getInt(R.styleable.SwipeFlingAdapterView_min_adapter_stack, MIN_ADAPTER_STACK)
        ROTATION_DEGREES =
            a.getFloat(R.styleable.SwipeFlingAdapterView_rotation_degrees, ROTATION_DEGREES)
        a.recycle()
    }
}