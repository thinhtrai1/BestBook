package com.app.bestbook.ui.book

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityBookBinding
import com.app.bestbook.ui.addBook.AddBookActivity
import com.app.bestbook.ui.read.ReadActivity

class BookActivity : BaseActivity() {
    private lateinit var mBinding: ActivityBookBinding
    private val mViewModel: BookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_book)
        mBinding.viewModel = mViewModel
        mViewModel.adapter.onClickListener = { book, isRead ->
            if (isRead) {
                startActivity(
                    Intent(this, ReadActivity::class.java)
                        .putExtra("data", book)
                        .putExtra("grade", mViewModel.grade)
                        .putExtra("subject", mViewModel.subject)
                )
            } else {
                startActivity(
                    Intent(this, AddBookActivity::class.java)
                        .putExtra("data", book)
                        .putExtra("grade", mViewModel.grade)
                        .putExtra("subject", mViewModel.subject)
                )
            }
        }
    }
}