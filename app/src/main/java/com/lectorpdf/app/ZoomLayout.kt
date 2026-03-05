package com.lectorpdf.app

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout

class ZoomLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false
    private var lastPointerCount = 0

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val prev = scaleFactor
                scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(1f, 5f)
                // Zoom hacia el punto central del pellizco
                val focusX = detector.focusX
                val focusY = detector.focusY
                translateX = focusX - (focusX - translateX) * (scaleFactor / prev)
                translateY = focusY - (focusY - translateY) * (scaleFactor / prev)
                clamp()
                applyTransform()
                return true
            }
        })

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Interceptar cuando hay 2+ dedos para hacer zoom
        if (ev.pointerCount >= 2) return true
        // Interceptar arrastre solo si hay zoom activo
        if (scaleFactor > 1f && ev.actionMasked == MotionEvent.ACTION_MOVE) return true
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                lastX = ev.rawX
                lastY = ev.rawY
                lastPointerCount = ev.pointerCount
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && scaleFactor > 1f && ev.pointerCount == 1) {
                    translateX += ev.rawX - lastX
                    translateY += ev.rawY - lastY
                    clamp()
                    applyTransform()
                    isDragging = true
                }
                lastX = ev.rawX
                lastY = ev.rawY
                lastPointerCount = ev.pointerCount
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Al soltar zoom vuelve a 1 si queda muy cerca de 1
                if (scaleFactor < 1.05f) {
                    scaleFactor = 1f
                    translateX = 0f
                    translateY = 0f
                    applyTransform()
                }
                isDragging = false
            }
        }
        return true
    }

    private fun clamp() {
        val child = if (childCount > 0) getChildAt(0) else return
        val maxX = ((child.width * scaleFactor) - width).coerceAtLeast(0f) / 2f
        val maxY = ((child.height * scaleFactor) - height).coerceAtLeast(0f) / 2f
        translateX = translateX.coerceIn(-maxX, maxX)
        translateY = translateY.coerceIn(-maxY, maxY)
    }

    private fun applyTransform() {
        val child = if (childCount > 0) getChildAt(0) else return
        child.scaleX = scaleFactor
        child.scaleY = scaleFactor
        child.translationX = translateX
        child.translationY = translateY
    }

    fun resetZoom() {
        scaleFactor = 1f; translateX = 0f; translateY = 0f
        applyTransform()
    }
}
