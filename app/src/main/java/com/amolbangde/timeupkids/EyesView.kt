package com.amolbangde.timeupkids

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Do aankhein jo palak jhapakti hain aur idhar-udhar dekhti hain. urgent = true => badi aur tez. */
class EyesView(context: Context) : View(context) {

    var urgent = false

    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111111") }
    private var t = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2400
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = resources.displayMetrics.density
        setMeasuredDimension((56 * d).toInt(), (30 * d).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val r = (if (urgent) 13f else 11f) * d
        val cy = height / 2f

        val speed = if (urgent) 4 else 1
        val look = sin(t * 2f * PI.toFloat() * speed)

        val phase = (t * (if (urgent) 3 else 1)) % 1f
        val lid = (if (phase > 0.92f) abs(phase - 0.96f) / 0.04f else 1f).coerceIn(0.08f, 1f)

        for (cx in floatArrayOf(width * 0.25f, width * 0.75f)) {
            canvas.save()
            canvas.scale(1f, lid, cx, cy)
            canvas.drawCircle(cx, cy, r, white)
            canvas.drawCircle(cx + look * r * 0.35f, cy, r * 0.45f, dark)
            canvas.restore()
        }
    }
}
