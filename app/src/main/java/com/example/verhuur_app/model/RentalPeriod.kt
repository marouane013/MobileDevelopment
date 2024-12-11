package com.example.verhuur_app.model

import java.util.Date
import com.google.firebase.firestore.DocumentId

data class RentalPeriod(
    @DocumentId
    val id: String = "",
    val productId: String = "",
    val rentedBy: String = "",  // userId van de huurder
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val status: String = "PENDING"  // PENDING, ACTIVE, COMPLETED, CANCELLED
) 