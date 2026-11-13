// com.cc.dsniper.ui.ProxiesFragment
package com.cc.dsniper.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cc.dsniper.R
import com.cc.dsniper.data.ProxyEntry
import com.cc.dsniper.databinding.FragmentProxiesBinding
import com.cc.dsniper.vm.ProxiesViewModel
import kotlinx.coroutines.launch

class ProxiesFragment : Fragment() {

    private var _b: FragmentProxiesBinding? = null
    private val b get() = _b!!
    private val vm: ProxiesViewModel by viewModels()
    private lateinit var adapter: ProxyAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentProxiesBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        adapter = ProxyAdapter(
            onRemove = { vm.removeProxy(it) },
            onTest   = { vm.testOne(it) }
        )
        b.rvProxies.layoutManager = LinearLayoutManager(requireContext())
        b.rvProxies.adapter = adapter

        b.btnAddProxies.setOnClickListener {
            val raw = b.etProxies.text.toString()
            if (raw.isBlank()) return@setOnClickListener
            vm.addProxiesFromText(raw)
            b.etProxies.setText("")
            Toast.makeText(requireContext(), "Proxies adicionados", Toast.LENGTH_SHORT).show()
        }

        b.btnTestAll.setOnClickListener { vm.testAll() }
        b.btnClearAll.setOnClickListener { vm.clearAll() }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.proxies.collect { list ->
                adapter.submitList(list)
                val ok = list.count { it.working == true }
                val dead = list.count { it.working == false }
                b.tvProxySummary.text = "Total: ${list.size} | ✓ $ok | ✗ $dead"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.testing.collect { testing ->
                b.btnTestAll.isEnabled = !testing
                b.progressProxy.visibility = if (testing) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
