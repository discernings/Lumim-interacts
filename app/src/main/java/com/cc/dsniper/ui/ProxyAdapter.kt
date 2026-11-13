// com.cc.dsniper.ui.ProxyAdapter
package com.cc.dsniper.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cc.dsniper.data.ProxyEntry
import com.cc.dsniper.databinding.ItemProxyBinding

class ProxyAdapter(
    private val onRemove: (ProxyEntry) -> Unit,
    private val onTest: (ProxyEntry) -> Unit
) : ListAdapter<ProxyEntry, ProxyAdapter.VH>(DIFF) {

    inner class VH(val b: ItemProxyBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
        VH(ItemProxyBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)
        h.b.tvProxyAddr.text = "${item.host}:${item.port}" +
            if (item.user.isNotEmpty()) " (auth)" else ""

        val latText = when (item.latencyMs) {
            -1L  -> "—"
            -2L  -> "DEAD"
            else -> "${item.latencyMs}ms"
        }
        val color = when (item.working) {
            true  -> Color.parseColor("#4CAF50")
            false -> Color.parseColor("#F44336")
            null  -> Color.GRAY
        }
        h.b.tvProxyStatus.text = latText
        h.b.tvProxyStatus.setTextColor(color)
        h.b.btnRemoveProxy.setOnClickListener { onRemove(item) }
        h.b.btnTestProxy.setOnClickListener { onTest(item) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ProxyEntry>() {
            override fun areItemsTheSame(a: ProxyEntry, b: ProxyEntry) =
                a.host == b.host && a.port == b.port
            override fun areContentsTheSame(a: ProxyEntry, b: ProxyEntry) = a == b
        }
    }
}
