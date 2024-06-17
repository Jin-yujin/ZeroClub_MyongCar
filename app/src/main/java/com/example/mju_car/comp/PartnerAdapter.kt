package com.example.mju_car.comp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.mju_car.R
import com.example.mju_car.model.PartnerItem

class PartnerAdapter(private val items: MutableList<PartnerItem>): BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): PartnerItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
        var convertView = view
        if (convertView == null) convertView = LayoutInflater.from(parent?.context).inflate(R.layout.comp_partner_item, parent, false)

        val item: PartnerItem = items[position]
        convertView!!.findViewById<TextView>(R.id.tvNickname).text    = item.nickname
        // convertView.findViewById<ImageView>(R.id.tvPartner).setImageDrawable(item.profile)

        return convertView
    }
}