package com.danielschatz.gliderweightbalance.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.danielschatz.gliderweightbalance.R

class CgRangeProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentProgress: Float = 0f // 0.0 to 1.0
    private var rangeStart: Float? = null // 0.0 to 1.0
    private var rangeEnd: Float? = null // 0.0 to 1.0

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

    fun setProgress(progress: Float, start: Float? = null, end: Float? = null) {
        this.currentProgress = progress.coerceIn(0f, 1f)
        this.rangeStart = start?.coerceIn(0f, 1f)
        this.rangeEnd = end?.coerceIn(0f, 1f)
        invalidate()
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
