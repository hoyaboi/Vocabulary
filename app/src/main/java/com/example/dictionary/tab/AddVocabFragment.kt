package com.example.dictionary.tab

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.Database
import com.example.dictionary.R
import com.example.dictionary.ShowVocabActivity
import com.example.dictionary.Vocab
import com.example.dictionary.tab.adapter.VocabAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class AddVocabFragment : Fragment() {
    private lateinit var vocabNameEditText: TextInputEditText
    private lateinit var vocabCreatorEditText: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var infoText: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: Database

    private lateinit var adapter: VocabAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_vocab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = Database()

        setupViews(view)

        searchButton.setOnClickListener {
            val vocabName = vocabNameEditText.text.toString().trim()
            val vocabCreator = vocabCreatorEditText.text.toString().trim()

            searchVocabs(vocabName, vocabCreator)
        }
    }

    private fun setupViews(view: View) {
        vocabNameEditText = view.findViewById(R.id.vocab_name_input)
        vocabCreatorEditText = view.findViewById(R.id.vocab_creator_input)
        searchButton = view.findViewById(R.id.search_btn)
        recyclerView = view.findViewById(R.id.vocabs_list)
        infoText = view.findViewById(R.id.info_text)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun searchVocabs(vocabName: String, vocabCreator: String) {
        if (vocabName.isNotEmpty()) {
            database.searchVocabs(vocabName, vocabCreator) { vocabs ->
                if (vocabs.isNotEmpty()) {
                    infoText.visibility = View.GONE
                    adapter = VocabAdapter(vocabs, { vocab ->
                        openVocab(vocab)
                    }, {})
                    recyclerView.adapter = adapter
                } else {
                    infoText.text = "검색된 단어장이 없습니다"
                    infoText.visibility = View.VISIBLE
                    recyclerView.adapter = null
                }
            }
        } else {
            Toast.makeText(requireContext(), "단어장 이름을 입력하세요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openVocab(vocab: Vocab) {
        val intent = Intent(requireContext(), ShowVocabActivity::class.java)
        intent.putExtra("vocabId", vocab.id)
        intent.putExtra("vocabName", vocab.name)
        startActivity(intent)
    }
}