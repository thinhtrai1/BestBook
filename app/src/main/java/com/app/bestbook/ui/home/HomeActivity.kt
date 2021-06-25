package com.app.bestbook.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityHomeBinding
import com.app.bestbook.model.User
import com.app.bestbook.ui.addBook.AddBookActivity
import com.app.bestbook.ui.register.RegisterActivity
import com.app.bestbook.ui.subject.SubjectActivity
import com.app.bestbook.util.showToast
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class HomeActivity : BaseActivity() {
    private lateinit var mBinding: ActivityHomeBinding
    private val mViewModel: HomeViewModel by viewModels()
    private val mConstraintSet = ConstraintSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_home)

        with(mBinding) {
            viewModel = mViewModel
            toolbar.setTitleTextColor(Color.WHITE)
            imvMenu.setOnClickListener {
                layoutContainer.openDrawer(layoutMenu)
            }
            mConstraintSet.clone(layoutMenu)
            viewLogin.setOnClickListener {
                with(mConstraintSet) {
                    clear(R.id.layoutLogin, ConstraintSet.TOP)
                    connect(R.id.layoutLogin, ConstraintSet.BOTTOM, R.id.layoutMenu, ConstraintSet.BOTTOM)
                    applyTo(layoutMenu)
                    TransitionManager.beginDelayedTransition(layoutMenu, ChangeBounds())
                }
            }
            imvCloseLogin.setOnClickListener {
                with(mConstraintSet) {
                    clear(R.id.layoutLogin, ConstraintSet.BOTTOM)
                    connect(R.id.layoutLogin, ConstraintSet.TOP, R.id.layoutMenu, ConstraintSet.BOTTOM)
                    applyTo(layoutMenu)
                    TransitionManager.beginDelayedTransition(layoutMenu, ChangeBounds())
                }
            }
            btnLogin.setOnClickListener {
                login()
            }
            btnRegister.setOnClickListener {
                startActivity(Intent(this@HomeActivity, RegisterActivity::class.java))
            }
            viewAddBook.setOnClickListener {
                startActivity(Intent(this@HomeActivity, AddBookActivity::class.java))
            }
        }

        mViewModel.classData = {
            startActivity(
                Intent(this, SubjectActivity::class.java)
                    .putExtra("data", it)
            )
        }
    }

    private fun login() {
        showLoading(true)
        val firebaseAuth = Firebase.auth
        firebaseAuth.signInWithEmailAndPassword(
            mBinding.edtUserName.text.toString().trim().plus("@vnbooks.com"),
            mBinding.edtPassword.text.toString().trim()
        ).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                firebaseAuth.currentUser?.let { user ->
                    getInformation(user)
                    return@addOnCompleteListener
                }
            } else {
                showToast(getString(R.string.login_unsuccessfully))
            }
            showLoading(false)
        }.addOnFailureListener {
            showLoading(false)
            showToast(getString(R.string.login_unsuccessfully))
        }
    }

    private fun getInformation(googleUser: FirebaseUser) {
        val firebaseDatabase = Firebase.database("https://bestbook-93f2f-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
        firebaseDatabase.child("user").child(googleUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                showToast(getString(R.string.login_unsuccessfully))
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                showLoading(false)
                snapshot.getValue(User::class.java)?.let {
                    mBinding.layoutProfile.visibility = View.VISIBLE
                    mBinding.tvName.text = it.name
                    mBinding.tvClass.text = getString(R.string.class_format, it.grade)
                    mBinding.tvSdt.text = it.phone
                    it.image?.let { url ->
                        Picasso.get().load(url).into(mBinding.imvAvatar)
                    }
                    mBinding.viewLogin.visibility = View.GONE
                    mBinding.viewLogout.visibility = View.VISIBLE
                    if (it.isAdmin) {
                        mBinding.viewAddBook.visibility = View.VISIBLE
                        mBinding.viewUpdateSubject.visibility = View.VISIBLE
                    }
                }
                mBinding.edtUserName.setText("")
                mBinding.edtPassword.setText("")
                mBinding.imvCloseLogin.performClick()
            }
        })
    }
}