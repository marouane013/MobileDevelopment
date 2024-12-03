package com.example.verhuur_app.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Product(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val address: String = "",
    val imageUrl: String = "",
    val userId: String = "",
    val rentedBy: String? = null,
    val rentalPeriods: List<RentalPeriod> = listOf(),
    @ServerTimestamp
    val createdAt: Date = Date()
)

data class RentalPeriod(
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val rentedBy: String = "",
    val status: RentalStatus = RentalStatus.PENDING
)

enum class RentalStatus {
    PENDING,
    APPROVED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}   