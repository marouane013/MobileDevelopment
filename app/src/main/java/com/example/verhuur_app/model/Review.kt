import java.util.Date

data class Review(
    val id: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val renterId: String = "",
    val renterName: String = "",
    val rating: Float = 0f,
    val review: String = "",
    val createdAt: Date = Date()
) 