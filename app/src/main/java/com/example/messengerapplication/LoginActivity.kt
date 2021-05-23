package com.example.messengerapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        create_account_login_page.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        if (FirebaseAuth.getInstance().currentUser != null){
            startActivity(Intent(this, MessengerActivity::class.java))
            finish()
        }

        login_button_login_page.setOnClickListener {

            val email = email_login_page.text.toString()
            val password = password_login_page.text.toString()

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    Toast.makeText(this, "Successfully LoggedIn!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MessengerActivity::class.java))
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Couldn't Login, Check the email/password and try again!", Toast.LENGTH_SHORT).show()
                    return@addOnFailureListener
                }

        }

    }
}