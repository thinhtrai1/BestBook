package com.app.bestbook.ui.register

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityRegisterBinding
import com.app.bestbook.model.User
import com.app.bestbook.ui.home.HomeActivity
import com.app.bestbook.util.*
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import java.io.File

class RegisterActivity: BaseActivity() {
    private lateinit var mBinding: ActivityRegisterBinding
    private val mViewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_register)

        mViewModel.user?.let {
            with(mBinding) {
                it.image?.let { url ->
                    Picasso.get().load(url).resizeDimen(R.dimen.avatar_size, R.dimen.avatar_size).centerCrop().into(imvAvatar)
                }
                edtUserName.isEnabled = false
                edtUserName.setText(mViewModel.sharedPreferencesHelper[Constant.PREF_EMAIL]?.emailToUsername())
                edtName.setText(it.name)
                edtGrade.setText(it.grade)
                edtPhone.setText(it.phone)
                btnLogin.text = getString(R.string.update)
            }
        }

        mBinding.btnLogin.setOnClickListener {
            if (isValid()) {
                showLoading(true)
                if (mViewModel.user != null) { // updateProfile
                    if (mViewModel.imageUri != null) {
                        uploadImage(mViewModel.user!!.id!!) {
                            addUser(
                                mViewModel.sharedPreferencesHelper[Constant.PREF_EMAIL]!!,
                                mViewModel.sharedPreferencesHelper[Constant.PREF_PASSWORD]!!,
                                mViewModel.user!!.id!!,
                                it
                            )
                        }
                    } else {
                        addUser(
                            mViewModel.sharedPreferencesHelper[Constant.PREF_EMAIL]!!,
                            mViewModel.sharedPreferencesHelper[Constant.PREF_PASSWORD]!!,
                            mViewModel.user!!.id!!,
                            mViewModel.user!!.image
                        )
                    }
                } else { // register
                    val email = mBinding.edtUserName.text.toString().trim().email()
                    val password = mBinding.edtPassword.text.toString().trim()
                    Firebase.auth
                        .createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val userId = result.user?.uid ?: return@addOnSuccessListener
                            if (mViewModel.imageUri != null) {
                                uploadImage(userId) {
                                    addUser(email, password, userId, it)
                                }
                            } else {
                                addUser(email, password, userId, null)
                            }
                        }
                        .addOnFailureListener {
                            if (it is FirebaseAuthUserCollisionException && it.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                                showToast(getString(R.string.select_other_username_message))
                            } else {
                                showToast(it.message)
                            }
                        }
                }
            }
        }

        mBinding.imvAvatar.setOnClickListener {
            AlertDialog.Builder(this)
                .setPositiveButton(getString(R.string.from_gallery)) { _, _ ->
                    if (isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        pickImage()
                    } else {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
                    }
                }
                .setNegativeButton(getString(R.string.take_a_picture)) { _, _ ->
                    if (isPermissionGranted(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        takeImage()
                    } else {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                    }
                }
                .setTitle(getString(R.string.select_image))
                .show()
        }
    }

    private fun uploadImage(userId: String, onSuccess: (String) -> Unit) {
        val reference = Firebase.storage.reference
            .child("user")
            .child(userId)
            .child(userId)
        reference
            .putFile(mViewModel.imageUri!!)
            .addOnFailureListener {
                showLoading(false)
                showToast(it.message)
            }
            .addOnSuccessListener {
                reference.downloadUrl.addOnSuccessListener {
                    onSuccess(it.toString())
                }
            }
    }

    private fun addUser(email: String, password: String, userId: String, imageUrl: String?) {
        Firebase.database(Constant.FIREBASE_DATABASE).reference
            .child("user")
            .child(userId)
            .setValue(
                User().apply {
                    name = mBinding.edtName.text.toString().trim()
                    phone = mBinding.edtPhone.text.toString().trim()
                    grade = mBinding.edtGrade.text.toString().trim()
                    image = imageUrl
                }
            )
            .addOnSuccessListener {
                if (mViewModel.user != null) {
                    showToast(getString(R.string.update_profile_successfully))
                } else {
                    mViewModel.sharedPreferencesHelper[Constant.PREF_EMAIL] = email
                    mViewModel.sharedPreferencesHelper[Constant.PREF_PASSWORD] = password
                    showToast(getString(R.string.register_successfully))
                }
                showLoading(false)
                startActivity(
                    Intent(this, HomeActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
            .addOnFailureListener {
                showLoading(false)
                showToast(it.message)
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 0) {
                pickImage()
            } else if (requestCode == 1) {
                takeImage()
            }
        }
    }

    private fun pickImage() {
        mPickForResult.launch("image/*")
    }

    private val mPickForResult = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            editImage(it)
        }
    }

    private fun takeImage() {
        mViewModel.tempFile = File.createTempFile("temp_", ".jpg", externalCacheDir)
        val tempUri = FileProvider.getUriForFile(this, "com.app.bestbook.fileprovider", mViewModel.tempFile!!)
        mTakeForResult.launch(tempUri)
    }

    private val mTakeForResult = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            MediaScannerConnection.scanFile(this, arrayOf(mViewModel.tempFile!!.path), null) { _, uri ->
                editImage(uri)
            }
        }
    }

    private fun editImage(uri: Uri) {
        mViewModel.imageUri = uri
        val editIntent = Intent(Intent.ACTION_EDIT)
            .setDataAndType(uri, "image/*")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        mEditForResult.launch(Intent.createChooser(editIntent, "Edit your photo"))
    }

    private val mEditForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let {
            mViewModel.imageUri = it
        }
        mBinding.imvAvatar.setImageURI(mViewModel.imageUri)
    }

    override fun onDestroy() {
        externalCacheDir?.delete()
        super.onDestroy()
    }

    private fun isValid(): Boolean {
        var b = true
        with(mBinding) {
            if (edtUserName.text.isBlank()) {
                edtUserName.error = getString(R.string.please_enter_information)
                b = false
            }
            if (edtPassword.text.length < 6) {
                edtPassword.error = getString(R.string.password_invalid)
                b = false
            } else if (mViewModel.user != null && edtPassword.text.toString() != mViewModel.sharedPreferencesHelper[Constant.PREF_PASSWORD]) {
                showToast(getString(R.string.wrong_password))
                b = false
            }
            if (edtName.text.isBlank()) {
                edtName.error = getString(R.string.please_enter_information)
                b = false
            }
        }
        return b
    }
}