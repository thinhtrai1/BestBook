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
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityAddBookBinding
import com.app.bestbook.databinding.ProgressDialogCustomBinding
import com.app.bestbook.model.Book
import com.app.bestbook.ui.cropImage.CropImageActivity
import com.app.bestbook.ui.home.HomeActivity
import com.app.bestbook.util.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import java.util.*

class AddBookActivity : BaseActivity() {
    private lateinit var mBinding: ActivityAddBookBinding
    private val mViewModel: AddBookViewModel by viewModels()
    private var mSpnSubjectIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_add_book)
        supportActionBar?.title = getString(R.string.add_book)

        if (Firebase.auth.currentUser?.email != Constant.ADMIN.email()) {
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
            spnClass.adapter = ArrayAdapter(this@AddBookActivity, android.R.layout.simple_list_item_1, listOf("Select class", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"))
            spnClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) {
                        spnSubject.adapter = ArrayAdapter(this@AddBookActivity, android.R.layout.simple_list_item_1, listOf("Select class"))
                    } else {
                        spnSubject.adapter = ArrayAdapter(this@AddBookActivity, android.R.layout.simple_list_item_1, mViewModel.subjectData[position - 1].map { it.name })
                        if (mSpnSubjectIndex != -1) {
                            spnSubject.setSelection(mSpnSubjectIndex)
                        }
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
                if (mViewModel.data != null) {
                    if (edtBookName.text.isBlank()) {
                        showToast(getString(R.string.missing_information))
                    } else {
                        updateImage()
                    }
                } else {
                    if (mViewModel.fileUri == null || mViewModel.imageUri == null || spnClass.selectedItemPosition == 0 || edtBookName.text.isBlank()) {
                        showToast(getString(R.string.missing_information))
                    } else {
                        putFileAndImage()
                    }
                }
            }

            mViewModel.data?.let { book ->
                if (book.id == null || mViewModel.subject == null || mViewModel.grade == null || mViewModel.grade!! > mViewModel.subjectData.size) {
                    showToast(getString(R.string.data_error))
                    finish()
                    return
                }
                btnSelectFile.visibility = View.GONE
                tvFileName.visibility = View.GONE
                spnClass.isEnabled = false
                spnSubject.isEnabled = false
                spnClass.setSelection(mViewModel.grade!!)
                mSpnSubjectIndex = mViewModel.subjectData[mViewModel.grade!! - 1].indexOfFirst { it.name == mViewModel.subject }
                if (mSpnSubjectIndex != -1) {
                    spnSubject.setSelection(mSpnSubjectIndex)
                }
                book.image?.let {
                    Picasso.get().load(it).resize(resources.getDimensionPixelSize(R.dimen.avatar_size), 0).centerCrop().into(imvImage)
                }
                edtBookName.setText(book.name)
                edtStartPage.setText(book.startPage.toString())
                btnAddBook.text = getString(R.string.update_book)
            }
        }

        intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)?.let {
            mViewModel.fileUri = it
            mBinding.tvFileName.text = mViewModel.fileUri!!.lastPathSegment
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (Firebase.auth.currentUser?.email != Constant.ADMIN.email()) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        intent?.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)?.let {
            mViewModel.fileUri = it
            mBinding.tvFileName.text = mViewModel.fileUri!!.lastPathSegment
        }
    }

    private fun putFileAndImage() {
        val classSelected = mBinding.spnClass.selectedItemPosition
        val subjectSelected = mViewModel.subjectData[classSelected - 1][mBinding.spnSubject.selectedItemPosition].id!!
        val binding: ProgressDialogCustomBinding
        val progressDialog = Dialog(this@AddBookActivity).apply {
            binding = ProgressDialogCustomBinding.inflate(LayoutInflater.from(this@AddBookActivity))
            setContentView(binding.root)
            setCancelable(false)
            window!!.setBackgroundDrawableResource(android.R.color.transparent)
            show()
            binding.tvMessage.text = getString(R.string.saving_book)
        }
        val reference = mViewModel.storageReference
            .child("book")
            .child(classSelected.toString())
            .child(subjectSelected)
            .child(mBinding.tvFileName.text.toString())
        reference.putFile(mViewModel.fileUri!!)
            .addOnProgressListener {
                binding.progressBarDownload.progress = 100f * it.bytesTransferred / it.totalByteCount
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                showToast(it.message)
            }
            .addOnSuccessListener {
                reference.downloadUrl.addOnSuccessListener {
                    mViewModel.bookUrl = it.toString()
                    if (mViewModel.imageUrl != null) {
                        addBook(classSelected, subjectSelected, progressDialog)
                    }
                }
            }
        mViewModel.storageReference
            .child("image")
            .child(classSelected.toString())
            .child(subjectSelected)
            .child(Calendar.getInstance().timeInMillis.toString()).apply {
                putFile(mViewModel.imageUri!!)
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        showToast(it.message)
                    }
                    .addOnSuccessListener {
                        downloadUrl.addOnSuccessListener {
                            mViewModel.imageUrl = it.toString()
                            if (mViewModel.bookUrl != null) {
                                addBook(classSelected, subjectSelected, progressDialog)
                            }
                        }
                    }
            }
    }

    private fun addBook(classSelected: Int, subjectSelected: String, progressDialog: Dialog) {
        val subjectRef = mViewModel.databaseReference
            .child(classSelected.toString())
            .child(subjectSelected)
        subjectRef.child("name").setValue(mViewModel.subjectData[classSelected - 1][mBinding.spnSubject.selectedItemPosition].name)
        subjectRef.child("books").push().setValue(
            Book().apply {
                name = mBinding.edtBookName.text.toString().trim()
                url = mViewModel.bookUrl!!
                image = mViewModel.imageUrl!!
                startPage = mBinding.edtStartPage.text.toString().toIntOrNull() ?: 0
                time = Calendar.getInstance().timeInMillis
            }
        ).addOnSuccessListener {
            progressDialog.dismiss()
            showToast(getString(R.string.success))
            mViewModel.fileUri = null
            mViewModel.imageUri = null
            mBinding.tvFileName.text = ""
            mBinding.imvImage.setImageResource(0)
            mBinding.spnClass.setSelection(0)
            mBinding.edtStartPage.setText("0")
            mBinding.edtBookName.text = null
        }.addOnFailureListener {
            progressDialog.dismiss()
            showToast(it.message)
        }
    }

    private fun updateImage() {
        showLoading(true)
        val subjectId = mViewModel.subjectData[mViewModel.grade!! - 1].first { it.name == mViewModel.subject }.id!!
        if (mViewModel.imageUri != null) {
            Firebase.storage.getReferenceFromUrl(mViewModel.data!!.image!!).apply {
                putFile(mViewModel.imageUri!!)
                    .addOnFailureListener {
                        showLoading(false)
                        showToast(it.message)
                    }
                    .addOnSuccessListener {
                        downloadUrl.addOnSuccessListener {
                            mViewModel.imageUrl = it.toString()
                            updateBook(subjectId)
                        }
                    }
            }
        } else {
            updateBook(subjectId)
        }
    }

    private fun updateBook(subjectId: String) {
        val book = Book().apply {
            name = mBinding.edtBookName.text.toString().trim()
            url = mViewModel.data!!.url
            image = mViewModel.imageUrl ?: mViewModel.data!!.image
            startPage = mBinding.edtStartPage.text.toString().toIntOrNull() ?: 0
            time = mViewModel.data!!.time
        }
        mViewModel.databaseReference
            .child(mViewModel.grade!!.toString())
            .child(subjectId)
            .child("books")
            .child(mViewModel.data!!.id!!)
            .setValue(book)
            .addOnSuccessListener {
                showLoading(false)
                finish()
            }
            .addOnFailureListener {
                showLoading(false)
                showToast(it.message)
            }
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
            mViewModel.fileUri = it
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
                100 -> openFile()
                101 -> pickImage()
            }
        }
    }
}