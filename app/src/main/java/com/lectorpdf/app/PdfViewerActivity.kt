package com.lectorpdf.app

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnErrorListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle

class PdfViewerActivity : AppCompatActivity(),
    OnPageChangeListener, OnLoadCompleteListener, OnErrorListener {

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_NAME = "extra_name"
    }

    private lateinit var pdfView: PDFView
    private lateinit var pageIndicator: TextView
    private var totalPages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra(EXTRA_NAME) ?: "PDF"

        pdfView = findViewById(R.id.pdfView)
        pageIndicator = findViewById(R.id.pageIndicator)

        val uriString = intent.getStringExtra(EXTRA_URI) ?: run {
            Toast.makeText(this, "No se pudo abrir el archivo", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        pdfView.fromUri(Uri.parse(uriString))
            .defaultPage(0)
            .onPageChange(this)
            .onLoad(this)
            .onError(this)
            .scrollHandle(DefaultScrollHandle(this))
            .spacing(4)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAntialiasing(true)
            .load()
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        totalPages = pageCount
        pageIndicator.text = "${page + 1} / $pageCount"
    }

    override fun loadComplete(nbPages: Int) {
        totalPages = nbPages
        pageIndicator.text = "1 / $nbPages"
    }

    override fun onError(t: Throwable?) {
        Toast.makeText(this, "Error al abrir el PDF", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
