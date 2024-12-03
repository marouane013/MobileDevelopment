package com.example.verhuur_app.model

import java.util.Date

data class RentalRequest(
    val productId: String,
    val productTitle: String,
    val startDate: Date,
    val endDate: Date,
    val renterId: String
) 