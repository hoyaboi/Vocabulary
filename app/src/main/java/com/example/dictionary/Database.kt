package com.example.dictionary

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

data class User(
    val uid: String,
    val name: String,
    val vocabs: Map<String, Map<String, Boolean>> = mapOf() // 사용자별 단어장 및 체크 상태 관리
)

data class Word(
    val id: String = "",
    var english: String = "",
    var korean: String = "",
    var checked: Boolean = false
)

data class Vocab(
    val id: String = "",
    val name: String = "",
    val owner: String = "",
    val ownerId: String = "",
    val words: Map<String, Word> = mapOf()
)

class Database {
    private val database = Firebase.database
    private val userReference = database.getReference("users")
    private val vocabReference = database.getReference("vocabs")

    fun saveUser(user: User) {
        userReference.child(user.uid).setValue(user)
    }

    fun createVocab(vocabName: String, userId: String, context: Context, callback: () -> Unit) {
        userReference.child(userId).child("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.getValue(String::class.java) ?: "Unknown"
                val vocabId = vocabReference.push().key ?: ""
                val newVocab = Vocab(id = vocabId, name = vocabName, owner = userName, ownerId = userId)

                // 단어장 정보 저장
                vocabReference.child(vocabId).setValue(newVocab)

                // 사용자의 vocabs 목록에 단어장 ID 추가하고 checked 초기화
                val userVocabRef = userReference.child(userId).child("vocabs").child(vocabId)
                val initialCheckedData = mapOf("initial_dummy_word" to false)
                userVocabRef.child("checked").setValue(initialCheckedData) // 기본 체크리스트 초기화

                callback()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "단어장 생성에 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun saveWordToVocab(word: Word, vocabId: String, userId: String, callback: (Boolean) -> Unit) {
        vocabReference.child(vocabId).child("ownerId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ownerId = snapshot.getValue(String::class.java)
                if (ownerId == userId) {
                    val wordId = vocabReference.child(vocabId).child("words").push().key ?: ""
                    val wordData = mapOf(
                        "id" to wordId,
                        "english" to word.english,
                        "korean" to word.korean
                    )
                    vocabReference.child(vocabId).child("words").child(wordId).setValue(wordData)
                    userReference.child(userId).child("vocabs").child(vocabId).child("checked").child(wordId).setValue(false)
                    callback(true)
                } else {
                    callback(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }

    fun deleteWordFromVocab(userId: String, vocabId: String, wordId: String, callback: (Boolean) -> Unit) {
        vocabReference.child(vocabId).child("ownerId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ownerId = snapshot.getValue(String::class.java)
                if (ownerId == userId) {
                    vocabReference.child(vocabId).child("words").child(wordId).removeValue()
                    userReference.child(userId).child("vocabs").child(vocabId).child("checked").child(wordId).removeValue()
                    callback(true)
                } else {
                    callback(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }

    fun getWordsFromVocab(userId: String, vocabId: String, context: Context, callback: (List<Word>) -> Unit) {
        vocabReference.child(vocabId).child("words").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wordList = mutableListOf<Word>()
                for (wordSnapshot in snapshot.children) {
                    val word = wordSnapshot.getValue(Word::class.java)
                    if (word != null) {
                        wordList.add(word)
                    }
                }

                // 사용자별 체크 상태를 반영
                userReference.child(userId).child("vocabs").child(vocabId).child("checkedWords")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            for (word in wordList) {
                                val isChecked = userSnapshot.child(word.id).getValue(Boolean::class.java) ?: false
                                word.checked = isChecked
                            }
                            callback(wordList)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(context, "단어를 불러오는데 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "단어를 불러오는데 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun updateWordCheckedStatus(wordId: String, vocabId: String, userId: String, checked: Boolean) {
        userReference.child(userId).child("vocabs").child(vocabId).child("checked").child(wordId).setValue(checked)
    }

    fun getVocabsForUser(userId: String, context: Context, callback: (List<Vocab>) -> Unit) {
        userReference.child(userId).child("vocabs").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val vocabList = mutableListOf<Vocab>()
                val vocabIds = snapshot.children.map { it.key }

                vocabIds.forEach { vocabId ->
                    vocabReference.child(vocabId!!).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val vocab = snapshot.getValue(Vocab::class.java)
                            if (vocab != null) {
                                vocabList.add(vocab)
                            }
                            if (vocabList.size == vocabIds.size) {
                                callback(vocabList)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(context, "단어장을 불러오는데 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "단어장을 불러오는데 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun updateWordToUnchecked(wordId: String, userId: String) {
        Log.d("UpdateWordToUnchecked", "Updating word with ID: $wordId")

        userReference.child(userId).child("vocabs").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (vocabSnapshot in snapshot.children) {
                    val vocabId = vocabSnapshot.key ?: continue
                    val wordCheckRef = vocabSnapshot.child("checked").child(wordId)
                    if (wordCheckRef.exists()) {
                        userReference.child(userId).child("vocabs").child(vocabId)
                            .child("checked").child(wordId).setValue(false)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun getCheckedWords(userId: String, context: Context, callback: (List<Word>) -> Unit) {
        val userVocabsRef = userReference.child(userId).child("vocabs")
        val checkedWordIds = mutableListOf<Pair<String, String>>() // (vocabId, wordId) 페어 리스트
        val checkedWords = mutableListOf<Word>()

        // 모든 vocabId와 해당 vocab에서 checked가 true인 wordId들을 수집
        userVocabsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { vocabSnapshot ->
                    val vocabId = vocabSnapshot.key ?: return@forEach
                    val checkedRef = vocabSnapshot.child("checked")

                    checkedRef.children.forEach { wordSnapshot ->
                        val wordId = wordSnapshot.key ?: return@forEach
                        val isChecked = wordSnapshot.getValue(Boolean::class.java) ?: false

                        if (isChecked) {
                            checkedWordIds.add(Pair(vocabId, wordId))
                        }
                    }
                }

                // 수집된 wordId에 대해 해당 단어를 vocabs에서 가져옴
                if (checkedWordIds.isNotEmpty()) {
                    checkedWordIds.forEach { (vocabId, wordId) ->
                        vocabReference.child(vocabId).child("words").child(wordId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(wordSnapshot: DataSnapshot) {
                                    val word = wordSnapshot.getValue(Word::class.java)
                                    if (word != null) {
                                        checkedWords.add(word)
                                    }

                                    // 모든 단어를 다 가져왔으면 callback 호출
                                    if (checkedWords.size == checkedWordIds.size) {
                                        callback(checkedWords)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(context, "단어를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                } else {
                    // 체크된 단어가 없을 경우
                    callback(checkedWords)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "단어장을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
