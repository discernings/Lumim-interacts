// com.cc.dsniper.ui.SniperFragment
package com.cc.dsniper.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.cc.dsniper.R
import com.cc.dsniper.data.Prefs
import com.cc.dsniper.data.SniperConfig
import com.cc.dsniper.data.SniperState
import com.cc.dsniper.databinding.FragmentSniperBinding
import com.cc.dsniper.vm.SniperViewModel
import kotlinx.coroutines.launch

class SniperFragment : Fragment() {

    private var _b: FragmentSniperBinding? = null
    private val b get() = _b!!
    private val vm: SniperViewModel by viewModels()
    private lateinit var prefs: Prefs
    private val logLines = mutableListOf<String>()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSniperBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        prefs = Prefs(requireContext())

        // Load saved config into UI
        val cfg = prefs.sniperConfig
        b.spinnerLength.setSelection(cfg.digits - 1)  // spinner 1-8
        b.switchNumbers.isChecked  = cfg.onlyNumbers
        b.switchLetters.isChecked  = cfg.onlyLetters
        b.etSpeed.setText(cfg.speedMs.toString())
        b.etJitter.setText(cfg.jitterMs.toString())
        b.etConcurrent.setText(cfg.maxConcurrent.toString())
        b.switchStopFirst.isChecked = cfg.stopOnFirst

        b.btnStart.setOnClickListener {
            saveConfig()
            vm.start()
        }
        b.btnStop.setOnClickListener { vm.stop() }
        b.btnClearLog.setOnClickListener {
            vm.clearLog()
            logLines.clear()
            b.tvLog.text = ""
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                when (state) {
                    is SniperState.Idle    -> setStatus("Pronto", false)
                    is SniperState.Running -> setStatus("Checado: ${state.checked} | Encontrados: ${state.found.size}", true)
                    is SniperState.Found   -> setStatus("🎯 Claimed: ${state.username}", false)
                    is SniperState.Error   -> setStatus("ERRO: ${state.msg}", false)
                    is SniperState.Stopped -> setStatus("Parado", false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.log.collect { entries ->
                if (entries.isEmpty()) return@collect
                val last = entries.last()
                logLines.add(last.msg)
                if (logLines.size > 500) logLines.removeAt(0)
                b.tvLog.text = logLines.takeLast(100).joinToString("\n")
                b.scrollLog.post { b.scrollLog.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun setStatus(msg: String, running: Boolean) {
        b.tvStatus.text = msg
        b.btnStart.isEnabled = !running
        b.btnStop.isEnabled = running
        b.progressBar.visibility = if (running) View.VISIBLE else View.GONE
    }

    private fun saveConfig() {
        val cfg = SniperConfig(
            digits        = b.spinnerLength.selectedItemPosition + 1,
            onlyNumbers   = b.switchNumbers.isChecked,
            onlyLetters   = b.switchLetters.isChecked,
            alphanumeric  = !b.switchNumbers.isChecked && !b.switchLetters.isChecked,
            speedMs       = b.etSpeed.text.toString().toLongOrNull() ?: 500L,
            jitterMs      = b.etJitter.text.toString().toLongOrNull() ?: 200L,
            maxConcurrent = b.etConcurrent.text.toString().toIntOrNull() ?: 3,
            stopOnFirst   = b.switchStopFirst.isChecked
        )
        prefs.sniperConfig = cfg
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
