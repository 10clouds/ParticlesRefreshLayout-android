package com.tenclouds.particlesrefreshlayout

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.animation.PathInterpolatorCompat
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.plattysoft.leonids.ParticleSystem
import kotlin.math.cos
import kotlin.math.sin

private const val PARTICLES_MIN_SPEED = 0.01f
private const val PARTICLES_MAX_SPEED = 0.02f
private const val PARTICLES_MIN_ACCELERATION = 0.05f
private const val PARTICLES_MAX_ACCELERATION = 0.05f
private const val PARTICLES_MIN_ANGLE = 0
private const val PARTICLES_MAX_ANGLE = 10
private const val PARTICLES_MIN_ROTATION_SPEED = 0.000003f
private const val PARTICLES_MIN_ROTATION = 0f
private const val PARTICLES_MAX_ROTATION = 360f
private const val PARTICLES_PER_SECOND = 300
private const val PARTICLES_EMITTING_TIME = 1350
private const val PARTICLES_ACCELERATION = 0.000023f
private const val PARTICLES_MAX = 1000
private const val PARTICLES_TIME_TO_LIVE = 400L
private const val PARTICLES_MS_BEFORE_END_FADE_OUT = 800L

private const val PARTICLES_SMALL_MIN_SCALE = 0.3f
private const val PARTICLES_SMALL_MAX_SCALE = 0.4f
private const val PARTICLES_MIN_SCALE = 0.6f
private const val PARTICLES_MAX_SCALE = 1.1f

private const val SWEEP_START_ANIMATION_DURATION = 500L
private const val SWEEP_END_ANIMATION_DURATION = 700L
private const val ANGLE_ANIMATION_DURATION = 1500L

private const val ARC_ANGLE_START = 0f
private const val ARC_SWEEP_ANGLE_START = 0f
private const val ARC_RECT_SIZE_SCALE = 0.96
private const val ARC_STROKE_WIDTH = 8f

internal class ParticlesAnimationView : FrameLayout {

