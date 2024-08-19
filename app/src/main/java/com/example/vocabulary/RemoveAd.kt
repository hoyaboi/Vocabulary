package hoya.studio.vocabulary

import android.content.Context
import android.graphics.Paint
import com.google.android.gms.ads.AdRequest
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import hoya.studio.vocabulary.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RemoveAd : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
    private lateinit var removeAdSubs: LinearLayout
    private lateinit var removeAdAnnual: LinearLayout
    private lateinit var removeAdFree: LinearLayout
    private lateinit var subsDescText: TextView
    private lateinit var annualDescText: TextView
    private lateinit var originPriceText: TextView

    private var skuDetailsList: List<SkuDetails> = emptyList()
    private val database = Firebase.database.reference
    private var rewardedAd: RewardedAd? = null
    private val rewardAdUnitId get() = getString(R.string.reward_ad_unit_id_for_test)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_ad)
        window.statusBarColor = ContextCompat.getColor(this, R.color.gray)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setupViews()
        setupBillingClient()
        checkAndDisplayPurchaseInfo()
        loadRewardedAd()

        removeAdSubs.setOnClickListener {
            if (subsDescText.text.contains("다음 결제일")) {
                showCancelSubscriptionDialog()
            } else {
                initiatePurchase("remove_ad_subs")
            }
        }

        removeAdAnnual.setOnClickListener {
            initiatePurchase("remove_ad_annual")
        }

        removeAdFree.setOnClickListener {
            showRewardedAd()
        }
    }

    private fun setupViews() {
        removeAdSubs = findViewById(R.id.remove_ad_subs)
        removeAdAnnual = findViewById(R.id.remove_ad_annual)
        removeAdFree = findViewById(R.id.remove_ad_free)
        subsDescText = findViewById(R.id.subs_desc_text)
        annualDescText = findViewById(R.id.annual_desc_text)
        originPriceText = findViewById(R.id.annual_origin_text)
        originPriceText.paintFlags = originPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                Toast.makeText(applicationContext, "결제 서비스와의 연결이 끊어졌습니다", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadProducts() {
        val subsSkuList = listOf("remove_ad_subs")
        val subsParams = SkuDetailsParams.newBuilder()
        subsParams.setSkusList(subsSkuList).setType(BillingClient.SkuType.SUBS)

        billingClient.querySkuDetailsAsync(subsParams.build()) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                this.skuDetailsList = this.skuDetailsList + skuDetailsList
            } else {
                Toast.makeText(applicationContext, "상품을 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        }

        val inAppSkuList = listOf("remove_ad_annual")
        val inAppParams = SkuDetailsParams.newBuilder()
        inAppParams.setSkusList(inAppSkuList).setType(BillingClient.SkuType.INAPP)

        billingClient.querySkuDetailsAsync(inAppParams.build()) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                this.skuDetailsList = this.skuDetailsList + skuDetailsList
            } else {
                Toast.makeText(applicationContext, "상품을 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initiatePurchase(productId: String) {
        val skuDetails = skuDetailsList.find { it.sku == productId }
        if (skuDetails != null) {
            val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
            billingClient.launchBillingFlow(this, flowParams)
        } else {
            Toast.makeText(this, "상품을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this, "결제가 취소되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "오류: ${billingResult.debugMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        unlockPremiumFeatures(purchase.skus.first(), purchase.purchaseTime)
                    }
                }
            }
        }
    }

    private fun unlockPremiumFeatures(sku: String, purchaseTime: Long) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        savePurchaseToFirebase(userId, sku, purchaseTime)

        when (sku) {
            "remove_ad_subs" -> {
                Toast.makeText(this, "구독이 시작되었습니다", Toast.LENGTH_SHORT).show()
                updateSubscriptionInfo(purchaseTime)
                disableOtherOptions(true)
            }
            "remove_ad_annual" -> {
                Toast.makeText(this, "연간 결제가 완료되었습니다", Toast.LENGTH_SHORT).show()
                updateAnnualInfo(purchaseTime)
                disableOtherOptions(false)
            }
        }
    }

    private fun savePurchaseToFirebase(userId: String, sku: String, purchaseTime: Long) {
        val paymentRef = database.child("users").child(userId).child("payment")
        val paymentData = mutableMapOf<String, Any>(
            "sku" to sku,
            "purchaseTime" to purchaseTime
        )

        paymentRef.setValue(paymentData)
    }

    private fun checkAndDisplayPurchaseInfo() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val paymentRef = database.child("users").child(userId).child("payment")

        paymentRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sku = snapshot.child("sku").getValue(String::class.java)
                val purchaseTime = snapshot.child("purchaseTime").getValue(Long::class.java) ?: 0L

                when (sku) {
                    "remove_ad_subs" -> {
                        updateSubscriptionInfo(purchaseTime)
                        disableOtherOptions(true)
                    }
                    "remove_ad_annual" -> {
                        updateAnnualInfo(purchaseTime)
                        disableOtherOptions(false)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "결제 정보를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateSubscriptionInfo(purchaseTime: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = purchaseTime
        calendar.add(Calendar.MONTH, 1)

        val nextBillingDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault()).format(calendar.time)
        subsDescText.text = "다음 결제일: $nextBillingDate \n구독 취소를 원하면 해당 상품을 눌러주세요"
    }

    private fun updateAnnualInfo(purchaseTime: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = purchaseTime
        calendar.add(Calendar.YEAR, 1)

        val expirationDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault()).format(calendar.time)
        annualDescText.text = "연간권 만료일: $expirationDate"
    }

    private fun disableOtherOptions(isSubscription: Boolean) {
        if (isSubscription) {
            removeAdAnnual.isEnabled = false
            removeAdFree.isEnabled = false
        } else {
            removeAdSubs.isEnabled = false
            removeAdAnnual.isEnabled = false
            removeAdFree.isEnabled = false
        }
    }

    private fun showCancelSubscriptionDialog() {
        AlertDialog.Builder(this)
            .setTitle("구독 취소")
            .setMessage("정기 구독을 취소하시겠습니까? 취소 시 혜택이 바로 종료됩니다.")
            .setPositiveButton("예") { _, _ ->
                cancelSubscription()
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    private fun cancelSubscription() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val paymentRef = database.child("users").child(userId).child("payment")

        paymentRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                subsDescText.text = "정기 구독이 취소되었습니다"
                removeAdAnnual.isEnabled = true
                removeAdFree.isEnabled = true
                Toast.makeText(this, "구독이 취소되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "구독 취소에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, rewardAdUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
                Toast.makeText(applicationContext, "광고를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }

    private fun showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd() // 광고를 다시 로드
                }
            }

            rewardedAd?.show(this) { rewardItem ->
                // 보상 획득 처리
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@show
                val rewardTime = System.currentTimeMillis()
                saveRewardTimeToFirebase(userId, rewardTime)
                Toast.makeText(this, "하루 동안 광고가 제거됩니다", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "광고가 준비되지 않았습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRewardTimeToFirebase(userId: String, rewardTime: Long) {
        val rewardRef = database.child("users").child(userId).child("reward")
        rewardRef.child("time").setValue(rewardTime)
    }
}
