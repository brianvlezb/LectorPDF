package com.lectorpdf.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class HistoryAdapter(
    private val items: MutableList<HistoryItem>,
    private val onClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.tvFileName)
        val date: TextView = view.findViewById(R.id.tvDate)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.fileName.text = item.fileName
        holder.date.text = item.date
        holder.itemView.setOnClickListener { onClick(item) }
        holder.btnDelete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_ID.toInt()) {
                items.removeAt(pos)
                notifyItemRemoved(pos)
                saveHistory(holder.itemView.context)
            }
        }
    }

    override fun getItemCount() = items.size

    private fun saveHistory(context: Context) {
        val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("uri", item.uriString)
            obj.put("name", item.fileName)
            obj.put("date", item.date)
            array.put(obj)
        }
        prefs.edit().putString("items", array.toString()).apply()
    }
}
