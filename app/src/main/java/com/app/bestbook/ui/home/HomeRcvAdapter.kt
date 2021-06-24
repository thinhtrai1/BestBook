package com.app.bestbook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.app.bestbook.R
import com.app.bestbook.databinding.ItemRcvHomeBinding
import com.app.bestbook.util.getString

class HomeRcvAdapter(private val list: List<Int>, private val onClick: (Int) -> Unit) : RecyclerView.Adapter<HomeRcvAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRcvHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.tvClass.text = getString(R.string.class_format, list[position])
        holder.view.root.setOnClickListener {
            onClick(list[position])
        }
    }

    class ViewHolder(val view: ItemRcvHomeBinding) : RecyclerView.ViewHolder(view.root)
}