import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.verhuur_app.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewsAdapter : RecyclerView.Adapter<ReviewsAdapter.ViewHolder>() {
    private var reviews = listOf<Review>()

    fun updateReviews(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount() = reviews.size

    inner class ViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) {
            binding.apply {
                productTitle.text = review.productTitle
                ratingBar.rating = review.rating
                reviewText.text = review.review
                renterName.text = "Door: ${review.renterName}"
                
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                reviewDate.text = dateFormat.format(review.createdAt)
            }
        }
    }
} 