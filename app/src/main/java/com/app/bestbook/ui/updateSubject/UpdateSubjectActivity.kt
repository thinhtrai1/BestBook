package com.app.bestbook.ui.updateSubject

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityUpdateSubjectBinding
import com.app.bestbook.databinding.ProgressDialogCustomBinding
import com.app.bestbook.model.Subject
import com.app.bestbook.ui.cropImage.CropImageActivity
import com.app.bestbook.util.Constant
import com.app.bestbook.util.Utility
import com.app.bestbook.util.isPermissionGranted
import com.app.bestbook.util.showToast
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlin.collections.ArrayList

class UpdateSubjectActivity : BaseActivity() {
    private lateinit var mBinding: ActivityUpdateSubjectBinding
    private val mSubjectData = ArrayList<List<Subject>>()
    private var mImageUri: Uri? = null
    private val mDatabaseReference = Firebase.database(Constant.FIREBASE_DATABASE).reference.child("data")
    private val mStorageReference = Firebase.storage.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_update_subject)

        with(mBinding) {
            spnClass.adapter = ArrayAdapter(this@UpdateSubjectActivity, android.R.layout.simple_list_item_1, listOf("Select class", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"))
            spnClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) {
                        spnSubject.adapter = ArrayAdapter(this@UpdateSubjectActivity, android.R.layout.simple_list_item_1, listOf("Select class"))
                    } else {
                        spnSubject.adapter = ArrayAdapter(this@UpdateSubjectActivity, android.R.layout.simple_list_item_1, mSubjectData[position - 1].map { it.name })
                    }
                }
            }

            btnSelectImage.setOnClickListener {
                if (isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    pickImage()
                } else {
                    ActivityCompat.requestPermissions(this@UpdateSubjectActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
                }
            }

            btnAddBook.setOnClickListener {
                val classSelected = spnClass.selectedItemPosition
                if (mImageUri == null || classSelected == 0) {
                    showToast("missing information")
                } else {
                    val subjectSelected = mSubjectData[classSelected - 1][spnSubject.selectedItemPosition].id!!
                    val binding: ProgressDialogCustomBinding
                    val progressDialog = Dialog(this@UpdateSubjectActivity).apply {
                        binding = ProgressDialogCustomBinding.inflate(LayoutInflater.from(this@UpdateSubjectActivity))
                        setContentView(binding.root)
                        setCancelable(false)
                        window!!.setBackgroundDrawableResource(android.R.color.transparent)
                        show()
                        binding.tvMessage.text = getString(R.string.saving_book)
                    }
                    mStorageReference
                        .child("image")
                        .child(classSelected.toString())
                        .child(subjectSelected)
                        .child(subjectSelected).apply {
                            putFile(mImageUri!!)
                                .addOnProgressListener {
                                    binding.progressBarDownload.progress = 100f * it.bytesTransferred / it.totalByteCount
                                }
                                .addOnFailureListener {
                                    progressDialog.dismiss()
                                    showToast(it.message)
                                }
                                .addOnSuccessListener {
                                    downloadUrl.addOnSuccessListener {
                                        addBook(it.toString(), classSelected, subjectSelected, progressDialog)
                                    }
                                }
                        }
                }
            }
        }

        Utility.generateData(mSubjectData)
    }

    private fun addBook(imageUrl: String, classSelected: Int, subjectSelected: String, progressDialog: Dialog) {
        mDatabaseReference
            .child(classSelected.toString())
            .child(subjectSelected)
            .child("image")
            .setValue(imageUrl)
            .addOnSuccessListener {
                progressDialog.dismiss()
                mImageUri = null
                mBinding.imvImage.setImageResource(0)
                mBinding.spnClass.setSelection(0)
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                showToast(it.message)
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

    private fun editImage(uri: Uri) {
        val i = Intent(this, CropImageActivity::class.java).setData(uri)
        mEditForResult.launch(i)
    }

    private val mEditForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let {
            mImageUri = it
            mBinding.imvImage.setImageURI(mImageUri)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            when (requestCode) {
                101 -> pickImage()
            }
        }
    }
}