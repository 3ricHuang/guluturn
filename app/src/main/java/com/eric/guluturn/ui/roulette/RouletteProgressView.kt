package com.eric.guluturn.ui.roulette

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.eric.guluturn.R

/**
 * Circular progress indicator that visually displays the number of roulette spins.
 */
class RouletteProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var spinCount: Int = 0
        set(value) {
            field = value.coerceIn(0, 6)
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 6f
        val size = width.coerceAtMost(height).toFloat()
        val radius = size / 2f - padding
        val centerX = width / 2f
        val centerY = height / 2f

        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        val sweepAngle = 360f / 6
        val colors = listOf(
            ContextCompat.getColor(context, R.color.guluturn_primary),
            ContextCompat.getColor(context, R.color.guluturn_secondary)
        )

        // Draw completed segments
        for (i in 0 until spinCount) {
            paint.color = colors[i % colors.size]
            canvas.drawArc(rect, i * sweepAngle - 90f, sweepAngle, true, paint)
        }

        // Draw segment lines
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.guluturn_background)
            strokeWidth = 4f
        }

        for (i in 0 until 6) {
            val angleRad = Math.toRadians((i * sweepAngle - 30f).toDouble())
            val endX = centerX + radius * Math.cos(angleRad).toFloat()
            val endY = centerY + radius * Math.sin(angleRad).toFloat()
            canvas.drawLine(centerX, centerY, endX, endY, linePaint)
        }

        // Draw outer ring
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(context, R.color.guluturn_brown)
            strokeWidth = 12f
        }
        canvas.drawCircle(centerX, centerY, radius, borderPaint)
    }
}
