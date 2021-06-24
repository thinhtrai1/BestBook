package com.app.bestbook.ui.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.app.bestbook.databinding.ItemRcvBookBinding

class BookRcvAdapter  : RecyclerView.Adapter<BookRcvAdapter.ViewHolder>() {
    lateinit var onClickListener: (String) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRcvBookBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return 3
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.root.setOnClickListener {
            onClickListener.invoke("")
        }
    }

    class ViewHolder(val view: ItemRcvBookBinding) : RecyclerView.ViewHolder(view.root)
}