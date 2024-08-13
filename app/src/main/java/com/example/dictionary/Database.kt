package com.example.dictionary

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
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
)

data class Vocab(
    val id: String = "",
    val name: String = "",
    val owner: String = "",
    val ownerId: String = "",
    val words: Map<String, Word> = mapOf()
)

class Database {
    private val auth = FirebaseAuth.getInstance()
    private val database = Firebase.database
    private val userReference = database.getReference("users")
    private val vocabReference = database.getReference("vocabs")

    // 유저 정보 저장
    fun saveUser(user: User) {
        userReference.child(user.uid).setValue(user)
    }

    // 단어장 생성
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

    // 단어장에 단어 저장
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

    // 단어장에서 단어 삭제
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

    // 개인 DB에 있는 단어장 목록 불러오기
    fun getVocabsForUser(userId: String, context: Context, callback: (List<Vocab>) -> Unit) {
        userReference.child(userId).child("vocabs").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val vocabList = mutableListOf<Vocab>()
                if (snapshot.exists()) {  // vocabs 노드가 존재하는지 확인
                    val vocabIds = snapshot.children.map { it.key }

                    if (vocabIds.isEmpty()) {
                        callback(vocabList)
                    } else {
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
                } else {
                    // vocabs 노드가 없으면 빈 리스트 반환
                    callback(vocabList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "단어장을 불러오는데 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 단어장에서 단어 불러오기
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
                callback(wordList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "단어를 불러오는데 실패했습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 개인 DB 단어장에서 체크된 단어 불러오기
    fun getCheckedWords(userId: String, context: Context, callback: (List<Word>) -> Unit) {
        val userVocabsRef = userReference.child(userId).child("vocabs")
        val checkedWordIds = mutableSetOf<Pair<String, String>>() // (vocabId, wordId) 페어 리스트
        val checkedWords = mutableSetOf<Word>() // Set으로 중복 제거
        var totalCheckedItems = 0 // 체크된 단어 총 개수

        userVocabsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                checkedWordIds.clear()
                checkedWords.clear()
                totalCheckedItems = 0

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

                totalCheckedItems = checkedWordIds.size

                if (checkedWordIds.isNotEmpty()) {
                    checkedWordIds.forEach { (vocabId, wordId) ->
                        vocabReference.child(vocabId).child("words").child(wordId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(wordSnapshot: DataSnapshot) {
                                    val word = wordSnapshot.getValue(Word::class.java)
                                    if (word != null) {
                                        checkedWords.add(word)
                                    } else {
                                        // 단어가 존재하지 않을 경우, 체크 상태를 제거
                                        userReference.child(userId).child("vocabs")
                                            .child(vocabId).child("checked").child(wordId)
                                            .removeValue()
                                    }

                                    // 모든 단어를 다 가져왔거나 삭제한 경우 callback 호출
                                    if (checkedWords.size + (totalCheckedItems - checkedWords.size) == totalCheckedItems) {
                                        callback(checkedWords.toList())
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(context, "단어를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                } else {
                    callback(checkedWords.toList()) // 체크된 단어가 없을 경우
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "단어장을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // 개인 DB 단어장에 단어 체크
    fun updateWordToChecked(wordId: String, vocabId: String, userId: String) {
        userReference.child(userId).child("vocabs").child(vocabId).child("checked").child(wordId).setValue(true)
    }

    // 개인 DB 단어장에 단어 체크 해제
    fun updateWordToUnchecked(wordId: String, userId: String, callback: () -> Unit) {
        Log.d("UpdateWordToUnchecked", "Updating word with ID: $wordId")

        userReference.child(userId).child("vocabs").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (vocabSnapshot in snapshot.children) {
                    val vocabId = vocabSnapshot.key ?: continue
                    val wordCheckRef = vocabSnapshot.child("checked").child(wordId)
                    if (wordCheckRef.exists()) {
                        userReference.child(userId).child("vocabs").child(vocabId)
                            .child("checked").child(wordId).setValue(false).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    callback()
                                }
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 사용자 vocabs에서 해당 단어장 삭제
    fun deleteVocab(userId: String, vocabId: String, callback: (Boolean) -> Unit) {
        userReference.child(userId).child("vocabs").child(vocabId).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
    }

    // 단어장 검색
    fun searchVocabs(vocabName: String, vocabCreator: String, callback: (List<Vocab>) -> Unit) {
        val query = vocabReference.orderByChild("name")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = mutableListOf<Vocab>()

                for (vocabSnapshot in snapshot.children) {
                    val vocab = vocabSnapshot.getValue(Vocab::class.java) ?: continue

                    val nameMatches = vocab.name.contains(vocabName, ignoreCase = true)
                    val creatorMatches = vocabCreator.isEmpty() || vocab.owner.contains(vocabCreator, ignoreCase = true)
                    val vocabCount = vocab.words.size
                    if (nameMatches && creatorMatches && vocabCount != 0) {
                        result.add(vocab)
                    }
                }

                callback(result)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList()) // 오류 발생 시 빈 리스트 반환
            }
        })
    }

    // 검색한 단어장 추가
    fun addVocabToUser(vocabId: String, userId: String, callback: (Boolean) -> Unit) {
        vocabReference.child(vocabId).child("words").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val wordIds = snapshot.children.map { it.key }
                    val userVocabRef = userReference.child(userId).child("vocabs").child(vocabId).child("checked")

                    val updates = wordIds.associateWith { false }
                    userVocabRef.setValue(updates).addOnCompleteListener { task ->
                        callback(task.isSuccessful)
                    }
                } else {
                    callback(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }

    // 이미 추가된 단어장인지 검사
    fun isVocabAlreadyAdded(vocabId: String, userId: String, callback: (Boolean) -> Unit) {
        userReference.child(userId).child("vocabs").child(vocabId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.exists()) // 단어장이 이미 존재하는지 여부를 콜백으로 전달
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false) // 에러 발생 시에도 false 반환
            }
        })
    }

    // 사용자 이름 가져오기
    fun getUserName(userId: String, callback: (String) -> Unit) {
        userReference.child(userId).child("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.getValue(String::class.java) ?: "Unknown"
                callback(name)
            }

            override fun onCancelled(error: DatabaseError) {
                callback("Unknown")
            }
        })
    }

    // 특정 단어장에서 체크된 단어 개수 가져오기
    fun getCheckedWordsInVocab(userId: String, vocabId: String, callback: (List<Word>) -> Unit) {
        userReference.child(userId).child("vocabs").child(vocabId).child("checked")
            .orderByValue().equalTo(true).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val checkedWords = mutableListOf<Word>()
                    snapshot.children.forEach { wordSnapshot ->
                        val wordId = wordSnapshot.key ?: return@forEach
                        vocabReference.child(vocabId).child("words").child(wordId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(wordSnap: DataSnapshot) {
                                    val word = wordSnap.getValue(Word::class.java)
                                    if (word != null) {
                                        checkedWords.add(word)
                                    }
                                    if (checkedWords.size == snapshot.childrenCount.toInt()) {
                                        callback(checkedWords)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                    if (checkedWords.isEmpty()) {
                        callback(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // 사용자 데이터 삭제
    fun deleteUserData(userId: String, callback: (Boolean) -> Unit) {
        userReference.child(userId).removeValue().addOnCompleteListener { task ->
            callback(task.isSuccessful)
        }
    }

    // 이름 변경 함수
    fun changeUserName(userId: String, newName: String, callback: (Boolean) -> Unit) {
        userReference.child(userId).child("name").setValue(newName)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful)
            }
    }

    // 비밀번호 변경 함수
    fun changeUserPassword(currentPassword: String, newPassword: String, callback: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return callback(false)

        // 현재 사용자의 이메일 가져오기
        val email = user.email ?: return callback(false)

        // 현재 비밀번호로 재인증
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 비밀번호 변경
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    callback(updateTask.isSuccessful)
                }
            } else {
                callback(false)
            }
        }
    }
}
