package com.app.bestbook.ui.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.bestbook.R
import com.app.bestbook.databinding.ItemRcvBookBinding
import com.app.bestbook.model.Book
import com.app.bestbook.util.getDimension
import com.squareup.picasso.Picasso

class BookRcvAdapter(private val data: List<Book>) : RecyclerView.Adapter<BookRcvAdapter.ViewHolder>() {
    lateinit var onClickListener: (Book) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRcvBookBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        data[position].let {
            holder.view.tvName.text = it.name
            if (it.image != null) {
                Picasso.get().load(it.image).resize(getDimension(R.dimen.thumbnail_240), 0).placeholder(R.drawable.ic_book_holder).into(holder.view.imvImage)
            } else {
                holder.view.imvImage.setImageResource(R.drawable.ic_book_holder)
            }
            holder.view.root.setOnClickListener { _ ->
                onClickListener(it)
            }
        }
    }

    class ViewHolder(val view: ItemRcvBookBinding) : RecyclerView.ViewHolder(view.root)
}