package com.app.bestbook.ui.addBook

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.app.bestbook.R
import com.app.bestbook.base.BaseActivity
import com.app.bestbook.databinding.ActivityAddBookBinding
import com.app.bestbook.databinding.ProgressDialogCustomBinding
import com.app.bestbook.model.Book
import com.app.bestbook.model.Subject
import com.app.bestbook.util.showToast
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class AddBookActivity : BaseActivity() {
    private lateinit var mBinding: ActivityAddBookBinding
    private val mSubjectData = ArrayList<List<Subject>>()
    private var mFileUri: Uri? = null
    private val mDatabaseReference = Firebase.database("https://bestbook-93f2f-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    private val mStorageReference = Firebase.storage.reference.child("book")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_add_book)

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
                if (ContextCompat.checkSelfPermission(this@AddBookActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@AddBookActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
                } else {
                    openFile()
                }
            }

            btnAddBook.setOnClickListener {
                val classSelected = spnClass.selectedItemPosition
                if (mFileUri == null || classSelected == 0 || edtBookName.text.isNullOrBlank()) {
                    showToast("missing information")
                } else {
                    val subjectSelected = mSubjectData[classSelected][spnSubject.selectedItemPosition].id
                    val binding: ProgressDialogCustomBinding
                    val progressDialog = Dialog(this@AddBookActivity).apply {
                        binding = ProgressDialogCustomBinding.inflate(LayoutInflater.from(this@AddBookActivity))
                        setContentView(binding.root)
                        setCancelable(false)
                        window!!.setBackgroundDrawableResource(android.R.color.transparent)
                        show()
                    }
                    val reference = mStorageReference
                        .child(classSelected.toString())
                        .child(subjectSelected)
                        .child(mFileUri!!.lastPathSegment!!)
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
                                val subjectRef = mDatabaseReference
                                    .child(classSelected.toString())
                                    .child(subjectSelected)
                                subjectRef.child("name").setValue(mSubjectData[classSelected][spnSubject.selectedItemPosition].name)
                                subjectRef.child("books").push().setValue(Book(edtBookName.text.toString().trim(), it.toString(), spnStartPage.selectedItemPosition))
                                progressDialog.dismiss()
                            }
                        }
                }
            }
        }

        generateData()

        intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)?.let {
            mFileUri = it
            mBinding.imvFileSelected.visibility = View.VISIBLE
        }
    }

    private fun openFile() {
        startPdfFileForResult.launch(arrayOf("application/pdf"))
    }

    private val startPdfFileForResult = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let {
            mFileUri = it
            mBinding.imvFileSelected.visibility = View.VISIBLE
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            when (requestCode) {
                100 -> openFile()
            }
        }
    }

    private fun generateData() {
        for (i in 1..3) {
            mSubjectData.add(listOf(
                Subject("toan", "Toán"),
                Subject("tiengviet", "Tiếng Việt"),
                Subject("tienganh", "Tiếng Anh"),
                Subject("tunhienvaxahoi", "Tự nhiên và Xã hội"),
                Subject("amnhac", "Âm nhạc"),
                Subject("mythuat", "Mỹ thuật"),
                Subject("daoduc", "Đạo đức"),
                Subject("theduc", "Thể dục"),
                Subject("tinhoc", "Tin học"),
                Subject("congnghe", "Công nghệ")
            ))
        }
        for (i in 4..5) {
            mSubjectData.add(listOf(
                Subject("toan", "Toán"),
                Subject("tiengviet", "Tiếng Việt"),
                Subject("tienganh", "Tiếng Anh"),
                Subject("tunhienvaxahoi", "Tự nhiên và Xã hội"),
                Subject("khoahoc", "Khoa học"),
                Subject("lichsu", "Lịch sử"),
                Subject("dialy", "Địa lý"),
                Subject("amnhac", "Âm nhạc"),
                Subject("mythuat", "Mỹ thuật"),
                Subject("daoduc", "Đạo đức"),
                Subject("theduc", "Thể dục"),
                Subject("tinhoc", "Tin học"),
                Subject("congnghe", "Công nghệ")
            ))
        }
        for (i in 6..9) {
            mSubjectData.add(
                listOf(
                    Subject("toan", "Toán"),
                    Subject("nguvan", "Ngữ văn"),
                    Subject("tienganh", "Tiếng Anh"),
                    Subject("lichsu", "Lịch sử"),
                    Subject("dialy", "Địa lý"),
                    Subject("sinhhoc", "Sinh học"),
                    Subject("khoahoctunhien", "Khoa học tự nhiên"),
                    Subject("congnghe", "Công nghệ"),
                    Subject("giaoduccongdan", "Giáo dục công dân"),
                    Subject("amnhac", "Âm nhạc"),
                    Subject("mythuat", "Mỹ thuật"),
                    Subject("theduc", "Thể dục"),
                    Subject("tinhoc", "Tin học")
                )
            )
        }
        for (i in 10..12) {
            mSubjectData.add(
                listOf(
                    Subject("toan", "Toán"),
                    Subject("nguvan", "Ngữ văn"),
                    Subject("tienganh", "Tiếng Anh"),
                    Subject("lichsu", "Lịch sử"),
                    Subject("dialy", "Địa lý"),
                    Subject("sinhhoc", "Sinh học"),
                    Subject("hoahoc", "Hóa học"),
                    Subject("congnghe", "Công nghệ"),
                    Subject("giaoduckinhtevaquocphong", "Giáo dục kinh tế và pháp luật"),
                    Subject("giaoducquocphongvaaninh", "Giáo dục Quốc phòng và An ninh\n"),
                    Subject("nghethuat", "Nghệ thuật"),
                    Subject("theduc", "Thể dục"),
                    Subject("tinhoc", "Tin học")
                )
            )
        }
    }
}