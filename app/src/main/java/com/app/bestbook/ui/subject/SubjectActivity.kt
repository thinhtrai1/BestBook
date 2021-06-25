package com.app.bestbook.ui.subject

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivitySubjectBinding
import com.app.bestbook.ui.book.BookActivity
import com.app.bestbook.util.showToast
import java.io.Serializable

class SubjectActivity : BaseActivity() {
    private lateinit var mBinding: ActivitySubjectBinding
    private val mViewModel: SubjectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_subject)
        mBinding.viewModel = mViewModel
        mViewModel.adapter.onClickListener = {
            val books = it.books.map { b -> b.value }
            if (books.isNotEmpty()) {
                startActivity(
                    Intent(this, BookActivity::class.java)
                        .putExtra("data", books as Serializable)
                        .putExtra("grade", mViewModel.grade)
                        .putExtra("subject", it.name)
                )
            } else {
                showToast(getString(R.string.no_data))
            }
        }
    }
}