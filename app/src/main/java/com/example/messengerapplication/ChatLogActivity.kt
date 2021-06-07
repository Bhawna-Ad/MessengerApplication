package com.example.messengerapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Toast
import com.example.messengerapplication.Models.ChatMessage
import com.example.messengerapplication.Models.User
import com.example.messengerapplication.encryption.hexToByteArray
import com.example.messengerapplication.encryption.toHex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_low.view.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class ChatLogActivity : AppCompatActivity() {

    companion object {
        const val ANDROID_KEY_STORE: String = "AndroidKeyStore"
        const val TRANSFORMATION: String = "AES/GCM/NoPadding"
        const val KEY_ALIAS: String = "alias"
    }

    val adapter = GroupAdapter<ViewHolder>()
    var toUser: User? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        recycler_view_chat_log.adapter = adapter


        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)

        supportActionBar?.title = toUser?.username

        listenForMessages()

        send_button_chat_log.setOnClickListener {
            Toast.makeText(this, "Send button clicked!", Toast.LENGTH_SHORT).show()

            performSendMessage()
        }
    }

    private fun listenForMessages() {

        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid

        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                if (chatMessage != null) {
                    if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                        val currentUser = MessengerActivity.currentUser ?: return
                        adapter.add(ChatFromItem(dec(chatMessage.text.toString().hexToByteArray(), chatMessage.ivBytes.toString().hexToByteArray()), currentUser))
                    } else {
                        adapter.add(ChatToItem(dec(chatMessage.text.toString().hexToByteArray(), chatMessage.ivBytes.toString().hexToByteArray()), toUser!!))
                    }
                }
                recycler_view_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }
        })
    }



    private fun performSendMessage() {

        //Data Encryption
//        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
//        val keyGenParameterSpec = KeyGenParameterSpec.Builder("MyKeyAlias",
//                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
//                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
//                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
//                .build()
//
//        keyGenerator.init(keyGenParameterSpec)
//        keyGenerator.generateKey()
//
//        val pair = encryptData(enter_message_chat_log.text.toString())
//        val text = pair.second.toString(Charsets.UTF_8)
//
//        val decryptedText = decryptData(pair.first, pair.second)


//        pair : ([B@cc9a1dc, [B@d5322e5)

        //change here
        val textMessage = enc(enter_message_chat_log.text.toString())


        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user?.uid

        if (fromId == null) return

//        val reference = FirebaseDatabase.getInstance().getReference("/messages").push()
        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessage = ChatMessage(reference.key!!, textMessage.first, fromId, toId!!, System.currentTimeMillis() / 1000, textMessage.second)


        //Set the encrypted value in firebase
        reference.setValue(chatMessage)
                .addOnSuccessListener {
                    Toast.makeText(this, "Saved our chat messages: ${reference.key}", Toast.LENGTH_SHORT).show()
                    enter_message_chat_log.text.clear()
                    recycler_view_chat_log.scrollToPosition(adapter.itemCount - 1)
                }

        toReference.setValue(chatMessage)
        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)


    }

    private fun enc(textToEncrypt: String): Pair<String, String> {

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        keyGenerator.init(keyGenParameterSpec)
        val secretKey = keyGenerator.generateKey()

        var iv: ByteArray = ByteArray(0)


        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        iv = cipher.iv

        val cipherText = cipher.doFinal(textToEncrypt.toByteArray()).toHex()
        Log.d("Martin", "iv: ${iv.toHex()}, cipherText: $cipherText")

//        text.text = cipherText
        return Pair(cipherText, iv.toHex())
    }

    private fun dec(encryptedData: ByteArray, encryptedIv: ByteArray): String {

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, encryptedIv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

//        text.text = String(cipher.doFinal(encryptedData))
        return String(cipher.doFinal(encryptedData))
    }
}

//Data Encryption

//    private fun getKey(): SecretKey {
//        val keystore = KeyStore.getInstance("AndroidKeyStore")
//        keystore.load(null)
//
//        val secretKeyEntry = keystore.getEntry("MyKeyAlias", null) as KeyStore.SecretKeyEntry
//        return secretKeyEntry.secretKey
//    }

//    fun encryptData(data: String): Pair<ByteArray, ByteArray> {
//        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
//
//        var temp = data
//        while (temp.toByteArray().size % 16 != 0)
//            temp += "\u0020"
//
//        cipher.init(Cipher.ENCRYPT_MODE, getKey())
//
//        val ivBytes = cipher.iv
//        val encryptedBytes = cipher.doFinal(temp.toByteArray(Charsets.UTF_8))
//
//        return Pair(ivBytes, encryptedBytes)
//    }

//    fun decryptData(ivBytes: ByteArray, data: ByteArray): String{
//        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
//        val spec = IvParameterSpec(ivBytes)
//
//        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
//        return cipher.doFinal(data).toString(Charsets.UTF_8).trim()
//    }

//Data Encryption

//}

class ChatFromItem(val text: String, val user: User) : Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.message_user_from_row.text = text

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.user_profile_image_user_from_row
        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}

class ChatToItem (val text : String, val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.message_user_to_row.text = text

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.user_profile_image_user_to_row
        Picasso.get().load(uri).into(targetImageView)

    }

    override fun getLayout(): Int {
        return R.layout.chat_to_low
    }
}

