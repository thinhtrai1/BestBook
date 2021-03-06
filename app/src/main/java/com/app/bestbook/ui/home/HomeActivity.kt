package com.app.bestbook.ui.home

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityHomeBinding
import com.app.bestbook.databinding.DialogAboutBinding
import com.app.bestbook.model.Subject
import com.app.bestbook.model.User
import com.app.bestbook.ui.addBook.AddBookActivity
import com.app.bestbook.ui.read.ReadActivity
import com.app.bestbook.ui.register.RegisterActivity
import com.app.bestbook.ui.subject.SubjectActivity
import com.app.bestbook.ui.updateSubject.UpdateSubjectActivity
import com.app.bestbook.util.Constant
import com.app.bestbook.util.email
import com.app.bestbook.util.showToast
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import java.io.Serializable

class HomeActivity : BaseActivity() {
    private lateinit var mBinding: ActivityHomeBinding
    private val mViewModel: HomeViewModel by viewModels()
    private val mConstraintSet = ConstraintSet()

    private fun checkSharedStreamUri(intent: Intent?) {
        intent?.getParcelableExtra<Uri>("uri")?.let {
            if (Firebase.auth.currentUser?.email != Constant.ADMIN.email()) {
                startActivity(
                    Intent(this, ReadActivity::class.java).putExtra("uri", it)
                )
            } else {
                startActivity(
                    Intent(this, AddBookActivity::class.java).putExtra("uri", it)
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkSharedStreamUri(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkSharedStreamUri(intent)
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
                login(edtUserName.text.toString().trim().email(), edtPassword.text.toString().trim())
            }
            btnRegister.setOnClickListener {
                startActivity(Intent(this@HomeActivity, RegisterActivity::class.java))
            }
            viewAddBook.setOnClickListener {
                startActivity(Intent(this@HomeActivity, AddBookActivity::class.java))
            }
            viewUpdateSubject.setOnClickListener {
                startActivity(Intent(this@HomeActivity, UpdateSubjectActivity::class.java))
            }
            viewUpdateProfile.setOnClickListener {
                startActivity(
                    Intent(this@HomeActivity, RegisterActivity::class.java).putExtra("user", mViewModel.user)
                )
            }
            viewLogout.setOnClickListener {
                AlertDialog.Builder(this@HomeActivity)
                    .setMessage(R.string.logout_confirm_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        Firebase.auth.signOut()
                        mViewModel.user = null
                        mViewModel.sharedPreferencesHelper.getSharedPreferences().edit().clear().apply()
                        recreate()
                    }.show()
            }
            viewAbout.setOnClickListener {
                val binding = DialogAboutBinding.inflate(LayoutInflater.from(this@HomeActivity))
                Dialog(this@HomeActivity).apply {
                    setContentView(binding.root)
                    binding.btnOk.setOnClickListener {
                        dismiss()
                    }
                    window?.attributes?.width = RecyclerView.LayoutParams.MATCH_PARENT
                    show()
                }
            }
        }

        mViewModel.sharedPreferencesHelper.let {
            if (!it[Constant.PREF_EMAIL].isNullOrBlank() && !it[Constant.PREF_PASSWORD].isNullOrBlank()) {
                login(it[Constant.PREF_EMAIL]!!, it[Constant.PREF_PASSWORD]!!)
            } else {
                login(Constant.ANONYMOUS.email(), Constant.ANONYMOUS_PASS)
            }
        }

        mViewModel.classData = { grade ->
            showLoading(true)
            mViewModel.firebaseDatabase.child("data").child(grade.toString()).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    showToast(getString(R.string.an_error_occurred_please_try_again))
                    showLoading(false)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChildren()) {
                        val data = ArrayList<Subject>()
                        snapshot.children.onEach {
                            it.key?.let { key ->
                                it.getValue(Subject::class.java)?.apply { id = key }?.let { s -> data.add(s) }
                            }
                        }
                        startActivity(
                            Intent(this@HomeActivity, SubjectActivity::class.java)
                                .putExtra("data", data as Serializable)
                                .putExtra("grade", grade)
                                .putExtra("isAdmin", mViewModel.user?.isAdmin)
                        )
                    } else {
                        showToast(getString(R.string.no_data))
                    }
                    showLoading(false)
                }
            })
        }
    }

    private fun login(email: String, password: String) {
        showLoading(true)
        val firebaseAuth = Firebase.auth
        firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    firebaseAuth.currentUser?.let { user ->
                        if (email != Constant.ANONYMOUS.email()) {
                            getInformation(user, email, password)
                            return@addOnCompleteListener
                        }
                    }
                } else {
                    showToast(getString(R.string.login_unsuccessfully))
                }
                showLoading(false)
            }
            .addOnFailureListener {
                showLoading(false)
                showToast(getString(R.string.login_unsuccessfully))
            }
    }

    private fun getInformation(googleUser: FirebaseUser, email: String, password: String) {
        mViewModel.firebaseDatabase.child("user").child(googleUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                showToast(getString(R.string.login_unsuccessfully))
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                showLoading(false)
                with(mBinding) {
                    snapshot.getValue(User::class.java)?.let {
                        mViewModel.user = it.apply { id = googleUser.uid }
                        if (it.isAdmin) {
                            viewAddBook.visibility = View.VISIBLE
                            viewUpdateSubject.visibility = View.VISIBLE
                        }
                        layoutProfile.visibility = View.VISIBLE
                        tvName.text = it.name
                        tvClass.text = getString(R.string.class_format, it.grade)
                        tvSdt.text = it.phone
                        it.image?.let { url ->
                            Picasso.get().load(url).resizeDimen(R.dimen.avatar_size, R.dimen.avatar_size).centerCrop().into(imvAvatar)
                        }
                        viewUpdateProfile.visibility = View.VISIBLE
                        viewLogout.visibility = View.VISIBLE
                        viewLogin.visibility = View.GONE
                        mViewModel.sharedPreferencesHelper[Constant.PREF_EMAIL] = email
                        mViewModel.sharedPreferencesHelper[Constant.PREF_PASSWORD] = password
                    }
                    imvCloseLogin.performClick()
                    edtUserName.setText("")
                    edtPassword.setText("")
                }
            }
        })
    }

    private var isDoubleClickToFinish = false
    override fun onBackPressed() {
        if (mBinding.layoutContainer.isDrawerOpen(mBinding.layoutMenu)) {
            mBinding.layoutContainer.closeDrawer(mBinding.layoutMenu)
        } else {
            if (isDoubleClickToFinish) {
                super.onBackPressed()
            } else {
                showToast(getString(R.string.tap_again_to_exit))
                Handler(Looper.getMainLooper()).postDelayed({
                    isDoubleClickToFinish = false
                }, 2000)
                isDoubleClickToFinish = true
            }
        }
    }
}