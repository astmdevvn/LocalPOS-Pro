package com.localpos.pro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localpos.pro.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartLine(val product: ProductEntity, val quantity: Int)

class PosViewModel(private val repository: PosRepository) : ViewModel() {
    val products = repository.products.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sales = repository.sales.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val debts = repository.debts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cart = MutableStateFlow<List<CartLine>>(emptyList())
    val cart = _cart.asStateFlow()

    init { viewModelScope.launch { repository.seed() } }

    fun addToCart(product: ProductEntity) {
        if (product.stock <= 0) return
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index < 0) current += CartLine(product, 1)
        else if (current[index].quantity < product.stock) current[index] = current[index].copy(quantity = current[index].quantity + 1)
        _cart.value = current
    }

    fun decrease(productId: Long) {
        _cart.value = _cart.value.mapNotNull {
            if (it.product.id != productId) it else if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
        }
    }

    fun checkout(customer: String? = null, done: () -> Unit) = viewModelScope.launch {
        repository.checkout(_cart.value.map { it.product to it.quantity }, customer)
        _cart.value = emptyList()
        done()
    }

    fun addProduct(name: String, barcode: String, price: Long, stock: Int, imageUri: String?) = viewModelScope.launch {
        repository.addProduct(name, barcode, price, stock, imageUri)
    }

    fun deleteProduct(product: ProductEntity) = viewModelScope.launch { repository.deleteProduct(product) }

    fun settle(debt: DebtEntity) = viewModelScope.launch { repository.settle(debt) }

    suspend fun backupJson() = repository.backupJson()
    suspend fun restoreJson(text: String) = repository.restoreJson(text)
    suspend fun importWooCommerce(url: String, key: String, secret: String) = repository.importWooCommerce(url, key, secret)
}
