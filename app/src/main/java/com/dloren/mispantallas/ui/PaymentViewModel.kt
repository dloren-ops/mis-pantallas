package com.dloren.mispantallas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dloren.mispantallas.data.AppDatabase
import com.dloren.mispantallas.data.Payment
import com.dloren.mispantallas.data.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PaymentViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: PaymentRepository =
        PaymentRepository(AppDatabase.getInstance(app).paymentDao())

    fun paymentsFor(accountId: Long): Flow<List<Payment>> = repository.observeForAccount(accountId)

    fun addPayment(accountId: Long, amount: Double, dateMillis: Long, note: String) {
        viewModelScope.launch {
            repository.save(
                Payment(accountId = accountId, amount = amount, dateMillis = dateMillis, note = note)
            )
        }
    }

    fun delete(payment: Payment) {
        viewModelScope.launch { repository.delete(payment) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as Application
                return PaymentViewModel(app) as T
            }
        }
    }
}
