package com.example.verhuur_app.model

import java.util.Date
import com.google.firebase.firestore.DocumentId

data class Reservation(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val status: String = "pending"
) 