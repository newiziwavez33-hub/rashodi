package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())
    }

    // UI state filters
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "EXPENSE", "INCOME"
    val selectedCategoryFilter = MutableStateFlow("ALL")
    
    // Month filter: year and month
    val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0-11
    val isAllTimeFilter = MutableStateFlow(false) // Whether to ignore month filter

    // All transactions from DB
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered transactions
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter,
        combine(selectedYear, selectedMonth, isAllTimeFilter) { year, month, allTime -> Triple(year, month, allTime) }
    ) { transactions, query, type, category, dateInfo ->
        val (year, month, allTime) = dateInfo
        transactions.filter { transaction ->
            // Type filter
            val matchesType = when (type) {
                "EXPENSE" -> transaction.type == "EXPENSE"
                "INCOME" -> transaction.type == "INCOME"
                else -> true
            }

            // Category filter
            val matchesCategory = if (category == "ALL") true else transaction.category == category

            // Search query filter
            val matchesQuery = if (query.isBlank()) true else {
                transaction.title.contains(query, ignoreCase = true) ||
                transaction.category.contains(query, ignoreCase = true)
            }

            // Date filter
            val matchesDate = if (allTime) {
                true
            } else {
                val cal = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            }

            matchesType && matchesCategory && matchesQuery && matchesDate
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Financial balance calculations for the filtered transactions
    val totalIncome: StateFlow<Double> = filteredTransactions
        .map { transactions ->
            transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = filteredTransactions
        .map { transactions ->
            transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balance: StateFlow<Double> = filteredTransactions
        .map { transactions ->
            val inc = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val exp = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            inc - exp
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Stats calculations: Category breakdown for expenses of current selection
    val categoryStats: StateFlow<List<CategoryStat>> = filteredTransactions
        .map { transactions ->
            val expenses = transactions.filter { it.type == "EXPENSE" }
            val total = expenses.sumOf { it.amount }
            if (total == 0.0) {
                emptyList()
            } else {
                expenses.groupBy { it.category }
                    .map { (category, list) ->
                        val amount = list.sumOf { it.amount }
                        val percentage = (amount / total * 100.0)
                        CategoryStat(category, amount, percentage)
                    }
                    .sortedByDescending { it.amount }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Income Stats: Category breakdown for income
    val incomeCategoryStats: StateFlow<List<CategoryStat>> = filteredTransactions
        .map { transactions ->
            val incomes = transactions.filter { it.type == "INCOME" }
            val total = incomes.sumOf { it.amount }
            if (total == 0.0) {
                emptyList()
            } else {
                incomes.groupBy { it.category }
                    .map { (category, list) ->
                        val amount = list.sumOf { it.amount }
                        val percentage = (amount / total * 100.0)
                        CategoryStat(category, amount, percentage)
                    }
                    .sortedByDescending { it.amount }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Operations
    fun addTransaction(title: String, amount: Double, type: String, category: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insert(
                TransactionEntity(
                    title = title.ifBlank { category },
                    amount = amount,
                    type = type,
                    category = category,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    // Date navigation helpers
    fun nextMonth() {
        if (isAllTimeFilter.value) {
            isAllTimeFilter.value = false
        } else {
            val currentM = selectedMonth.value
            val currentY = selectedYear.value
            if (currentM == 11) {
                selectedMonth.value = 0
                selectedYear.value = currentY + 1
            } else {
                selectedMonth.value = currentM + 1
            }
        }
    }

    fun prevMonth() {
        if (isAllTimeFilter.value) {
            isAllTimeFilter.value = false
        } else {
            val currentM = selectedMonth.value
            val currentY = selectedYear.value
            if (currentM == 0) {
                selectedMonth.value = 11
                selectedYear.value = currentY - 1
            } else {
                selectedMonth.value = currentM - 1
            }
        }
    }

    fun toggleAllTime() {
        isAllTimeFilter.value = !isAllTimeFilter.value
    }

    // Factory for creating ViewModel
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TransactionViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class CategoryStat(
    val category: String,
    val amount: Double,
    val percentage: Double
)
