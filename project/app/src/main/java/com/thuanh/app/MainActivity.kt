package com.thuanh.app

import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.thuanh.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Đăng ký launcher xử lý kết quả chọn file (docx/pdf/ảnh) từ input file trong WebView
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val results: Array<Uri>? = when {
            data == null -> null
            data.clipData != null -> {
                // Người dùng chọn nhiều file cùng lúc
                val count = data.clipData!!.itemCount
                Array(count) { i -> data.clipData!!.getItemAt(i).uri }
            }
            data.data != null -> arrayOf(data.data!!)
            else -> null
        }
        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView(binding.webView)
        binding.webView.loadUrl("file:///android_asset/docx.html")
    }

    private fun setupWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            // docx.html tự tải JSZip/Tesseract.js/heic-to qua CDN khi cần (loadScriptOnce),
            // nên vẫn cần mạng cho các tính năng OCR/Workers AI dù giao diện chạy offline từ assets.
        }

        webView.webChromeClient = object : WebChromeClient() {
            // Bắt sự kiện <input type="file"> trong docx.html, mở file picker hệ thống Android
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePathCallback = callback
                val intent = fileChooserParams?.createIntent()
                    ?: return false
                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    filePathCallback = null
                    false
                }
            }

            // Cho phép truy cập camera/micro nếu docx.html từng cần (không dùng hiện tại,
            // nhưng để sẵn tránh vỡ tính năng nếu công cụ mở rộng thêm sau này)
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
