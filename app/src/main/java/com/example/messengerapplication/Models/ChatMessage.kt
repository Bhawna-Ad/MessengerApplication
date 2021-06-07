package com.example.messengerapplication.Models

class ChatMessage(val id: String, val text: String, val fromId: String, val toId: String, val timestamp: Long, val ivBytes: String) {

    constructor() : this("", "", "", "", -1, "")


//    byteArrayOf(), byteArrayOf()
}