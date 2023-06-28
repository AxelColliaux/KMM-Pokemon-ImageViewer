package example.imageviewer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

@Serializable
data class Pokemon (
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("imageUrl")
    val imageUrl: String,
        )

@Serializable
data class PokemonApiResponse (
    @SerialName("cards")
    val cards: List<Pokemon>
        )

class PokemonService {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getAll(): List<Pokemon> {
        val response: PokemonApiResponse = httpClient.get("https://api.pokemontcg.io/v1/cards").body()
        return response.cards
    }
}



