package com.angcyo.wasm_android_demo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.javascriptengine.JavaScriptIsolate
import androidx.javascriptengine.JavaScriptSandbox
import com.angcyo.wasm_android_demo.databinding.FragmentFirstBinding
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            //findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)

            if (JavaScriptSandbox.isSupported()) {
                thread {
                    val jsSandboxFuture: ListenableFuture<JavaScriptSandbox> =
                        JavaScriptSandbox.createConnectedInstanceAsync(requireContext())
                    val jsSandbox = jsSandboxFuture.get()
                    val jsIsolate: JavaScriptIsolate = jsSandbox.createIsolate()

                    val code =
                        "function sum(a, b) { let r = a + b; return r.toString(); }; sum(3, 4)"
                    var resultFuture = jsIsolate.evaluateJavaScriptAsync(code)
                    var result = resultFuture[5, TimeUnit.SECONDS]

                    val wasm = resources.assets.open("image_bg.wasm").readBytes()

                    val jsCode = """
                        android.consumeNamedDataAsArrayBuffer('wasm-1').then((value) => { 
                            return WebAssembly.compile(value).then((module) => {
                                return new WebAssembly.Instance(module)
                                            .exports
                                            .gcode2acii('angcyo').toString(); 
                            })
                        })
                    """.trimIndent()
                    val success: Boolean = jsIsolate.provideNamedData("wasm-1", wasm)
                    if (success) {
                        resultFuture = jsIsolate.evaluateJavaScriptAsync(jsCode)
                        result = resultFuture[5, TimeUnit.SECONDS]
                    } else {
                        // the data chunk name has been used before, use a different name
                    }

                    jsSandbox.close()

                    view.post {
                        binding.textviewFirst.text = result
                    }
                }
            } else {
                Log.e("angcyo", "not support!")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}