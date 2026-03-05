package com.lectorpdf.app

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()

    private var mode = NONE
    private val start = PointF()
    private val mid = PointF()
    private var dist = 1f

    private var minScale = 1f
    private var maxScale = 8f
    private var currentScale = 1f

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
                mode = ZOOM
                savedMatrix.set(matrix)
                mid.set(d.focusX, d.focusY)
                return true
            }
            override fun onScale(d: ScaleGestureDetector): Boolean {
                var scaleFactor = d.scaleFactor
                val newScale = currentScale * scaleFactor
                when {
                    newScale < minScale -> scaleFactor = minScale / currentScale
                    newScale > maxScale -> scaleFactor = maxScale / currentScale
                }
                currentScale *= scaleFactor
                matrix.set(savedMatrix)
                matrix.postScale(scaleFactor, scaleFactor, d.focusX, d.focusY)
                savedMatrix.set(matrix)
                mid.set(d.focusX, d.focusY)
                fixTranslation()
                imageMatrix = matrix
                return true
            }
            override fun onScaleEnd(d: ScaleGestureDetector) {
                mode = NONE
            }
        })

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (currentScale > minScale * 1.5f) {
                    resetZoom()
                } else {
                    val targetScale = minScale * 3f
                    val scaleFactor = targetScale / currentScale
                    currentScale = targetScale
                    matrix.postScale(scaleFactor, scaleFactor, e.x, e.y)
                    fixTranslation()
                    imageMatrix = matrix
                }
                return true
            }
        })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetZoom()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                savedMatrix.set(matrix)
                mode = ZOOM
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG && currentScale > minScale * 1.01f && !scaleDetector.isInProgress) {
                    matrix.set(savedMatrix)
                    matrix.postTranslate(event.x - start.x, event.y - start.y)
                    fixTranslation()
                    imageMatrix = matrix
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                // Snap back if below min scale
                if (currentScale < minScale) resetZoom()
            }
        }
        return true
    }

    private fun fixTranslation() {
        val d = drawable ?: return
        val values = FloatArray(9)
        matrix.getValues(values)
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]

        val imgW = d.intrinsicWidth * scaleX
        val imgH = d.intrinsicHeight * scaleY
        val vW = width.toFloat()
        val vH = height.toFloat()

        var dx = 0f
        var dy = 0f

        if (imgW <= vW) {
            dx = (vW - imgW) / 2f - transX
        } else {
            if (transX > 0) dx = -transX
            else if (transX + imgW < vW) dx = vW - (transX + imgW)
        }

        if (imgH <= vH) {
            dy = (vH - imgH) / 2f - transY
        } else {
            if (transY > 0) dy = -transY
            else if (transY + imgH < vH) dy = vH - (transY + imgH)
        }

        matrix.postTranslate(dx, dy)
    }

    fun resetZoom() {
        val d = drawable ?: run { matrix.reset(); imageMatrix = matrix; return }
        val vW = width.toFloat()
        val vH = height.toFloat()
        val dW = d.intrinsicWidth.toFloat()
        val dH = d.intrinsicHeight.toFloat()
        if (vW == 0f || vH == 0f || dW == 0f || dH == 0f) return

        // Scale to fill full width
        val scale = vW / dW
        minScale = scale
        currentScale = scale

        matrix.reset()
        matrix.postScale(scale, scale)
        // Center vertically if image is shorter than screen
        val scaledH = dH * scale
        if (scaledH < vH) {
            matrix.postTranslate(0f, (vH - scaledH) / 2f)
        }
        imageMatrix = matrix
        savedMatrix.set(matrix)
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}
