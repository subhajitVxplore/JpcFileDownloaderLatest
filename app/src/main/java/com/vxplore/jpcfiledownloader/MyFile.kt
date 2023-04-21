package com.vxplore.jpcfiledownloader
data class MyFile(
    val id:String,
    val name:String,
    val type:String,
    val url:String,
    var downloadedUri:String?=null,
    var isDownloading:Boolean = false,
)