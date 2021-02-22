package com.sumaiya.customswipecard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import java.util.ArrayList

class CardAdapter(
    private val mContext: Context,
    list: ArrayList<CardItem?>
) : ArrayAdapter<CardItem?>(mContext, 0, list) {
    private var mDataset = ArrayList<CardItem?>()
    fun addData(mDataset: ArrayList<CardItem?>) {
        this.mDataset = mDataset
        notifyDataSetChanged()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var listItem = convertView
        if (listItem == null) listItem =
            LayoutInflater.from(mContext).inflate(R.layout.item, parent, false)
        val cardItem = mDataset[position]
        val imgCardView =
            listItem!!.findViewById<View>(R.id.img_card_item) as ImageView
        if (cardItem != null) {
            imgCardView.setImageResource(cardItem.getImgRsc())
        }
        return listItem
    }

    init {
        mDataset = list
    }
}