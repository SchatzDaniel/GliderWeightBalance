package com.danielschatz.gliderweightbalance.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.danielschatz.gliderweightbalance.R

class CgRangeProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentProgress: Float = 0f
    private var rangeStart: Float? = null
    private var rangeEnd: Float? = null

    private var animator: ValueAnimator? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var cornerRadius: Float = 0f

    init {
        // Default colors
        val surfaceVariant = ContextCompat.getColor(context, R.color.md_theme_surfaceVariant)
        trackPaint.color = surfaceVariant
        indicatorPaint.color = ContextCompat.getColor(context, R.color.md_theme_primary)
        dotPaint.color = Color.WHITE
        dotOutlinePaint.color = surfaceVariant
    }

    fun setProgress(progress: Float, start: Float? = null, end: Float? = null, animated: Boolean = true) {
        val targetProgress = progress.coerceIn(0f, 1f)
        val targetStart = start?.coerceIn(0f, 1f)
        val targetEnd = end?.coerceIn(0f, 1f)

        if (!animated) {
            this.currentProgress = targetProgress
            this.rangeStart = targetStart
            this.rangeEnd = targetEnd
            invalidate()
            return
        }

        animator?.cancel()

        val startProgress = this.currentProgress
        val startRangeS = this.rangeStart ?: targetStart ?: 0f
        val startRangeE = this.rangeEnd ?: targetEnd ?: 0f

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                currentProgress = startProgress + (targetProgress - startProgress) * fraction
                
                if (targetStart != null) {
                    rangeStart = startRangeS + (targetStart - startRangeS) * fraction
                }
                if (targetEnd != null) {
                    rangeEnd = startRangeE + (targetEnd - startRangeE) * fraction
                }
                
                invalidate()
            }
            start()
        }
    }

    fun setIndicatorColor(color: Int) {
        indicatorPaint.color = color
        invalidate()
    }
    
    fun setTrackColor(color: Int) {
        trackPaint.color = color
        dotOutlinePaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        
        // The track is centered and thinner than the view height
        val trackHeight = h * 0.4f
        val trackTop = (h - trackHeight) / 2f
        val trackBottom = trackTop + trackHeight
        cornerRadius = trackHeight / 2

        // Draw track
        canvas.drawRoundRect(0f, trackTop, w, trackBottom, cornerRadius, cornerRadius, trackPaint)

        // Draw range if present
        if (rangeStart != null && rangeEnd != null && (rangeEnd!! - rangeStart!!) > 0.001f) {
            val left = rangeStart!! * w
            val right = rangeEnd!! * w
            // Draw range segment with rounded corners (pill shape)
            canvas.drawRoundRect(left, trackTop, right, trackBottom, cornerRadius, cornerRadius, indicatorPaint)
        }

        // Always draw the dot for current CG
        val dotX = (currentProgress * w).coerceIn(h / 2, w - h / 2)
        val dotRadius = h * 0.35f 
        val outlineWidth = 4f

        // Draw dot outline (in track color or specific color)
        dotOutlinePaint.style = Paint.Style.STROKE
        dotOutlinePaint.strokeWidth = outlineWidth
        canvas.drawCircle(dotX, h / 2, dotRadius, dotOutlinePaint)
        
        // Draw dot (white fill)
        dotPaint.style = Paint.Style.FILL
        canvas.drawCircle(dotX, h / 2, dotRadius - (outlineWidth / 2), dotPaint)
    }
}
