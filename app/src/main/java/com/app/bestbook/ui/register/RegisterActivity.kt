package com.app.bestbook.ui.register

import androidx.activity.viewModels
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityRegisterBinding
import com.app.bestbook.ui.home.HomeViewModel

class RegisterActivity: BaseActivity() {
    private lateinit var mBinding: ActivityRegisterBinding
    private val mViewModel: HomeViewModel by viewModels()
}