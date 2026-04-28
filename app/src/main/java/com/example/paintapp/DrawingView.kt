package com.example.paintapp

import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.BaseSavedState
import android.animation.ValueAnimator
import android.graphics.PorterDuff

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var path = Path()
    private var paint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.RED
    }
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var lastX = 0f
    private var lastY = 0f
    private var isDrawing = false
    private var isRainbow = false
    private var rainbowAnimator: ValueAnimator? = null
    private var backgroundColor = Color.BLACK
    private var stampMode = 0
    private var stampBitmaps = arrayOfNulls<Bitmap>(7)

    init {
        setupRainbowAnimator()
        loadStampBitmaps()
    }

    private fun loadStampBitmaps() {
        val stampIds = listOf(
            com.example.paintapp.R.drawable.stamp1,
            com.example.paintapp.R.drawable.stamp2,
            com.example.paintapp.R.drawable.stamp3,
            com.example.paintapp.R.drawable.stamp4,
            com.example.paintapp.R.drawable.stamp5,
            com.example.paintapp.R.drawable.stamp6,
            com.example.paintapp.R.drawable.stamp7
        )
        stampIds.forEachIndexed { index, resId ->
            stampBitmaps[index] = BitmapFactory.decodeResource(context.resources, resId)
        }
    }

    private fun setupRainbowAnimator() {
        rainbowAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                val hue = it.animatedValue as Float
                paint.color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                if (isDrawing) invalidate()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == oldw && h == oldh) return

        val oldBitmap = bitmap
        val newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val newCanvas = Canvas(newBitmap)
        newCanvas.drawColor(backgroundColor)

        if (oldBitmap != null) {
            val dx = (w - oldBitmap.width) / 2f
            val dy = (h - oldBitmap.height) / 2f
            newCanvas.drawBitmap(oldBitmap, dx, dy, null)
            oldBitmap.recycle()
        }

        bitmap = newBitmap
        canvas = newCanvas
        setBackgroundColor(backgroundColor)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (stampMode > 0) {
                    drawStamp(x, y)
                } else {
                    path.moveTo(x, y)
                    lastX = x
                    lastY = y
                    isDrawing = true
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (stampMode > 0) {
                    drawStamp(x, y)
                } else if (isDrawing) {
                    path.quadTo(lastX, lastY, x, y)
                    lastX = x
                    lastY = y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (stampMode == 0 && isDrawing) {
                    canvas?.drawPath(path, paint)
                    path.reset()
                    isDrawing = false
                }
            }
        }
        return true
    }

    private fun drawStamp(x: Float, y: Float) {
        val stampIndex = stampMode - 1
        if (stampIndex in 0..6) {
            stampBitmaps[stampIndex]?.let { stamp ->
                val size = 64 * 3
                val scaledStamp = Bitmap.createScaledBitmap(stamp, size, size, true)
                val tintedStamp = scaledStamp.copy(Bitmap.Config.ARGB_8888, true)
                val stampCanvas = Canvas(tintedStamp)
                stampCanvas.drawColor(paint.color, PorterDuff.Mode.SRC_IN)
                val left = x - size / 2
                val top = y - size / 2
                canvas?.drawBitmap(tintedStamp, left, top, null)
                invalidate()
            }
        }
    }

    override fun onDraw(screenCanvas: Canvas) {
        super.onDraw(screenCanvas)
        bitmap?.let { bmp ->
            screenCanvas.drawBitmap(bmp, 0f, 0f, null)
        }
        if (!path.isEmpty) {
            screenCanvas.drawPath(path, paint)
        }
    }

    fun clearCanvas() {
        backgroundColor = if (backgroundColor == Color.BLACK) Color.WHITE else Color.BLACK
        canvas?.drawColor(backgroundColor)
        path.reset()
        setBackgroundColor(backgroundColor)
        invalidate()
    }

    fun setColor(color: Int) {
        isRainbow = false
        rainbowAnimator?.cancel()
        paint.color = color
    }

    fun setBrushSize(size: Float) {
        paint.strokeWidth = size
    }

    fun getBrushSize(): Float {
        return paint.strokeWidth
    }

    fun setRainbowEnabled(enabled: Boolean) {
        isRainbow = enabled
        if (enabled) {
            rainbowAnimator?.start()
        } else {
            rainbowAnimator?.cancel()
        }
    }

    fun setStampMode(mode: Int) {
        stampMode = mode
    }

    fun getStampMode(): Int = stampMode

    fun getBitmap(): Bitmap? = bitmap

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rainbowAnimator?.cancel()
        bitmap?.recycle()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        bitmap?.let {
            val stream = java.io.ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.PNG, 100, stream)
            savedState.bitmapBytes = stream.toByteArray()
        }
        savedState.isRainbow = isRainbow
        savedState.backgroundColor = backgroundColor
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            state.bitmapBytes?.let { bytes ->
                bitmap?.recycle()
                val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (decodedBitmap != null) {
                    val w = width
                    val h = height
                    if (w > 0 && h > 0) {
                        val newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val newCanvas = Canvas(newBitmap)
                        newCanvas.drawColor(backgroundColor)
                        val dx = (w - decodedBitmap.width) / 2f
                        val dy = (h - decodedBitmap.height) / 2f
                        newCanvas.drawBitmap(decodedBitmap, dx, dy, null)
                        bitmap = newBitmap
                        canvas = newCanvas
                        decodedBitmap.recycle()
                    } else {
                        bitmap = decodedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        canvas = bitmap?.let { Canvas(it) }
                    }
                    invalidate()
                }
            }
            isRainbow = state.isRainbow
            backgroundColor = state.backgroundColor
            setBackgroundColor(backgroundColor)
            if (isRainbow) {
                rainbowAnimator?.start()
            }
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    internal class SavedState : BaseSavedState {
        var bitmapBytes: ByteArray? = null
        var isRainbow = false
        var backgroundColor = Color.BLACK

        constructor(superState: Parcelable?) : super(superState)

        constructor(source: Parcel) : super(source) {
            bitmapBytes = source.createByteArray()
            isRainbow = source.readInt() == 1
            backgroundColor = source.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeByteArray(bitmapBytes)
            dest.writeInt(if (isRainbow) 1 else 0)
            dest.writeInt(backgroundColor)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }
}
