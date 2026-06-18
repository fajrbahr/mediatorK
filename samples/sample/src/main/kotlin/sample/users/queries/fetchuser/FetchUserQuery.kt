package sample.users.queries.fetchuser

import com.fajrbahr.mediatork.Request

data class FetchUserQuery(
    val id: String,
    val amount: Double,
) : Request<User>

data class User(
    val name: String,
    val email: String,
)
