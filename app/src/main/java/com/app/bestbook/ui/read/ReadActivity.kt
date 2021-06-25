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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_read)
        if (mViewModel.book?.url == null) {
            showToast(getString(R.string.data_error))
            return
        }
        supportActionBar?.title = mViewModel.subject + " - " + mViewModel.book!!.name

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
            }).renderUrl(mViewModel.book!!.url!!)
        } else {
            mBinding.pdfView.renderFile(File(mViewModel.pdfFile!!))
        }
    }

    override fun onDestroy() {
        mBinding.pdfView.closePdfRender()
        super.onDestroy()
    }
}