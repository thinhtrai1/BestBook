package com.app.bestbook.ui.addBook

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityAddBookBinding
import com.app.bestbook.databinding.ProgressDialogCustomBinding
import com.app.bestbook.model.Book
import com.app.bestbook.model.Subject
import com.app.bestbook.ui.cropImage.CropImageActivity
import com.app.bestbook.ui.home.HomeActivity
import com.app.bestbook.util.Constant
import com.app.bestbook.util.Utility
import com.app.bestbook.util.isPermissionGranted
import com.app.bestbook.util.showToast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.*
import kotlin.collections.ArrayList

class AddBookActivity : BaseActivity() {
    private lateinit var mBinding: ActivityAddBookBinding
    private val mSubjectData = ArrayList<List<Subject>>()
    private var mFileUri: Uri? = null
    private var mImageUri: Uri? = null
    private val mDatabaseReference = Firebase.database(Constant.FIREBASE_DATABASE).reference.child("data")
    private val mStorageReference = Firebase.storage.reference
    private var mBookUrl: String? = null
    private var mImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_add_book)
        supportActionBar?.title = getString(R.string.add_book)

        if (Firebase.auth.currentUser?.email != "admin@vnbooks.com") {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        if (intent.action == Intent.ACTION_SEND) {
            Firebase.database(Constant.FIREBASE_DATABASE).reference.child("token").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    showToast(getString(R.string.an_error_occurred_please_try_again))
                    finish()
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != "test") {
                        showToast("The app has been blocked by the developer!")
                        finish()
                    }
                }
            })
        }

        with(mBinding) {
            spnStartPage.adapter = ArrayAdapter(this@AddBookActivity, android.R.layout.simple_list_item_1, listOf("0", "1", "2", "3", "4", "5"))

            spnClass.adapter = ArrayAdapter(this@AddBookActivity, android.R.layout.simple_list_item_1, listOf("Select class", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"))
            spnClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) {
                        spnSubject.adapter = ArrayAdapter(this@AddBookActivity, android.R.layout.simple_list_item_1, listOf("Select class"))
                    } else {
                        spnSubject.adapter = ArrayAdapter(this@AddBookActivity, android.R.layout.simple_list_item_1, mSubjectData[position - 1].map { it.name })
                    }
                }
            }

            btnSelectFile.setOnClickListener {
                if (isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    ActivityCompat.requestPermissions(this@AddBookActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
                } else {
                    openFile()
                }
            }

            btnSelectImage.setOnClickListener {
                if (isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    pickImage()
                } else {
                    ActivityCompat.requestPermissions(this@AddBookActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
                }
            }

            btnAddBook.setOnClickListener {
                val classSelected = spnClass.selectedItemPosition
                if (mFileUri == null || mImageUri == null || classSelected == 0 || edtBookName.text.isBlank()) {
                    showToast("missing information")
                } else {
                    val subjectSelected = mSubjectData[classSelected - 1][spnSubject.selectedItemPosition].id!!
                    val binding: ProgressDialogCustomBinding
                    val progressDialog = Dialog(this@AddBookActivity).apply {
                        binding = ProgressDialogCustomBinding.inflate(LayoutInflater.from(this@AddBookActivity))
                        setContentView(binding.root)
                        setCancelable(false)
                        window!!.setBackgroundDrawableResource(android.R.color.transparent)
                        show()
                        binding.tvMessage.text = getString(R.string.saving_book)
                    }
                    val reference = mStorageReference
                        .child("book")
                        .child(classSelected.toString())
                        .child(subjectSelected)
                        .child(tvFileName.text.toString())
                    reference.putFile(mFileUri!!)
                        .addOnProgressListener {
                            binding.progressBarDownload.progress = 100f * it.bytesTransferred / it.totalByteCount
                        }
                        .addOnFailureListener {
                            progressDialog.dismiss()
                            showToast(it.message)
                        }
                        .addOnSuccessListener {
                            reference.downloadUrl.addOnSuccessListener {
                                mBookUrl = it.toString()
                                if (mImageUrl != null) {
                                    addBook(classSelected, subjectSelected, progressDialog)
                                }
                            }
                        }
                    mStorageReference
                        .child("image")
                        .child(classSelected.toString())
                        .child(subjectSelected)
                        .child(Calendar.getInstance().timeInMillis.toString()).apply {
                            putFile(mImageUri!!)
                                .addOnFailureListener {
                                    progressDialog.dismiss()
                                    showToast(it.message)
                                }
                                .addOnSuccessListener {
                                    downloadUrl.addOnSuccessListener {
                                        mImageUrl = it.toString()
                                        if (mBookUrl != null) {
                                            addBook(classSelected, subjectSelected, progressDialog)
                                        }
                                    }
                                }
                        }
                }
            }
        }

        Utility.generateData(mSubjectData)

        intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)?.let {
            mFileUri = it
            mBinding.tvFileName.text = mFileUri!!.lastPathSegment
        }
    }

    private fun addBook(classSelected: Int, subjectSelected: String, progressDialog: Dialog) {
        val subjectRef = mDatabaseReference
            .child(classSelected.toString())
            .child(subjectSelected)
        subjectRef.child("name").setValue(mSubjectData[classSelected - 1][mBinding.spnSubject.selectedItemPosition].name)
        subjectRef.child("books").push().setValue(
            Book().apply {
                name = mBinding.edtBookName.text.toString().trim()
                url = mBookUrl!!
                image = mImageUrl!!
                startPage = mBinding.spnStartPage.selectedItemPosition
                time = Calendar.getInstance().timeInMillis
            }
        )
        progressDialog.dismiss()
        mFileUri = null
        mImageUri = null
        mBinding.tvFileName.text = ""
        mBinding.imvImage.setImageResource(0)
        mBinding.spnClass.setSelection(0)
        mBinding.spnStartPage.setSelection(0)
        mBinding.edtBookName.text = null
    }

    private fun openFile() {
        startPdfFileForResult.launch(arrayOf("application/pdf"))
    }

    private val startPdfFileForResult = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let {
            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                mBinding.tvFileName.text = cursor.getString(nameIndex)
            }
            mFileUri = it
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
                100 -> openFile()
                101 -> pickImage()
            }
        }
    }
}