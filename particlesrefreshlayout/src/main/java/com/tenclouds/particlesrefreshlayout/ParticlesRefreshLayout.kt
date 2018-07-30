package com.tenclouds.particlesrefreshlayout

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v4.widget.ViewDragHelper.INVALID_POINTER
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.tenclouds.particlesrefreshlayout.listener.OnParticleRefreshListener

private const val DRAG_RATE = 0.5f

private const val CIRCLE_DEFAULT_TRANSLATION_Y = 300f
private const val CIRCLE_SIZE = 13

private const val MOVE_HEIGHT_TO_REFRESH_HEIGHT_ANIMATION_DURATION = 300L
private const val MOVE_CIRCLE_TO_TOP_OF_PARTICLES_ANIMATION_DURATION = 300L
private const val HIDE_CONTAINER_ANIMATION_DURATION = 300L
private const val SHOW_CIRCLE_ANIMATION_DURATION = 300L
private const val HIDE_CIRCLE_ANIMATION_DURATION = 300L

class ParticlesRefreshLayout : FrameLayout {

    var onParticleRefreshListener: OnParticleRefreshListener? = null

    private lateinit var particlesAnimationView: ParticlesAnimationView

    private var isRefreshing = false
    private var isReturningToStart = false
    private var isBeginDragged = false

    private var accentColor = ContextCompat.getColor(context, R.color.accentColor)
    private var isSmallSize = false
    private val headerHeight
        get() =
            if (isSmallSize) resources.getDimension(R.dimen.particlesRefreshLayoutHeaderHeightSmall)
            else resources.getDimension(R.dimen.particlesRefreshLayoutHeaderHeight)

    private var childView: View? = null
    private var activePointerId = INVALID_POINTER
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    private var initialMotionY: Float = 0f
    private var initialDownY: Float = 0f

    private var isCircleVisible: Boolean = false

    private val hideContainerAnimation
        get() =
            ValueAnimator.ofFloat(headerHeight, 0f)
                    .apply {
                        duration = HIDE_CONTAINER_ANIMATION_DURATION
                        interpolator = AccelerateDecelerateInterpolator()
                        addUpdateListener {
                            childView?.translationY = it.animatedValue as Float
                            particlesAnimationView.translationY = (it.animatedValue as Float) - headerHeight
                        }
                    }
    private val moveCircleToTopOfParticlesAnimation
        get() = ValueAnimator.ofFloat(
                imageCircle?.translationY ?: 0f,
                headerHeight / 2 - particlesAnimationView.arcSize)
                .apply {
                    duration = MOVE_CIRCLE_TO_TOP_OF_PARTICLES_ANIMATION_DURATION
                    addUpdateListener {
                        imageCircle?.translationY = it.animatedValue as Float
                    }
                }

    private val showCircleAnimation
        get() =
            ValueAnimator.ofFloat(0f, 1f)
                    .apply {
                        duration = SHOW_CIRCLE_ANIMATION_DURATION
                        interpolator = AccelerateDecelerateInterpolator()
                        addUpdateListener {
                            imageCircle?.scaleX = it.animatedValue as Float
                            imageCircle?.scaleY = it.animatedValue as Float
                        }
                        addEndAction { isCircleVisible = true }
                    }

    private val hideCircleAnimation
        get() =
            ValueAnimator.ofFloat(1f, 0f)
                    .apply {
                        duration = HIDE_CIRCLE_ANIMATION_DURATION
                        interpolator = AccelerateDecelerateInterpolator()
                        addUpdateListener {
                            imageCircle?.scaleX = it.animatedValue as Float
                            imageCircle?.scaleY = it.animatedValue as Float
                        }
                        addEndAction { isCircleVisible = false }
                    }

