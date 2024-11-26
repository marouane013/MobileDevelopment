package com.example.verhuur_app.model  // Voeg deze package declaratie toe


import java.util.Date

data class Product(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val address: String = "",
    val imageUrl: String = "",
    val userId: String = "",
    val createdAt: Date = Date()
) 