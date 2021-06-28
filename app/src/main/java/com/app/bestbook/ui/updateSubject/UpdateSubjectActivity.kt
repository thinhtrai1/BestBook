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
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityUpdateSubjectBinding
import com.app.bestbook.databinding.ProgressDialogCustomBinding
import com.app.bestbook.ui.cropImage.CropImageActivity
import com.app.bestbook.util.isPermissionGranted
import com.app.bestbook.util.showToast
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso

class UpdateSubjectActivity : BaseActivity() {
    private lateinit var mBinding: ActivityUpdateSubjectBinding
    private val mViewModel: UpdateSubjectViewModel by viewModels()
    private var mSpnSubjectIndex = -1

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
                        spnSubject.adapter = ArrayAdapter(this@UpdateSubjectActivity, android.R.layout.simple_list_item_1, mViewModel.subjectData[position - 1].map { it.name })
                        if (mSpnSubjectIndex != -1) {
                            spnSubject.setSelection(mSpnSubjectIndex)
                        }
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
                if (mViewModel.imageUri == null || spnClass.selectedItemPosition == 0) {
                    showToast(getString(R.string.missing_information))
                } else {
                    val classSelected = mViewModel.grade ?: spnClass.selectedItemPosition
                    val subjectSelected = mViewModel.subject?.id ?: mViewModel.subjectData[classSelected - 1][spnSubject.selectedItemPosition].id!!
                    val binding: ProgressDialogCustomBinding
                    val progressDialog = Dialog(this@UpdateSubjectActivity).apply {
                        binding = ProgressDialogCustomBinding.inflate(LayoutInflater.from(this@UpdateSubjectActivity))
                        setContentView(binding.root)
                        setCancelable(false)
                        window!!.setBackgroundDrawableResource(android.R.color.transparent)
                        show()
                        binding.tvMessage.text = getString(R.string.saving_book)
                    }
                    if (mViewModel.subject?.image != null) {
                        Firebase.storage.getReferenceFromUrl(mViewModel.subject!!.image!!)
                    } else {
                        mViewModel.storageReference
                            .child("image")
                            .child(classSelected.toString())
                            .child(subjectSelected)
                            .child(subjectSelected)
                    }.apply {
                        putFile(mViewModel.imageUri!!)
                            .addOnProgressListener {
                                binding.progressBarDownload.progress = 100f * it.bytesTransferred / it.totalByteCount
                            }
                            .addOnFailureListener {
                                progressDialog.dismiss()
                                showToast(it.message)
                            }
                            .addOnSuccessListener {
                                downloadUrl.addOnSuccessListener {
                                    updateSubject(it.toString(), classSelected, subjectSelected, progressDialog)
                                }
                            }
                    }
                }
            }

            mViewModel.subject?.let { subject ->
                if (subject.id == null || mViewModel.grade == null || mViewModel.grade!! >= mViewModel.subjectData.size) {
                    showToast(getString(R.string.data_error))
                    finish()
                    return
                }
                spnClass.isEnabled = false
                spnSubject.isEnabled = false
                spnClass.setSelection(mViewModel.grade!!)
                mSpnSubjectIndex = mViewModel.subjectData[mViewModel.grade!! - 1].indexOfFirst { it.id == mViewModel.subject!!.id }
                if (mSpnSubjectIndex != -1) {
                    spnSubject.setSelection(mSpnSubjectIndex)
                }
                subject.image?.let {
                    Picasso.get().load(it).resize(resources.getDimensionPixelSize(R.dimen.avatar_size), 0).centerCrop().into(imvImage)
                }
                btnAddBook.text = getString(R.string.update_subject)
            }
        }
    }

    private fun updateSubject(imageUrl: String, classSelected: Int, subjectSelected: String, progressDialog: Dialog) {
        mViewModel.databaseReference
            .child(classSelected.toString())
            .child(subjectSelected)
            .child("image")
            .setValue(imageUrl)
            .addOnSuccessListener {
                progressDialog.dismiss()
                showToast(getString(R.string.success))
                if (mViewModel.subject != null) {
                    finish()
                } else {
                    mViewModel.imageUri = null
                    mBinding.imvImage.setImageResource(0)
                    mBinding.spnClass.setSelection(0)
                }
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
            mViewModel.imageUri = it
            mBinding.imvImage.setImageURI(mViewModel.imageUri)
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