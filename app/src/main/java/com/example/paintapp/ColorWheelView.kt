package com.example.paintapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var markerPaint: Paint? = null
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var markerX = 0f
    private var markerY = 0f
    private var showMarker = false
    private var colorSelectedListener: ((Int) -> Unit)? = null

    // 24 major colors (HSV hues)
    private val hues = FloatArray(24) { it * 15f }
    private val numSectors = 24

    init {
        setupMarker()
        setBackgroundColor(Color.BLACK)
    }

    private fun setupMarker() {
        markerPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.WHITE
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = Math.min(w, h) / 2f - 20f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (radius <= 0) return
        drawSectors(canvas)
        drawCenterCircle(canvas)
        if (showMarker) {
            markerPaint?.let {
                canvas.drawCircle(markerX, markerY, 14f, it)
                canvas.drawCircle(markerX, markerY, 8f, Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    color = Color.BLACK
                })
            }
        }
    }

    private fun drawSectors(canvas: Canvas) {
        val angleStep = 360f / numSectors
        val outerR = radius
        val innerR = radius * 0.18f
        for (i in 0 until numSectors) {
            val hue = hues[i]
            val startAngle = i * angleStep - 90f
            drawSector(canvas, hue, startAngle, angleStep, outerR, innerR)
        }
    }

    private fun drawSector(canvas: Canvas, hue: Float, startAngle: Float, sweep: Float, outerR: Float, innerR: Float) {
        val path = Path()
        // Start at center
        path.moveTo(centerX, centerY)
        // Arc outer bounds
        val oval = RectF(centerX - outerR, centerY - outerR, centerX + outerR, centerY + outerR)
        // First corner on outer arc
        val rad0 = Math.toRadians(startAngle.toDouble()).toFloat()
        val x0 = centerX + outerR * Math.cos(rad0.toDouble()).toFloat()
        val y0 = centerY + outerR * Math.sin(rad0.toDouble()).toFloat()
        path.lineTo(x0, y0)
        path.arcTo(oval, startAngle, sweep)
        path.lineTo(centerX, centerY)
        path.close()

        Paint().apply {
            isAntiAlias = true
            color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            style = Paint.Style.FILL
            canvas.drawPath(path, this)
        }
        Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#22000000")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            canvas.drawPath(path, this)
        }
    }

    private fun drawCenterCircle(canvas: Canvas) {
        val r = radius * 0.18f
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
            canvas.drawCircle(centerX, centerY, r, this)
        }
        Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#44000000")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            canvas.drawCircle(centerX, centerY, r, this)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val dx = x - centerX
        val dy = y - centerY
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist <= radius && dist > radius * 0.05f) {
            // 0 deg CW from top (0° = top)
            var a = (Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())) + 90) % 360
            if (a < 0) a += 360
            val step = 360f / numSectors
            val idx = (a / step).toInt() % numSectors
            val hue = hues[idx]

            val ra = Math.toRadians((a - 90 + 360) % 360)
            markerX = centerX + (dist * Math.cos(ra)).toFloat()
            markerY = centerY + (dist * Math.sin(ra)).toFloat()
            showMarker = true
            invalidate()

            colorSelectedListener?.invoke(Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        }
        return true
    }

    fun setOnColorSelectedListener(listener: (Int) -> Unit) {
        colorSelectedListener = listener
    }
}
