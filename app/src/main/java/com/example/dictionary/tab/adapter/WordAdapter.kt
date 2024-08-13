package com.example.dictionary.tab.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.R
import com.example.dictionary.Word
import com.google.android.material.checkbox.MaterialCheckBox

class WordAdapter(
    private val items: List<Word>,
    private val onCheckChanged: (Boolean) -> Unit,
    private val onAllItemsChecked: (Boolean) -> Unit,
    private val isCheckBoxEnabled: Boolean = true
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    private val selectedWords = mutableSetOf<Word>()
    private val hiddenEnglishWords = mutableSetOf<String>()
    private val hiddenKoreanWords = mutableSetOf<String>()

    class WordViewHolder(wordView: View) : RecyclerView.ViewHolder(wordView) {
        val checkBox: FrameLayout = wordView.findViewById(R.id.check_btn)
        val checkBoxImage: ImageView = wordView.findViewById(R.id.check_btn_image)
        val engText: TextView = wordView.findViewById(R.id.eng_text)
        val korText: TextView = wordView.findViewById(R.id.kor_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = items[position]

        holder.engText.text = if (hiddenEnglishWords.contains(word.id)) "" else word.english
        holder.korText.text = if (hiddenKoreanWords.contains(word.id)) "" else word.korean

        val isChecked = selectedWords.contains(word)
        updateCheckBox(holder.checkBoxImage, isChecked)

        holder.checkBox.isEnabled = isCheckBoxEnabled

        if (isCheckBoxEnabled) {
            holder.checkBox.setOnClickListener {
                val currentCheckedState = selectedWords.contains(word)
                if (currentCheckedState) {
                    selectedWords.remove(word)
                } else {
                    selectedWords.add(word)
                }
                updateCheckBox(holder.checkBoxImage, !currentCheckedState)
                onCheckChanged(selectedWords.isNotEmpty())
                onAllItemsChecked(selectedWords.size == items.size)
            }

            holder.engText.setOnClickListener {
                if (holder.engText.text.isEmpty()) {
                    holder.engText.text = word.english
                    hiddenEnglishWords.remove(word.id)
                } else {
                    hiddenEnglishWords.add(word.id)
                    notifyDataSetChanged()
                }
            }

            holder.korText.setOnClickListener {
                if (holder.korText.text.isEmpty()) {
                    holder.korText.text = word.korean
                    hiddenKoreanWords.remove(word.id)
                } else {
                    hiddenKoreanWords.add(word.id)
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun updateCheckBox(checkBox: ImageView, isChecked: Boolean) {
        checkBox.setImageResource(if (isChecked) R.drawable.checkbox_checked else R.drawable.checkbox_unchecked)
    }

    fun getSelectedWords(): List<Word> = selectedWords.toList()

    fun clearSelection() {
        selectedWords.clear()
        notifyDataSetChanged()
        onAllItemsChecked(false)
    }

    fun hideEnglishText() {
        hiddenEnglishWords.addAll(items.map { it.id })
        notifyDataSetChanged()
    }

    fun hideKoreanText() {
        hiddenKoreanWords.addAll(items.map { it.id })
        notifyDataSetChanged()
    }

    fun resetWords() {
        hiddenEnglishWords.clear()
        hiddenKoreanWords.clear()
        notifyDataSetChanged()
    }

    fun selectAllItems(selectAll: Boolean) {
        if (selectAll) {
            selectedWords.addAll(items)
        } else {
            selectedWords.clear()
        }
        notifyDataSetChanged()
        onAllItemsChecked(selectAll)
    }
}

