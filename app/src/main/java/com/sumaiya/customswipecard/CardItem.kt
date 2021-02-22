package com.sumaiya.customswipecard

class CardItem {
    private var imgRsc = 0
    private var name: String? = null

    fun getImgRsc(): Int {
        return imgRsc
    }

    fun setImgRsc(imgRsc: Int) {
        this.imgRsc = imgRsc
    }

    fun getName(): String? {
        return name
    }

    fun setName(name: String?) {
        this.name = name
    }
}