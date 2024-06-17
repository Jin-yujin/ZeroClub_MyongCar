package com.example.mju_car.comp

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.mju_car.R
import com.example.mju_car.model.ListViewItem

class ListViewAdapter(private val items: MutableList<ListViewItem>): BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): ListViewItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
        var convertView = view
        if (convertView == null) convertView = LayoutInflater.from(parent?.context).inflate(R.layout.comp_list_item, parent, false)

        val item: ListViewItem = items[position]
        convertView!!.findViewById<TextView>(R.id.tvFrom).text  = item.from
        convertView.findViewById<TextView>(R.id.tvTo).text      = item.to
        convertView.findViewById<TextView>(R.id.tvDate).text    = item.dateInfo
        convertView.findViewById<TextView>(R.id.tvWriter).text  = item.writer
        convertView.findViewById<TextView>(R.id.tvPartner).text = "${item.curPartner}/${item.maxPartner}"
        // convertView!!.image_title.setImageDrawable(item.icon)
        //
        // convertView.text_sub_title.text = item.subTitle

        return convertView
    }
}