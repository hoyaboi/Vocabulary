package hoya.studio.vocabulary

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import hoya.studio.vocabulary.tab.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var billingClient: BillingClient
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adView: AdView

    private var backPressedTime: Long = 0
    private val database = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 상태표시줄 색상 변경
        window.statusBarColor = ContextCompat.getColor(this, R.color.gray)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setupViews()
        setupBillingClient()
        checkAndRemoveAds()

        // ad 로드
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when(position) {
                0 -> tab.text = "단어장"
                1 -> tab.text = "체크 단어"
                2 -> tab.text = "검색"
                3 -> tab.text = "설정"
            }
        }.attach()

        // 모든 탭의 프래그먼트 한 번에 로드
        viewPager.offscreenPageLimit = 3
    }

    private fun setupViews() {
        adView = findViewById(R.id.ad_view)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = ViewPagerAdapter(this)
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener { billingResult, purchases -> }
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingClient", "Setup finished successfully")
                    updatePurchaseInfo() // 구독 정보 업데이트
                } else {
                    Log.e("BillingClient", "Setup failed with response code: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("BillingClient", "Service disconnected")
            }
        })
    }

    private fun updatePurchaseInfo() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val paymentRef = database.child("users").child(currentUserId).child("payment")

        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchases.isNotEmpty()) {
                    val purchase = purchases.first()
                    val sku = purchase.skus.first()

                    paymentRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                // payment 정보가 없으면 추가
                                paymentRef.child("sku").setValue(sku)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Firebase", "Failed to read payment info: ${error.message}")
                        }
                    })
                } else {
                    // payment 정보가 있으면 제거
                    paymentRef.removeValue()
                }
            } else {
                Log.e("BillingClient", "Query failed with response code: ${billingResult.responseCode}")
            }
        }
    }

    private fun checkAndRemoveAds() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val paymentRef = database.child("users").child(currentUserId).child("payment")
        val rewardRef = database.child("users").child(currentUserId).child("reward").child("time")

        paymentRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sku = snapshot.child("sku").getValue(String::class.java)
                val currentCalendar = Calendar.getInstance()

                when (sku) {
                    "remove_ad_subs" -> removeAds()
                    "remove_ad_annu" -> removeAds()
                    else -> {
                        rewardRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val rewardTime = snapshot.getValue(Long::class.java) ?: 0L
                                val rewardCalendar = Calendar.getInstance().apply {
                                    timeInMillis = rewardTime
                                    add(Calendar.HOUR, 6) // 6시간 추가
//                                    add(Calendar.MINUTE, 1) // 테스트
                                }

                                if (currentCalendar.before(rewardCalendar)) {
                                    removeAds()
                                } else {
                                    showAds()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                showAds()
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showAds()
            }
        })
    }

    private fun removeAds() {
        adView.visibility = View.GONE
    }

    private fun showAds() {
        adView.visibility = View.VISIBLE
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            finishAffinity()
            return
        } else {
            Toast.makeText(this, "단어장을 닫으려면 뒤로 가기 버튼을 한 번 더 누르세요", Toast.LENGTH_SHORT).show()
        }
        backPressedTime = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        setupBillingClient()
        checkAndRemoveAds()
    }
}
