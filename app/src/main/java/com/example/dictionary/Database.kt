package com.example.dictionary

import android.content.Context
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

data class User(
    val uid: String,
    val name: String
)

data class Word(
    val id: String = "",
    var english: String = "",
    var korean: String = "",
    var checked: Boolean = false
)

class Database {
    private val database = Firebase.database
    private val userReference = database.getReference("users")

    fun saveUser(user: User) {
        userReference.child(user.uid).setValue(user)
    }

    fun saveWord(word: Word, userId: String) {
        val wordId = userReference.child(userId).child("words").push().key ?: ""
        val wordWithId = word.copy(id = wordId)
        userReference.child(userId).child("words").child(wordId).setValue(wordWithId)
    }

    fun updateWord(wordId: String, word: Word, userId: String) {
        userReference.child(userId).child("words").child(wordId).setValue(word)
    }

    fun deleteWord(wordId: String, userId: String) {
        userReference.child(userId).child("words").child(wordId).removeValue()
    }

    fun getWords(userId: String, context: Context, callback: (List<Word>) -> Unit) {
        userReference.child(userId).child("words").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wordList = mutableListOf<Word>()
                for (wordSnapshot in snapshot.children) {
                    val word = wordSnapshot.getValue(Word::class.java)
                    if (word != null) {
                        wordList.add(word)
                    }
                }
                callback(wordList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "단어를 불러오는데 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun updateWordCheckedStatus(wordId: String, userId: String, checked: Boolean) {
        val databaseReference = userReference.child(userId).child("words").child(wordId)

        val updateData = mapOf<String, Any>(
            "checked" to checked
        )

        databaseReference.updateChildren(updateData)
    }
}
