// com.cc.dsniper.ui.TokenFragment
package com.cc.dsniper.ui

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.cc.dsniper.R
import com.cc.dsniper.databinding.FragmentTokenBinding
import com.cc.dsniper.vm.TokenState
import com.cc.dsniper.vm.TokenViewModel
import kotlinx.coroutines.launch

class TokenFragment : Fragment() {

    private var _b: FragmentTokenBinding? = null
    private val b get() = _b!!
    private val vm: TokenViewModel by viewModels()
    private var showToken = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentTokenBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        b.etToken.setText(vm.token.value)
        b.etToken.transformationMethod = PasswordTransformationMethod.getInstance()

        b.btnToggleShow.setOnClickListener {
            showToken = !showToken
            b.etToken.transformationMethod =
                if (showToken) null else PasswordTransformationMethod.getInstance()
            b.etToken.setSelection(b.etToken.text?.length ?: 0)
            b.btnToggleShow.text = if (showToken) "Ocultar" else "Mostrar"
        }

        b.btnSaveToken.setOnClickListener {
            val t = b.etToken.text.toString().trim()
            vm.saveToken(t)
            Toast.makeText(requireContext(), "Token salvo", Toast.LENGTH_SHORT).show()
        }

        b.btnTestToken.setOnClickListener {
            vm.saveToken(b.etToken.text.toString().trim())
            vm.testToken()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                when (state) {
                    is TokenState.Idle    -> {
                        b.tvTokenResult.text = ""
                        b.progressToken.visibility = View.GONE
                    }
                    is TokenState.Testing -> {
                        b.tvTokenResult.text = "Testando..."
                        b.progressToken.visibility = View.VISIBLE
                    }
                    is TokenState.Valid   -> {
                        b.tvTokenResult.text = "✓ Válido — ${state.username}"
                        b.tvTokenResult.setTextColor(0xFF4CAF50.toInt())
                        b.progressToken.visibility = View.GONE
                    }
                    is TokenState.Invalid -> {
                        b.tvTokenResult.text = "✗ Inválido — ${state.reason}"
                        b.tvTokenResult.setTextColor(0xFFF44336.toInt())
                        b.progressToken.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
