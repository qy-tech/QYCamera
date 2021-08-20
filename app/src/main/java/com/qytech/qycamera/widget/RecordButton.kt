package com.qytech.qycamera.widget

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.qytech.qycamera.R
import com.qytech.qycamera.utils.toBitmap
import kotlin.math.min

class RecordButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), Animatable {

    companion object {
        private val TAG = RecordButton::class.simpleName
    }

    private val buttonPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = buttonColor
            style = Paint.Style.FILL
        }
    }
    private val progressEmptyPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = progressEmptyColor
            style = Paint.Style.STROKE
            strokeWidth = progressStroke.toFloat()
        }
    }
    private val progressPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = progressColor
            style = Paint.Style.STROKE
            strokeWidth = progressStroke.toFloat()
            strokeCap = Paint.Cap.ROUND
        }
    }
    private val rectF: RectF by lazy {
        RectF()
    }

    private val bitmap: Bitmap? by lazy {
        (recordIcon != -1).let {
            ContextCompat.getDrawable(context, recordIcon)?.toBitmap()
        }
    }

    var buttonRadius: Float = 0f
        private set

    var progressStroke: Int = 0
    var buttonGap: Float = 0f
        private set
    var progressEmptyColor: Int = 0
    var recordIcon: Int = -1
    var progressColor: Int = 0
    var buttonColor: Int = 0

    private val minDuration: Long = 100L
    private var currentDuration: Int = 0
    var maxDuration: Int = 0

    private var isRecording: Boolean = false

    private val startAngle = 270f
    private var sweepAngle: Float = 0f

    private var onRecordListener: RecordListener? = null

    init {
        val resource = context.obtainStyledAttributes(attrs, R.styleable.RecordButton)

        initAttribute(resource)
    }

    private fun initAttribute(resource: TypedArray) {
        buttonRadius = resource.getDimension(
            R.styleable.RecordButton_buttonRadius,
            resources.displayMetrics.scaledDensity * 40f
        )
        progressStroke = resource.getInt(R.styleable.RecordButton_progressStroke, 10)
        buttonGap = resource.getDimension(
            R.styleable.RecordButton_buttonGap,
            resources.displayMetrics.scaledDensity * 8f
        )
        buttonColor = resource.getColor(R.styleable.RecordButton_buttonColor, Color.RED)
        progressEmptyColor =
            resource.getColor(R.styleable.RecordButton_progressEmptyColor, Color.LTGRAY)
        progressColor = resource.getColor(R.styleable.RecordButton_progressColor, Color.BLUE)
        recordIcon = resource.getResourceId(R.styleable.RecordButton_recordIcon, -1)
        maxDuration = resource.getInt(R.styleable.RecordButton_maxMilisecond, 5000)
        resource.recycle()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2.0f
        val cy = width / 2.0f
//        Timber.d("onDraw width = $width height = $width")

        canvas.drawCircle(cx, cy, buttonRadius, buttonPaint)
        canvas.drawCircle(cx, cy, buttonRadius + buttonGap, progressEmptyPaint)

        bitmap?.let {
            canvas.drawBitmap(it, (cx - it.height / 2.0f), (cy - it.width / 2.0f), null)
        }


        sweepAngle = 360 * currentDuration.toFloat() / maxDuration
        rectF.set(
            cx - buttonRadius - buttonGap,
            cy - buttonRadius - buttonGap,
            cx + buttonRadius + buttonGap,
            cy + buttonRadius + buttonGap
        )
        canvas.drawArc(rectF, startAngle, sweepAngle, false, progressPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (buttonRadius * 2 + buttonGap * 2 + progressStroke + 30).toInt()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
//        Timber.d("onMeasure size = $size , widthMode = $widthMode ,widthSize = $widthSize, heightMode = $heightMode , heightSize = $heightSize")
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(size, widthSize)
            else -> size
        }
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(size, heightSize)
            else -> size
        }
//        Timber.d("onMeasure width = $width ,height = $height")
        setMeasuredDimension(width, height)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        performClick()
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                start()
                return true

            }
            MotionEvent.ACTION_UP -> {
                stop()
                return true
            }
        }
        return false
    }

    override fun start() {
        isRecording = true
        scaleAnimation(1.1f, 1.1f)
        postDelayed({
            progressAnimate.start()
        }, minDuration)
    }

    override fun stop() {
        when (currentDuration) {
            in 0..minDuration -> onRecordListener?.onTakePicture()
            in minDuration..maxDuration -> onRecordListener?.onRecordCancel()
            else -> onRecordListener?.onRecordStop()
        }

        isRecording = false
        currentDuration = 0
        scaleAnimation(1f, 1f)
    }

    override fun isRunning() = isRecording

    private fun scaleAnimation(scaleX: Float, scaleY: Float) {
        animate().scaleX(scaleX).scaleY(scaleY).start()
    }

    @SuppressLint("ObjectAnimatorBinding")
    private val progressAnimate =
        ObjectAnimator.ofInt(this, "progress", currentDuration, maxDuration).apply {
            interpolator = LinearInterpolator()

            duration = maxDuration.toLong()

            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                if (isRecording) {
                    setCurrentMilliSecond(value)
                } else {
                    animation.cancel()
                }
                if (value == maxDuration) {
                    stop()
                }
            }

        }

    private fun setCurrentMilliSecond(currentMilliSecond: Int) {
        this.currentDuration = currentMilliSecond
        if (currentDuration > minDuration) {
            onRecordListener?.onRecording(currentMilliSecond)
        }
        postInvalidate()
    }

    fun setOnRecordListener(recordListener: RecordListener) {
        this.onRecordListener = recordListener
    }

    interface RecordListener {
        fun onTakePicture()
        fun onRecording(duration: Int)
        fun onRecordCancel()
        fun onRecordStop()
    }
}