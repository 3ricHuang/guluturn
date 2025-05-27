package com.eric.guluturn.ui.roulette

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.eric.guluturn.R
import com.eric.guluturn.common.models.Restaurant
import kotlin.math.min
import kotlin.random.Random

/**
 * Custom roulette view that displays a 6-segment spinning wheel.
 * Each segment corresponds to a Restaurant and is drawn with a label.
 */
class RouletteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.guluturn_background)
        textSize = 48f
        textAlign = Paint.Align.CENTER
    }

    private var spinItems: List<SpinItem> = emptyList()
    private var rotationAngle = 0f
    private var isSpinning = false
    private var spinSpeed = 0f

    var onSpinEnd: ((Restaurant) -> Unit)? = null

    fun setItems(restaurants: List<Restaurant>) {
        println("DEBUG: setItems called with ${restaurants.size} items")
        spinItems = restaurants.mapIndexed { index, r ->
            println("DEBUG: spinItem[$index] = ${r.name}")
            SpinItem(id = index + 1, name = r.name, restaurant = r)
        }
        invalidate()
    }

    fun spin() {
        if (!isSpinning) {
            spinSpeed = Random.nextFloat() * 50 + 30
            isSpinning = true
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height)
        val radius = size * 0.375f
        val centerX = width / 2f
        val centerY = height / 2f
        val rectF = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        val sweepAngle = 360f / spinItems.size
        val colors = listOf(
            ContextCompat.getColor(context, R.color.guluturn_primary),
            ContextCompat.getColor(context, R.color.guluturn_secondary)
        )

        canvas.save()
        canvas.rotate(rotationAngle, centerX, centerY)

        for (i in spinItems.indices) {
            paint.color = colors[i % colors.size]
            canvas.drawArc(rectF, i * sweepAngle, sweepAngle, true, paint)

            val angleDeg = i * sweepAngle + sweepAngle / 2
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val x = centerX + radius / 1.5f * Math.cos(angleRad).toFloat()
            val y = centerY + radius / 1.5f * Math.sin(angleRad).toFloat()

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(angleDeg + 90f)
            canvas.drawText(spinItems[i].id.toString(), 0f, 0f, textPaint)
            canvas.restore()
        }

        canvas.restore()

        if (isSpinning) {
            rotationAngle += spinSpeed
            spinSpeed *= 0.98f
            if (spinSpeed < 1f) {
                isSpinning = false
                onSpinEnd?.invoke(getSelectedItem())
            }
            invalidate()
        }

        // Draw outer border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(context, R.color.guluturn_brown)
            strokeWidth = 15f
        }
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        // Draw triangle pointer
        paint.color = ContextCompat.getColor(context, R.color.guluturn_brown)
        val trianglePath = Path().apply {
            moveTo(centerX, centerY - radius + 50f)
            lineTo(centerX - 15f, centerY - radius - 5f)
            lineTo(centerX + 15f, centerY - radius - 5f)
            close()
        }
        canvas.drawPath(trianglePath, paint)
    }

    private fun getSelectedItem(): Restaurant {
        val adjustedAngle = (rotationAngle + 90) % 360
        val index = ((360 - adjustedAngle) / (360f / spinItems.size)).toInt() % spinItems.size
        return spinItems[index].restaurant
    }

    private data class SpinItem(
        val id: Int,
        val name: String,
        val restaurant: Restaurant
    )
}
