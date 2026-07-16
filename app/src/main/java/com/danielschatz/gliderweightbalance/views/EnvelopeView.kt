package com.danielschatz.gliderweightbalance.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.danielschatz.gliderweightbalance.R

class EnvelopeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var minCg: Double = 0.0
    private var maxCg: Double = 0.0
    private var maxMass: Double = 0.0
    private var emptyMass: Double = 0.0

    private var simulationPath: List<Pair<Double, Double>> = emptyList() // CG, Mass
    private var currentPoint: Pair<Double, Double>? = null

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.md_theme_outlineVariant)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val limitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.md_theme_onSurface)
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.md_theme_primary)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.md_theme_primary)
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics)
    }

    fun setData(minCg: Double, maxCg: Double, emptyMass: Double, maxMass: Double) {
        this.minCg = minCg
        this.maxCg = maxCg
        this.emptyMass = emptyMass
        this.maxMass = maxMass
        invalidate()
    }

    fun setSimulationPath(path: List<Pair<Double, Double>>, current: Pair<Double, Double>?) {
        this.simulationPath = path
        this.currentPoint = current
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (minCg <= 0.0 || maxCg <= 0.0 || maxMass <= 0.0) return

        val w = width.toFloat()
        val h = height.toFloat()

        // Symmetrische Ränder für bessere Zentrierung
        val marginL = 60f
        val marginR = 60f
        val marginT = 40f
        val marginB = 60f

        val cgRange = maxCg - minCg
        val massRange = maxMass - emptyMass
        
        // Viewport (10% Puffer für Labels)
        val minX = minCg - cgRange * 0.1
        val maxX = maxCg + cgRange * 0.1
        val minY = emptyMass - massRange * 0.1
        val maxY = maxMass + massRange * 0.1

        fun scaleX(cg: Double): Float = (marginL + (cg - minX) / (maxX - minX) * (w - marginL - marginR)).toFloat()
        fun scaleY(mass: Double): Float = (h - marginB - (mass - minY) / (maxY - minY) * (h - marginT - marginB)).toFloat()

        // 1. Grid zeichnen
        gridPaint.alpha = 40
        canvas.drawLine(scaleX(minX), scaleY(emptyMass), scaleX(maxX), scaleY(emptyMass), gridPaint)
        canvas.drawLine(scaleX(minX), scaleY(maxMass), scaleX(maxX), scaleY(maxMass), gridPaint)
        canvas.drawLine(scaleX(minCg), scaleY(minY), scaleX(minCg), scaleY(maxY), gridPaint)
        canvas.drawLine(scaleX(maxCg), scaleY(minY), scaleX(maxCg), scaleY(maxY), gridPaint)

        // 2. Limit-Box zeichnen
        val limitPath = Path()
        limitPath.moveTo(scaleX(minCg), scaleY(emptyMass))
        limitPath.lineTo(scaleX(maxCg), scaleY(emptyMass))
        limitPath.lineTo(scaleX(maxCg), scaleY(maxMass))
        limitPath.lineTo(scaleX(minCg), scaleY(maxMass))
        limitPath.close()
        canvas.drawPath(limitPath, limitPaint)

        // 3. Beschriftungen
        labelPaint.textAlign = Paint.Align.CENTER
        // X-Achse (tiefer gesetzt: +40f statt +25f)
        canvas.drawText("${minCg.toInt()}mm", scaleX(minCg), scaleY(emptyMass) + 40f, labelPaint)
        canvas.drawText("${maxCg.toInt()}mm", scaleX(maxCg), scaleY(emptyMass) + 40f, labelPaint)
        
        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${maxMass.toInt()}kg", scaleX(minCg) - 15f, scaleY(maxMass) + 10f, labelPaint)
        canvas.drawText("${emptyMass.toInt()}kg", scaleX(minCg) - 15f, scaleY(emptyMass) + 10f, labelPaint)

        // 4. Simulationspfad
        if (simulationPath.size > 1) {
            val drawPath = Path()
            simulationPath.forEachIndexed { index, pair ->
                if (index == 0) drawPath.moveTo(scaleX(pair.first), scaleY(pair.second))
                else drawPath.lineTo(scaleX(pair.first), scaleY(pair.second))
            }
            canvas.drawPath(drawPath, pathPaint)
        }

        // 5. Aktueller Punkt
        currentPoint?.let {
            canvas.drawCircle(scaleX(it.first), scaleY(it.second), 12f, dotPaint)
            val corePaint = Paint(dotPaint).apply { color = Color.WHITE }
            canvas.drawCircle(scaleX(it.first), scaleY(it.second), 6f, corePaint)
        }
    }
}
