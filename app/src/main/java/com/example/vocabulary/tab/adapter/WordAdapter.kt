package hoya.studio.vocabulary.tab.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hoya.studio.vocabulary.R
import hoya.studio.vocabulary.Word

class WordAdapter(
    private var items: List<Word>,
    private val onCheckChanged: (Boolean) -> Unit,
    private val onAllItemsChecked: (Boolean) -> Unit,
    private val onEditWordClick: (Word) -> Unit, // 단어 수정 클릭 이벤트 처리
    private val isCheckBoxEnabled: Boolean = true
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    private var isEditMode = false // 수정 모드 상태
    private val selectedWords = mutableSetOf<Word>() // 선택된 단어 저장
    private val hiddenEnglishWords = mutableSetOf<String>() // 숨겨진 영어 단어 ID 저장
    private val hiddenKoreanWords = mutableSetOf<String>() // 숨겨진 한국어 단어 ID 저장

    // ViewHolder 클래스 정의
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

        // 모든 아이템에 기본 마진을 설정
        val layoutParams = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = 0

        // 마지막 아이템인지 확인하고, 마진 추가
        if (position == itemCount - 1) {
            layoutParams.bottomMargin = 100.dpToPx(holder.itemView.context)
        }
        holder.itemView.layoutParams = layoutParams

        // 단어 설정 (숨기기 로직에 따라 표시)
        holder.engText.text = if (hiddenEnglishWords.contains(word.id)) "" else word.english
        holder.korText.text = if (hiddenKoreanWords.contains(word.id)) "" else word.korean

        // 체크 상태를 정확하게 반영하도록 강제
        val isChecked = selectedWords.contains(word)
        updateCheckBox(holder.checkBoxImage, isChecked)

        if (isEditMode) {
            // 수정 모드일 때 체크박스 비활성화 및 단어 클릭 시 수정 다이얼로그 호출
            holder.checkBox.isEnabled = false

            holder.engText.setOnClickListener {
                onEditWordClick(word)  // 수정 다이얼로그 호출
            }
            holder.korText.setOnClickListener {
                onEditWordClick(word)  // 수정 다이얼로그 호출
            }
        } else {
            if (isCheckBoxEnabled) {
                // 기본 모드에서 영어 또는 한국어 텍스트 클릭 시 숨기기 또는 보이기
                holder.checkBox.isEnabled = true
                holder.engText.setOnClickListener {
                    if (hiddenEnglishWords.contains(word.id)) {
                        hiddenEnglishWords.remove(word.id)
                        holder.engText.text = word.english
                    } else {
                        hiddenEnglishWords.add(word.id)
                        holder.engText.text = ""
                    }
                }

                holder.korText.setOnClickListener {
                    if (hiddenKoreanWords.contains(word.id)) {
                        hiddenKoreanWords.remove(word.id)
                        holder.korText.text = word.korean
                    } else {
                        hiddenKoreanWords.add(word.id)
                        holder.korText.text = ""
                    }
                }

                // 체크박스 선택 로직
                val isChecked = selectedWords.contains(word)
                updateCheckBox(holder.checkBoxImage, isChecked)

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
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // 체크박스 상태 업데이트
    private fun updateCheckBox(checkBox: ImageView, isChecked: Boolean) {
        checkBox.setImageResource(if (isChecked) R.drawable.checkbox_checked else R.drawable.checkbox_unchecked)
    }

    // 선택된 단어 목록 반환
    fun getSelectedWords(): List<Word> = selectedWords.toList()

    // 선택된 단어 목록 초기화
    fun clearSelection() {
        selectedWords.clear()
        notifyItemRangeChanged(0, itemCount)
    }

    // 영어 텍스트 숨기기
    fun hideEnglishText() {
        hiddenEnglishWords.addAll(items.map { it.id })
        notifyDataSetChanged()
    }

    // 한국어 텍스트 숨기기
    fun hideKoreanText() {
        hiddenKoreanWords.addAll(items.map { it.id })
        notifyDataSetChanged()
    }

    // 숨겨진 단어 초기화
    fun resetWords() {
        hiddenEnglishWords.clear()
        hiddenKoreanWords.clear()
        notifyDataSetChanged()
    }

    // 모든 아이템 선택 또는 선택 해제
    fun selectAllItems(selectAll: Boolean) {
        if (selectAll) {
            selectedWords.addAll(items)
        } else {
            selectedWords.clear()
        }
        notifyDataSetChanged()
        onAllItemsChecked(selectAll)
    }

    // 아이템 섞기
    fun shuffleItems() {
        items = items.shuffled()
        notifyDataSetChanged()
    }

    // 수정 모드 활성화/비활성화
    fun enableEditMode(enable: Boolean) {
        isEditMode = enable

        if (isEditMode) {
            hiddenEnglishWords.clear()
            hiddenKoreanWords.clear()
        }

        notifyDataSetChanged()
    }

    // dp를 px로 변환하는 확장 함수
    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}

