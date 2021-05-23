package com.example.messengerapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.messengerapplication.Models.ChatMessage
import com.example.messengerapplication.Models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_messenger.*
import kotlinx.android.synthetic.main.latest_messages_row.view.*
import kotlin.math.PI

class MessengerActivity : AppCompatActivity() {

    companion object {
        var currentUser : User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)

        supportActionBar?.title = "Messenger"


        recycler_view_messenger_activity.adapter = adapter
        recycler_view_messenger_activity.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        adapter.setOnItemClickListener { item, view ->
            val intent = Intent(this, ChatLogActivity::class.java)
            val row = item as LatestMessagesRow
            intent.putExtra(NewMessageActivity.USER_KEY, row.chatPartenerUser )
            startActivity(intent)
        }


//        setupDummyRows()

        listenForLatestMessages()

        fetchCurrentUser()

        verifyUserIsLoggedIn()

    }

    val latestMessagesMap = HashMap<String, ChatMessage>()


    private fun listenForLatestMessages() {

        val fromId  = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId")
        ref.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshRecyclerViewMessages()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun refreshRecyclerViewMessages() {
        adapter.clear()
        latestMessagesMap.values.forEach {
            adapter.add(LatestMessagesRow(it))
        }
    }

    class LatestMessagesRow (val chatMessage: ChatMessage) : Item<ViewHolder>() {

        var chatPartenerUser : User? = null

        override fun bind(viewHolder: ViewHolder, position: Int) {
            viewHolder.itemView.message_latest_message_row.text = chatMessage.text
            val chatPartenerId : String
            if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                chatPartenerId = chatMessage.toId
            }
            else{
                chatPartenerId = chatMessage.fromId
            }

            val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartenerId")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {

                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    chatPartenerUser = snapshot.getValue(User::class.java)
                    viewHolder.itemView.username_latest_messages_row.text = chatPartenerUser?.username

                    val targetImageView = viewHolder.itemView.user_profile_image_latest_messages_row
                    Picasso.get().load(chatPartenerUser?.profileImageUrl).into(targetImageView)

                }
            })

        }

        override fun getLayout(): Int {
            return R.layout.latest_messages_row
        }
    }

    val adapter = GroupAdapter<ViewHolder>()


//    private fun setupDummyRows() {
//        adapter.add(LatestMessagesRow())
//        adapter.add(LatestMessagesRow())
//        adapter.add(LatestMessagesRow())
//        adapter.add(LatestMessagesRow())
//
//    }

    private fun fetchCurrentUser() {

        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
                Log.d("Messenger Activity", "Current User : ${currentUser?.username}")
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun verifyUserIsLoggedIn() {
        val uid = FirebaseAuth.getInstance().uid
        if (uid == null) {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_new_messages -> {
                val intent = Intent(this, NewMessageActivity::class.java)
                startActivity(intent)
            }

            R.id.menu_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}