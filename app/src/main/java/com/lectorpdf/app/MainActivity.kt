package com.lectorpdf.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: HistoryAdapter
    private val historyList = mutableListOf<HistoryItem>()

    private val openPdfLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            openPdf(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.recyclerHistory)
        emptyText = findViewById(R.id.emptyText)
        val fabOpen = findViewById<FloatingActionButton>(R.id.fabOpenPdf)
        adapter = HistoryAdapter(historyList) { item -> openPdf(Uri.parse(item.uriString)) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        fabOpen.setOnClickListener { launchFilePicker() }
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        openPdfLauncher.launch(intent)
    }

    private fun openPdf(uri: Uri) {
        val fileName = getFileName(uri)
        saveToHistory(uri, fileName)
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_URI, uri.toString())
            putExtra(PdfViewerActivity.EXTRA_NAME, fileName)
        }
        startActivity(intent)
    }

    private fun getFileName(uri: Uri): String {
        var name = "Documento PDF"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx)
                }
            }
        } catch (_: Exception) {}
        return name
    }

    private fun saveToHistory(uri: Uri, fileName: String) {
        val prefs = getSharedPreferences("history", MODE_PRIVATE)
        val json = prefs.getString("items", "[]") ?: "[]"
        val array = JSONArray(json)
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val newItem = JSONObject().apply {
            put("uri", uri.toString())
            put("name", fileName)
            put("date", dateStr)
        }
        val cleaned = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.getString("uri") != uri.toString()) cleaned.put(obj)
        }
        val final = JSONArray()
        final.put(newItem)
        for (i in 0 until cleaned.length()) final.put(cleaned.getJSONObject(i))
        val trimmed = JSONArray()
        for (i in 0 until minOf(20, final.length())) trimmed.put(final.getJSONObject(i))
        prefs.edit().putString("items", trimmed.toString()).apply()
    }

    private fun loadHistory() {
        historyList.clear()
        val prefs = getSharedPreferences("history", MODE_PRIVATE)
        val json = prefs.getString("items", "[]") ?: "[]"
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            historyList.add(HistoryItem(
                uriString = obj.getString("uri"),
                fileName = obj.getString("name"),
                date = obj.getString("date")
            ))
        }
        adapter.notifyDataSetChanged()
        emptyText.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (historyList.isEmpty()) View.GONE else View.VISIBLE
    }
}
