package com.app.bestbook.ui.subject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.bestbook.R
import com.app.bestbook.databinding.ItemRcvSubjectBinding
import com.app.bestbook.model.Subject
import com.app.bestbook.util.getDimension
import com.squareup.picasso.Picasso

class SubjectRcvAdapter(private val data: List<Subject>) : RecyclerView.Adapter<SubjectRcvAdapter.ViewHolder>() {
    lateinit var onClickListener: (Subject) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRcvSubjectBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        data[position].let {
            holder.view.tvName.text = it.name
            if (it.image != null) {
                Picasso.get().load(it.image).resize(getDimension(R.dimen.size_192), 0).placeholder(R.drawable.ic_menu).into(holder.view.imvImage)
            } else {
                holder.view.imvImage.setImageResource(R.drawable.ic_menu)
            }
            holder.view.root.setOnClickListener { _ ->
                onClickListener(it)
            }
        }
    }

    class ViewHolder(val view: ItemRcvSubjectBinding) : RecyclerView.ViewHolder(view.root)
}