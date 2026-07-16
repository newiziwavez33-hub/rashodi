package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Category(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

object Categories {
    val expenseCategories = listOf(
        Category("Продукты", Icons.Default.ShoppingCart, Color(0xFF4CAF50)),
        Category("Кафе", Icons.Default.LocalCafe, Color(0xFFFF9800)),
        Category("Транспорт", Icons.Default.DirectionsCar, Color(0xFF2196F3)),
        Category("Развлечения", Icons.Default.SportsEsports, Color(0xFF9C27B0)),
        Category("Покупки", Icons.Default.LocalMall, Color(0xFFE91E63)),
        Category("Здоровье", Icons.Default.MedicalServices, Color(0xFFF44336)),
        Category("Жилье", Icons.Default.Home, Color(0xFF795548)),
        Category("Другое", Icons.Default.Category, Color(0xFF607D8B))
    )

    val incomeCategories = listOf(
        Category("Зарплата", Icons.Default.Payments, Color(0xFF4CAF50)),
        Category("Фриланс", Icons.Default.Work, Color(0xFF00BCD4)),
        Category("Подарки", Icons.Default.CardGiftcard, Color(0xFFFFEB3B)),
        Category("Другое", Icons.Default.Category, Color(0xFF607D8B))
    )

    fun getIconForCategory(name: String, isExpense: Boolean = true): ImageVector {
        val list = if (isExpense) expenseCategories else incomeCategories
        return list.firstOrNull { it.name == name }?.icon ?: Icons.Default.Category
    }

    fun getColorForCategory(name: String, isExpense: Boolean = true): Color {
        val list = if (isExpense) expenseCategories else incomeCategories
        return list.firstOrNull { it.name == name }?.color ?: Color(0xFF607D8B)
    }
}
