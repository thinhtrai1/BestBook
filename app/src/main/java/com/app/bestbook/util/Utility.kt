package com.app.bestbook.util

import com.app.bestbook.model.Subject

object Utility {
    fun generateData(mSubjectData: ArrayList<List<Subject>>) {
        for (i in 1..3) {
            mSubjectData.add(
                listOf(
                    createSubject("toan", "Toán"),
                    createSubject("tiengviet", "Tiếng Việt"),
                    createSubject("tienganh", "Tiếng Anh"),
                    createSubject("tunhienvaxahoi", "Tự nhiên và Xã hội"),
                    createSubject("amnhac", "Âm nhạc"),
                    createSubject("mythuat", "Mỹ thuật"),
                    createSubject("daoduc", "Đạo đức"),
                    createSubject("theduc", "Thể dục"),
                    createSubject("tinhoc", "Tin học"),
                    createSubject("congnghe", "Công nghệ")
                )
            )
        }
        for (i in 4..5) {
            mSubjectData.add(
                listOf(
                    createSubject("toan", "Toán"),
                    createSubject("tiengviet", "Tiếng Việt"),
                    createSubject("tienganh", "Tiếng Anh"),
                    createSubject("tunhienvaxahoi", "Tự nhiên và Xã hội"),
                    createSubject("khoahoc", "Khoa học"),
                    createSubject("lichsu", "Lịch sử"),
                    createSubject("dialy", "Địa lý"),
                    createSubject("amnhac", "Âm nhạc"),
                    createSubject("mythuat", "Mỹ thuật"),
                    createSubject("daoduc", "Đạo đức"),
                    createSubject("theduc", "Thể dục"),
                    createSubject("tinhoc", "Tin học"),
                    createSubject("congnghe", "Công nghệ")
                )
            )
        }
        for (i in 6..9) {
            mSubjectData.add(
                listOf(
                    createSubject("toan", "Toán"),
                    createSubject("nguvan", "Ngữ văn"),
                    createSubject("tienganh", "Tiếng Anh"),
                    createSubject("lichsu", "Lịch sử"),
                    createSubject("dialy", "Địa lý"),
                    createSubject("sinhhoc", "Sinh học"),
                    createSubject("khoahoctunhien", "Khoa học tự nhiên"),
                    createSubject("congnghe", "Công nghệ"),
                    createSubject("giaoduccongdan", "Giáo dục công dân"),
                    createSubject("amnhac", "Âm nhạc"),
                    createSubject("mythuat", "Mỹ thuật"),
                    createSubject("theduc", "Thể dục"),
                    createSubject("tinhoc", "Tin học")
                )
            )
        }
        for (i in 10..12) {
            mSubjectData.add(
                listOf(
                    createSubject("toan", "Toán"),
                    createSubject("nguvan", "Ngữ văn"),
                    createSubject("tienganh", "Tiếng Anh"),
                    createSubject("lichsu", "Lịch sử"),
                    createSubject("dialy", "Địa lý"),
                    createSubject("sinhhoc", "Sinh học"),
                    createSubject("hoahoc", "Hóa học"),
                    createSubject("congnghe", "Công nghệ"),
                    createSubject("giaoduckinhtevaquocphong", "Giáo dục kinh tế và pháp luật"),
                    createSubject("giaoducquocphongvaaninh", "Giáo dục Quốc phòng và An ninh\n"),
                    createSubject("nghethuat", "Nghệ thuật"),
                    createSubject("theduc", "Thể dục"),
                    createSubject("tinhoc", "Tin học")
                )
            )
        }
    }

    private fun createSubject(id: String, name: String) = Subject().apply {
        this.id = id
        this.name = name
    }
}