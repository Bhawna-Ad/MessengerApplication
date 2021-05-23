package com.example.messengerapplication

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.example.messengerapplication.Models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        login_register_page.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        register_button_register_page.setOnClickListener {
            performRegister()
        }

        if (FirebaseAuth.getInstance().currentUser != null){
            startActivity(Intent(this, MessengerActivity::class.java))
            finish()
        }


        profile_image_register_page.setOnClickListener{
            Toast.makeText(this, "Select profile image to upload", Toast.LENGTH_SHORT).show()

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)

        }



    }

    var selectedPhotoUri : Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==0 && resultCode==Activity.RESULT_OK && data!=null) {
            Toast.makeText(this, "Photo was selected", Toast.LENGTH_SHORT).show()

            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)

            select_photo.setImageBitmap(bitmap)
            profile_image_register_page.alpha = 0f

//            val bitmapDrawable = BitmapDrawable(bitmap)
//            profile_image_register_page.setBackgroundDrawable(bitmapDrawable)
        }
    }

    private fun performRegister() {
        val email = email_register_page.text.toString()
        val password = password_register_page.text.toString()

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Mail/Password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        //Firebase Authentication
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener

                //else
                Toast.makeText(this, "Successfully created user with uid : ${it.result?.user?.uid}", Toast.LENGTH_SHORT).show()

                uploadImageToFirebaseStorage()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create User! Try Again", Toast.LENGTH_SHORT).show()
            }

    }

    private fun uploadImageToFirebaseStorage() {

        if (selectedPhotoUri == null) return

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    Toast.makeText(this, "Successfully uploaded Image!", Toast.LENGTH_SHORT).show()

                    ref.downloadUrl.addOnSuccessListener {
                        saveUserToFirebaseDatabase(it.toString())
                    }
                }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid?:""
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid")

        val user = User(uid, username_register_page.text.toString(), profileImageUrl)
        ref.setValue(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "The user is saved to the Firebase Database", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MessengerActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)

                }
    }
}

//class User(val uid : String, val username : String, val profileImageUrl : String) {
//    constructor() : this("", "", "")
//}