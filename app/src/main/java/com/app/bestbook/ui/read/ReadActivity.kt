package com.app.bestbook.ui.read

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityReadBinding
import com.app.bestbook.databinding.ProgressDialogCustomBinding
import com.app.bestbook.util.showToast
import com.app.bestbook.view.PdfRendererView
import java.io.File

class ReadActivity : BaseActivity() {
    private lateinit var mBinding: ActivityReadBinding
    private val mViewModel: ReadViewModel by viewModels()
    private val test = "https://firebasestorage.googleapis.com/v0/b/bestbook-93f2f.appspot.com/o/book%2F1%2Ftiengviet%2FSach%20Giao%20Khoa%20Tieng%20Viet%20lop%201%20Tap%201.pdf?alt=media"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_read)

        if (mViewModel.pdfFile == null) {
            val binding: ProgressDialogCustomBinding
            val progressDialog = Dialog(this).apply {
                binding = ProgressDialogCustomBinding.inflate(LayoutInflater.from(this@ReadActivity))
                setContentView(binding.root)
                setCancelable(false)
                window!!.setBackgroundDrawableResource(android.R.color.transparent)
                show()
            }
            mBinding.pdfView.setStatusListener(object : PdfRendererView.StatusListener {
                override fun onDownloadProgress(progress: Int) {
                    binding.progressBarDownload.progress = progress.toFloat()
                }

                override fun onDisplay() {
                    progressDialog.dismiss()
                    mViewModel.pdfFile = mBinding.pdfView.getFilePath()
                }

                override fun onError(error: Throwable) {
                    progressDialog.dismiss()
                    showToast(error.message)
                }
            }).renderUrl(test)
        } else {
            mBinding.pdfView.renderFile(File(mViewModel.pdfFile!!))
        }
    }

    override fun onDestroy() {
        mBinding.pdfView.closePdfRender()
        super.onDestroy()
    }
}