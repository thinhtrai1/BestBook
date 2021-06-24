package com.app.bestbook.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityHomeBinding
import com.app.bestbook.ui.subject.SubjectActivity

class HomeActivity : BaseActivity() {
    private lateinit var mBinding: ActivityHomeBinding
    private val mViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        mBinding.viewModel = mViewModel
        mViewModel.classData = {
            startActivity(Intent(this, SubjectActivity::class.java)
                .putExtra("data", it))
        }
    }
}