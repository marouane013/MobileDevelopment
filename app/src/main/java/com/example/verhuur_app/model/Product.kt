package com.example.verhuur_app.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Product(
    @get:Exclude var id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val address: String = "",
    val imageUrl: String = "",
    val userId: String = "",
    val rentalPeriods: List<RentalPeriod> = listOf()
)   