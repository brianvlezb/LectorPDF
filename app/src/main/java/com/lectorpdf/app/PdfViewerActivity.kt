package com.lectorpdf.app

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView

class PdfViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_NAME = "extra_name"
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var pageIndicator: TextView
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra(EXTRA_NAME) ?: "PDF"

        viewPager = findViewById(R.id.viewPager)
        pageIndicator = findViewById(R.id.pageIndicator)

        val uriString = intent.getStringExtra(EXTRA_URI) ?: run {
            Toast.makeText(this, "No se pudo abrir el archivo", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        openPdf(Uri.parse(uriString))
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

            viewPager.adapter = PdfPagerAdapter(pdfRenderer!!, pageCount)
            viewPager.offscreenPageLimit = 1

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    pageIndicator.text = "${position + 1} / $pageCount"
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
    }

    inner class PdfPagerAdapter(
        private val renderer: PdfRenderer,
        private val pageCount: Int
    ) : RecyclerView.Adapter<PdfPagerAdapter.PageViewHolder>() {

        inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ZoomableImageView = view.findViewById(R.id.pageImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pdf_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.imageView.setImageBitmap(null)
            holder.imageView.resetZoom()

            // Render at 2x for sharp text
            Thread {
                val bmp = renderPage(position)
                runOnUiThread {
                    holder.imageView.setImageBitmap(bmp)
                }
            }.start()
        }

        override fun getItemCount() = pageCount

        private fun renderPage(index: Int): Bitmap? = try {
            val page = renderer.openPage(index)
            // 2x screen density for crisp text
            val scale = 2
            val w = resources.displayMetrics.widthPixels * scale
            val h = (page.height.toFloat() / page.width.toFloat() * w).toInt()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bmp
        } catch (_: Exception) { null }
    }
}