    private var imageCircle: ImageView? = null

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        throwIfViewContainsMoreThanOneChild()
        getAttributesOrDefaultValues(attrs)
        particlesAnimationView =
                ParticlesAnimationView(context, attrs)
                        .apply {
                            headerHeight = this@ParticlesRefreshLayout.headerHeight
                            accentColor = this@ParticlesRefreshLayout.accentColor
                            isSmallSize = this@ParticlesRefreshLayout.isSmallSize
                        }
        post {
            childView = getChildAt(0)
            addParticlesView()
            addCircleView()
        }
    }

    private fun startDragging(yFromTop: Float) {
        val yMoveDiff = yFromTop - initialDownY
        if (yMoveDiff > touchSlop && !isBeginDragged) {
            initialMotionY = initialDownY + touchSlop
            isBeginDragged = true
        }
    }

    private fun updateDragging(yFromTop: Float) {
        if (yFromTop > 0) {
            childView?.translationY = yFromTop
            particlesAnimationView.translationY = -headerHeight + yFromTop
            imageCircle?.translationY = 300f - (yFromTop / 3)

            if (isCircleVisible.not() &&
                    ((imageCircle?.translationY ?: 0f) <= (childView?.translationY ?: 0f))) {
                showCircleAnimation.start()
            }
            if (isCircleVisible &&
                    ((imageCircle?.translationY ?: 0f) > (childView?.translationY ?: 0f))) {
                hideCircleAnimation.start()
            }
        }
    }

    private fun startRefreshing(yFromTop: Float) {
        getMoveHeightToRefreshHeightAnimation(yFromTop)
                .start()
        moveCircleToTopOfParticlesAnimation
                .apply {
                    addEndAction {
                        particlesAnimationView.startRefreshing()
                        onParticleRefreshListener?.onRefresh()
                        isRefreshing = true
                        hideCircleAnimation.start()
                    }
                }
                .start()

    }

    fun stopRefreshing() {
        hideCircleAnimation.start()
        hideContainerAnimation.start()
        particlesAnimationView.stopRefreshing()
        isRefreshing = false
        isBeginDragged = false
        isReturningToStart = false
    }

    private fun addParticlesView() {
        super.addView(particlesAnimationView)
    }

    private fun addCircleView() {
        ImageView(context)
                .apply {
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle))
                    LayoutParams(CIRCLE_SIZE, CIRCLE_SIZE)
                            .apply { gravity = Gravity.CENTER_HORIZONTAL }
                            .let {
                                this.layoutParams = it
                                this.translationY = CIRCLE_DEFAULT_TRANSLATION_Y
                            }
                    scaleX = 0f
                    scaleY = 0f
                }
                .let {
                    this.imageCircle = it
                    super.addView(it)
                }
    }

    private fun getMoveHeightToRefreshHeightAnimation(yFromTop: Float) =
            ValueAnimator.ofFloat(yFromTop, headerHeight)
                    .apply {
                        duration = MOVE_HEIGHT_TO_REFRESH_HEIGHT_ANIMATION_DURATION
                        interpolator = AccelerateDecelerateInterpolator()
                        addUpdateListener {
                            childView?.translationY = it.animatedValue as Float
                            particlesAnimationView.translationY = (it.animatedValue as Float) - headerHeight
                        }
                    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.findPointerIndex(activePointerId)
        val yFromTop = (event.getY(pointerIndex) - initialMotionY) * DRAG_RATE

        if (!isEnabled || canChildScrollUp() || isRefreshing)
            return false

        when (action) {
            MotionEvent.ACTION_UP -> {
                if (isPointerInvalid(pointerIndex)) return false
                if (isBeginDragged) {
                    if (yFromTop >= headerHeight) {
                        startRefreshing(yFromTop)
                    } else {
                        stopRefreshing()
                    }
                    isBeginDragged = false
                }
                activePointerId = INVALID_POINTER
                return false
            }
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                isBeginDragged = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (isPointerInvalid(pointerIndex)) return false
                if (!isBeginDragged) startDragging(y)
                if (isBeginDragged) updateDragging(yFromTop)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (pointerIndex == activePointerId) {
                    val newPointerIndex = if (event.actionIndex == 0) 1 else 0
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (isPointerInvalid(pointerIndex)) return false
                activePointerId = event.getPointerId(pointerIndex)
            }
            MotionEvent.ACTION_CANCEL -> return false
        }

        return true
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.findPointerIndex(activePointerId)

        if (isReturningToStart && action == MotionEvent.ACTION_DOWN)
            isReturningToStart = false

        if (!isEnabled || canChildScrollUp() || isRefreshing)
            return false

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                isBeginDragged = false
                if (isPointerInvalid(pointerIndex)) return false
                initialDownY = event.getY(pointerIndex)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isPointerInvalid(pointerIndex)) return false
                val yFromTop = event.getY(pointerIndex)
                if (!isBeginDragged) startDragging(yFromTop)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (pointerIndex == activePointerId) {
                    val newPointerIndex = if (event.actionIndex == 0) 1 else 0
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBeginDragged = false
                activePointerId = INVALID_POINTER
            }
        }

        return isBeginDragged
    }

    override fun addView(child: View?) {
        throwIfViewContainsMoreThanOneChild()
        childView = child
        super.addView(childView)
    }

    @SuppressLint("CustomViewStyleable")
    private fun getAttributesOrDefaultValues(attrs: AttributeSet?) {
        if (attrs != null) {
            with(context
                    .obtainStyledAttributes(
                            attrs,
                            R.styleable.ParticlesRefreshLayout,
                            0, 0)) {
                accentColor = getColor(
                        R.styleable.ParticlesRefreshLayout_accentColor,
                        ContextCompat.getColor(context, R.color.accentColor))
                isSmallSize = getBoolean(R.styleable.ParticlesRefreshLayout_isSmallSize, false)
                recycle()
            }
        }
    }

    private fun isPointerInvalid(pointerIndex: Int) = pointerIndex <= INVALID_POINTER

    private fun canChildScrollUp() = childView?.canScrollVertically(-1) ?: false

    private fun throwIfViewContainsMoreThanOneChild() {
        if (childCount > 1) {
            throw RuntimeException(context.getString(R.string.exception_more_than_one_child))
        }
    }
}