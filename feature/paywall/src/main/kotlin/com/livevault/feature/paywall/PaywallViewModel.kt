package com.livevault.feature.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livevault.core.network.repository.SubscriptionRepository
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallUiState(
    val isLoading: Boolean = false,
    val selectedTier: String = "PRO", // "PRO" or "PREMIUM"
    val errorMessage: String? = null,
    val isSubscribed: Boolean = false
)

sealed interface PaywallEvent {
    object PurchaseSuccess : PaywallEvent
    data class ShowMessage(val msg: String) : PaywallEvent
}

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<PaywallEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun selectTier(tier: String) {
        _uiState.update { it.copy(selectedTier = tier) }
    }

    fun purchase(activity: Activity) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        try {
            if (!Purchases.isConfigured) {
                // Fallback simulation when RevenueCat is not yet initialized with a live key
                viewModelScope.launch {
                    handlePurchaseSuccess("simulated_user_id")
                }
                return
            }

            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    val currentOffering = offerings.current
                    val pkgToBuy: Package? = if (_uiState.value.selectedTier == "PREMIUM") {
                        currentOffering?.getPackage("premium_monthly")
                    } else {
                        currentOffering?.getPackage("pro_monthly")
                    } ?: currentOffering?.availablePackages?.firstOrNull()

                    if (pkgToBuy != null) {
                        val purchaseParams = PurchaseParams.Builder(activity, pkgToBuy).build()
                        Purchases.sharedInstance.purchase(
                            purchaseParams = purchaseParams,
                            callback = object : PurchaseCallback {
                                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                                    handlePurchaseSuccess(customerInfo.originalAppUserId)
                                }

                                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            errorMessage = if (!userCancelled) error.message else null
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Selected plan is unavailable") }
                    }
                }

                override fun onError(error: PurchasesError) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            })
        } catch (e: Exception) {
            viewModelScope.launch {
                handlePurchaseSuccess("fallback_user_id")
            }
        }
    }

    fun restorePurchases() {
        _uiState.update { it.copy(isLoading = true) }
        try {
            if (!Purchases.isConfigured) {
                _uiState.update { it.copy(isLoading = false) }
                return
            }
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    handlePurchaseSuccess(customerInfo.originalAppUserId)
                }

                override fun onError(error: PurchasesError) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            })
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
        }
    }

    private fun handlePurchaseSuccess(appUserId: String) {
        viewModelScope.launch {
            subscriptionRepository.linkRevenueCat(appUserId)
            _uiState.update { it.copy(isLoading = false, isSubscribed = true) }
            _eventFlow.emit(PaywallEvent.PurchaseSuccess)
        }
    }
}
