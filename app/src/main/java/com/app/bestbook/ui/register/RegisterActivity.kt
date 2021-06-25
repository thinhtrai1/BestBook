package com.app.bestbook.ui.register

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityRegisterBinding
import com.app.bestbook.databinding.ProgressDialogCustomBinding
import com.app.bestbook.model.User
import com.app.bestbook.ui.home.HomeActivity
import com.app.bestbook.util.Constant
import com.app.bestbook.util.email
import com.app.bestbook.util.isPermissionGranted
import com.app.bestbook.util.showToast
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File

class RegisterActivity: BaseActivity() {
    private lateinit var mBinding: ActivityRegisterBinding
    private val mViewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_register)

        mBinding.btnLogin.setOnClickListener {
            if (isValid()) {
                val email = mBinding.edtUserName.text.toString().trim().email()
                val password = mBinding.edtPassword.text.toString().trim()
                Firebase.auth
                    .createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val userId = result.user?.uid ?: return@addOnSuccessListener
                        val binding: ProgressDialogCustomBinding
                        val progressDialog = Dialog(this).apply {
                            binding = ProgressDialogCustomBinding.inflate(LayoutInflater.from(this@RegisterActivity))
                            setContentView(binding.root)
                            setCancelable(false)
                            window!!.setBackgroundDrawableResource(android.R.color.transparent)
                            show()
                            binding.tvMessage.text = getString(R.string.saving_book)
                        }
                        if (mViewModel.imageUri != null) {
                            val reference = Firebase.storage.reference
                                .child("user")
                                .child(userId)
                                .child(userId)
                            reference
                                .putFile(mViewModel.imageUri!!)
                                .addOnProgressListener {
                                    binding.progressBarDownload.progress = 100f * it.bytesTransferred / it.totalByteCount
                                }
                                .addOnFailureListener {
                                    progressDialog.dismiss()
                                    showToast(it.message)
                                }
                                .addOnSuccessListener {
                                    reference.downloadUrl.addOnSuccessListener {
                                        addUser(progressDialog, email, password, userId, it.toString())
                                    }
                                }
                        } else {
                            addUser(progressDialog, email, password, userId, null)
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

    private fun addUser(progressDialog: Dialog, email: String, password: String, userId: String, imageUrl: String?) {
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
                mViewModel.sharedPreferencesHelper.set(Constant.PREF_EMAIL, email)[Constant.PREF_PASSWORD] = password
                progressDialog.dismiss()
                startActivity(
                    Intent(this, HomeActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
            .addOnFailureListener {
                progressDialog.dismiss()
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
            }
            if (edtName.text.isBlank()) {
                edtName.error = getString(R.string.please_enter_information)
                b = false
            }
        }
        return b
    }
}