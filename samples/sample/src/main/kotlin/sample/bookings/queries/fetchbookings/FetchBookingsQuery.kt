package sample.bookings.queries.fetchbookings

import com.fajrbahr.mediatork.api.Request

data class FetchBookingsQuery(
    val userEmail: String,
    val bookingId: String,
) : Request<String>
