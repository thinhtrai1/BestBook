package com.app.bestbook.ui.subject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.bestbook.databinding.ItemRcvSubjectBinding

class SubjectRcvAdapter : RecyclerView.Adapter<SubjectRcvAdapter.ViewHolder>() {
    lateinit var onClickListener: (String) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRcvSubjectBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return 12
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.root.setOnClickListener {
            onClickListener.invoke("")
        }
    }

    class ViewHolder(val view: ItemRcvSubjectBinding) : RecyclerView.ViewHolder(view.root)
}