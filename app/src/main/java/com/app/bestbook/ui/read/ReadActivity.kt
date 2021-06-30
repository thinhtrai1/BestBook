package com.app.bestbook.ui.read

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityReadBinding
import com.app.bestbook.databinding.DialogSelectPageBinding
import com.app.bestbook.databinding.ProgressDialogCustomBinding
import com.app.bestbook.util.showToast
import com.app.bestbook.view.PdfRendererView
import java.io.File

class ReadActivity : BaseActivity() {
    private lateinit var mBinding: ActivityReadBinding
    private val mViewModel: ReadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mViewModel.book?.url == null && mViewModel.pdfFile == null) {
            showToast(getString(R.string.data_error))
            finish()
            return
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_read)
        supportActionBar?.title = mViewModel.subject + " " + mViewModel.grade + " - " + mViewModel.book!!.name

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
                override fun onDownloadProgress(progress: Float) {
                    binding.progressBarDownload.progress = progress
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_select_page -> showSelectPage()
        }
        return true
    }

    private fun showSelectPage() {
        if (mViewModel.dialogSelectPage == null) {
            mViewModel.dialogSelectPage = Dialog(this).apply {
                setContentView(DialogSelectPageBinding.inflate(LayoutInflater.from(this@ReadActivity)).apply {
                    tvTotalPage.text = "/".plus(mBinding.pdfView.getTotalPage())
                    tvFirstPage.setOnClickListener {
                        dismiss()
                        mBinding.pdfView.scrollToPosition(0)
                    }
                    btnOk.setOnClickListener {
                        dismiss()
                        edtPageNumber.text.toString().toIntOrNull()?.let {
                            mBinding.pdfView.scrollToPosition(it + mViewModel.book!!.startPage)
                        }
                        edtPageNumber.setText("")
                    }
                    tvLastPage.setOnClickListener {
                        dismiss()
                        mBinding.pdfView.scrollToPosition(mBinding.pdfView.getTotalPage() - 1)
                    }
                }.root)
                window!!.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
        mViewModel.dialogSelectPage!!.show()
    }

    override fun onDestroy() {
        mBinding.pdfView.closePdfRender()
        super.onDestroy()
    }
}