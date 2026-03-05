package com.lectorpdf.app

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PdfViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_NAME = "extra_name"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var pageIndicator: TextView
    private lateinit var touchContainer: ViewGroup
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra(EXTRA_NAME) ?: "PDF"
        recyclerView = findViewById(R.id.recyclerPages)
        pageIndicator = findViewById(R.id.pageIndicator)
        touchContainer = findViewById(R.id.touchContainer)
        scaleGestureDetector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(0.8f, 5f)
                    applyTransform()
                    return true
                }
            })
        val uriString = intent.getStringExtra(EXTRA_URI) ?: run {
            Toast.makeText(this, "No se pudo abrir el archivo", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        openPdf(Uri.parse(uriString))
        setupTouchHandler()
        setupScrollListener()
    }

    private fun openPdf(uri: Uri) {
        try {
            parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: run {
                Toast.makeText(this, "No se pudo leer el archivo", Toast.LENGTH_SHORT).show()
                finish(); return
            }
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            val pageCount = pdfRenderer!!.pageCount
            pageIndicator.text = "1 / $pageCount"
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = PdfPageAdapter(pdfRenderer!!, pageCount)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                val first = lm.findFirstVisibleItemPosition() + 1
                val total = pdfRenderer?.pageCount ?: 0
                if (total > 0) pageIndicator.text = "$first / $total"
            }
        })
    }

    private fun setupTouchHandler() {
        touchContainer.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchX = event.x; lastTouchY = event.y
                        activePointerId = event.getPointerId(0)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (scaleFactor > 1f) {
                            val idx = event.findPointerIndex(activePointerId)
                            if (idx >= 0) {
                                translateX += event.getX(idx) - lastTouchX
                                translateY += event.getY(idx) - lastTouchY
                                lastTouchX = event.getX(idx)
                                lastTouchY = event.getY(idx)
                                clampTranslation(); applyTransform()
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                }
            }
            false
        }
    }

    private fun clampTranslation() {
        val mx = (recyclerView.width * (scaleFactor - 1f)) / 2f
        val my = (recyclerView.height * (scaleFactor - 1f)) / 2f
        translateX = translateX.coerceIn(-mx, mx)
        translateY = translateY.coerceIn(-my, my)
    }

    private fun applyTransform() {
        recyclerView.scaleX = scaleFactor; recyclerView.scaleY = scaleFactor
        recyclerView.translationX = translateX; recyclerView.translationY = translateY
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close(); parcelFileDescriptor?.close()
    }

    inner class PdfPageAdapter(
        private val renderer: PdfRenderer,
        private val pageCount: Int
    ) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

        inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.pageImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PageViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pdf_page, parent, false))

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.imageView.setImageBitmap(null)
            Thread {
                val bmp = renderPage(position)
                runOnUiThread { holder.imageView.setImageBitmap(bmp) }
            }.start()
        }

        override fun getItemCount() = pageCount

        private fun renderPage(index: Int): Bitmap? = try {
            val page = renderer.openPage(index)
            val w = resources.displayMetrics.widthPixels
            val h = (page.height * (w.toFloat() / page.width)).toInt()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close(); bmp
        } catch (_: Exception) { null }
    }
}
