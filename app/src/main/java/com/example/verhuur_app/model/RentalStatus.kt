package com.example.verhuur_app.model

enum class RentalStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELLED;

    override fun toString(): String {
        return when (this) {
            PENDING -> "In aanvraag"
            ACTIVE -> "Actief"
            COMPLETED -> "Afgerond"
            CANCELLED -> "Geannuleerd"
        }
    }
} 