    internal var accentColor = ContextCompat.getColor(context, R.color.accentColor)
    internal var isSmallSize = false
    internal var headerHeight = 0f
        set(value) {
            field = value
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, value.toInt())
                    .apply { gravity = Gravity.TOP }
                    .let { this.layoutParams = it }
            translationY = -value
        }

    private var centerYPoint: Float? = null
    private var centerXPoint: Float? = null

    internal val arcSize
        get() = headerHeight * 1 / 4
    private val arcRectSize
        get() = arcSize * ARC_RECT_SIZE_SCALE
    private val arcRect = RectF(0f, 0f, 0f, 0f)
    private val arcPaint: Paint? =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = ARC_STROKE_WIDTH
                strokeCap = Paint.Cap.BUTT
            }

    private var arcAngle = ARC_ANGLE_START
    private var arcSweepAngle = ARC_SWEEP_ANGLE_START

    private val particlesMinScale
        get() = if (isSmallSize) PARTICLES_SMALL_MIN_SCALE else PARTICLES_SMALL_MAX_SCALE
    private val particlesMaxScale
        get() = if (isSmallSize) PARTICLES_MIN_SCALE else PARTICLES_MAX_SCALE

    private var particleSystem: ParticleSystem? = null
    private val particleCircleDrawable =
            ContextCompat.getDrawable(context, R.drawable.circle)
                    ?.apply { DrawableCompat.setTint(this, accentColor) }

    private var isRefreshing = false
    private val isViewSizeValueInvalid
        get() = (centerYPoint == null) || (centerXPoint == null)

    private val angleInterpolator =
            PathInterpolatorCompat.create(0.7f, 0f, 0f, 1f)

    private val angleAnimator =
            ValueAnimator.ofFloat(-90f, 270f)
                    .apply {
                        duration = ANGLE_ANIMATION_DURATION
                        interpolator = angleInterpolator
                        addStartAction { animateParticles() }
                        addEndAction { if (isRefreshing) animateArch() }
                        addUpdateListener { animation ->
                            arcAngle = animation.animatedValue as Float
                            invalidate()
                        }
                    }

    private val sweepStartAnimation =
            ValueAnimator.ofFloat(0f, 60f)
                    .apply {
                        duration = SWEEP_START_ANIMATION_DURATION
                        interpolator = AccelerateInterpolator()
                        addStartAction { sweepStopAnimation.start() }
                        addUpdateListener { arcSweepAngle = it.animatedValue as Float }
                    }

    private val sweepStopAnimation =
            ValueAnimator.ofFloat(60f, 0f)
                    .apply {
                        duration = SWEEP_END_ANIMATION_DURATION
                        interpolator = DecelerateInterpolator()
                        addUpdateListener { arcSweepAngle = it.animatedValue as Float }
                    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            background = ColorDrawable(Color.TRANSPARENT)
        } else {
            @Suppress("DEPRECATION")
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

    }

    fun startRefreshing() {
        isRefreshing = true
        updateLocationInWindow()
        animateArch()
    }

    fun stopRefreshing() {
        isRefreshing = false
        particleSystem?.stopEmitting()
        particleSystem?.setAcceleration(PARTICLES_ACCELERATION, arcAngle.toInt())
    }

    private fun animateArch() {
        sweepStartAnimation.start()
        angleAnimator.start()
    }

    private fun animateParticles() {
        particleSystem =
                ParticleSystem(this@ParticlesAnimationView,
                        PARTICLES_MAX, particleCircleDrawable, PARTICLES_TIME_TO_LIVE)
                        .apply {
                            setScaleRange(particlesMinScale, particlesMaxScale)
                            setSpeedRange(PARTICLES_MIN_SPEED, PARTICLES_MAX_SPEED)
                            setAccelerationModuleAndAndAngleRange(PARTICLES_MIN_ACCELERATION, PARTICLES_MAX_ACCELERATION, PARTICLES_MIN_ANGLE, PARTICLES_MAX_ANGLE)
                            setRotationSpeedRange(PARTICLES_MIN_ROTATION, PARTICLES_MAX_ROTATION)
                            setRotationSpeed(PARTICLES_MIN_ROTATION_SPEED)
                            setFadeOut(PARTICLES_MS_BEFORE_END_FADE_OUT, AccelerateInterpolator())
                        }
        particleSystem?.emit(-100, -100, PARTICLES_PER_SECOND, PARTICLES_EMITTING_TIME)
    }

    private fun updateLocationInWindow() =
            IntArray(2)
                    .also { getLocationInWindow(it) }
                    .let {
                        centerYPoint = if (it.isNotEmpty()) it[1].toFloat() else null
                        centerXPoint = (width / 2).toFloat()
                    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isViewSizeValueInvalid) return
        if (isRefreshing) {
            updateParticlesEmitPoint()
            drawArc(canvas)
        }
    }

    private fun drawArc(canvas: Canvas) {
        if (isViewSizeValueInvalid) return
        arcRect.set(
                centerXPoint!! - arcSize,
                centerYPoint!! - arcSize,
                centerXPoint!! + arcSize,
                centerYPoint!! + arcSize)
        if (arcAngle > 0.1f)
            canvas.drawArc(arcRect, arcAngle, arcSweepAngle, false, arcPaint)
    }

    private fun updateParticlesEmitPoint() {
        if (isViewSizeValueInvalid) return

        val xFromCircleCenter = arcRectSize * cos((arcAngle + 5).toRadians())
        val x = (centerXPoint!! + xFromCircleCenter).toInt()
        val yFromCircleCenter = arcRectSize * sin((arcAngle + 5).toRadians())
        val y = (centerYPoint!! + (headerHeight / 2) + yFromCircleCenter).toInt()

        particleSystem?.updateEmitPoint(x, y)
        particleSystem?.setAcceleration(PARTICLES_ACCELERATION, arcAngle.toInt())
    }
}

internal fun Animator.addStartAction(action: () -> Unit) =
        this.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) = action.invoke()
            override fun onAnimationEnd(animation: Animator?) = Unit
            override fun onAnimationRepeat(animation: Animator?) = Unit
            override fun onAnimationCancel(animation: Animator?) = Unit
        })

internal fun Animator.addEndAction(action: () -> Unit) =
        this.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) = Unit
            override fun onAnimationEnd(animation: Animator?) = action.invoke()
            override fun onAnimationRepeat(animation: Animator?) = Unit
            override fun onAnimationCancel(animation: Animator?) = Unit
        })

internal fun Float.toRadians() = Math.toRadians((this + 5).toDouble())