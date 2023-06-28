package example.imageviewer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.filter.PlatformContext
import example.imageviewer.model.*
import example.imageviewer.model.Pokemon
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.utils.io.core.use
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource


suspend fun loadPicture(url: String): ImageBitmap {
    val httpClient = HttpClient()
    val image: ByteArray = httpClient.use { client ->
        client.get(url).body()
    }
    return image.toImageBitmap()
}

@OptIn(ExperimentalResourceApi::class)
abstract class Dependencies {
    abstract val notification: Notification
    abstract val imageStorage: ImageStorage
    abstract val sharePicture: SharePicture
    val pictures: SnapshotStateList<PictureData> = mutableStateListOf(*resourcePictures)
    open val externalEvents: Flow<ExternalImageViewerEvent> = emptyFlow()
    val localization: Localization = getCurrentLocalization()
    val imageProvider: ImageProvider = object : ImageProvider {
        override suspend fun getImage(picture: PictureData): ImageBitmap = when (picture) {
            is PictureData.Resource -> {
                resource(picture.resource).readBytes().toImageBitmap()
            }

            is PictureData.Camera -> {
                imageStorage.getImage(picture)
            }

            is PictureData.PokemonPictureData -> {
                loadPicture(picture.imageUrl)
            }
        }

        override suspend fun getThumbnail(picture: PictureData): ImageBitmap = when (picture) {
            is PictureData.Resource -> {
                resource(picture.thumbnailResource).readBytes().toImageBitmap()
            }

            is PictureData.Camera -> {
                imageStorage.getThumbnail(picture)
            }

            is PictureData.PokemonPictureData -> {
                loadPicture(picture.imageUrl)
            }
        }

        override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
            imageStorage.saveImage(picture, image)
        }

        override fun delete(picture: PictureData) {
            pictures.remove(picture)
            if (picture is PictureData.Camera) {
                imageStorage.delete(picture)
            }
        }

        override fun edit(picture: PictureData, name: String, description: String): PictureData {
            when (picture) {
                is PictureData.Resource -> {
                    val edited = picture.copy(
                        name = name,
                        description = description,
                    )
                    pictures[pictures.indexOf(picture)] = edited
                    return edited
                }

                is PictureData.Camera -> {
                    val edited = picture.copy(
                        name = name,
                        description = description,
                    )
                    pictures[pictures.indexOf(picture)] = edited
                    imageStorage.rewrite(edited)
                    return edited
                }

                is PictureData.PokemonPictureData -> {
                    val edited = picture.copy(
                        name = name,
                        description = description,
                    )
                    pictures[pictures.indexOf(picture)] = edited
                    return edited
                }
            }
        }

        init {
            println("init Application Dependencies")
            GlobalScope.async {
                try {
                    val pokemons: List<Pokemon> = PokemonService().getAll()
                    val pokemonPictures: List<PictureData.PokemonPictureData> = pokemons.map { pokemon ->
                        PictureData.PokemonPictureData(
                            imageUrl = pokemon.imageUrl,
                            name = pokemon.name,
                            description = "Description",
                            gps = GpsPosition(
                                0.0,
                                0.0
                            ),
                            dateString = "Date"
                        )
                    }
                    pictures.addAll(pokemonPictures)
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("ERROR : " + e)
                }
            }
        }
    }
}

interface Notification {
    fun notifyImageData(picture: PictureData)
}

abstract class PopupNotification(private val localization: Localization) : Notification {
    abstract fun showPopUpMessage(text: String)
    override fun notifyImageData(picture: PictureData) = showPopUpMessage(
        "${localization.picture} ${picture.name}"
    )
}

interface Localization {
    val appName: String
    val back: String
    val picture: String
    val takePhoto: String
    val addPhoto: String
    val kotlinConfName: String
    val kotlinConfDescription: String
    val newPhotoName: String
    val newPhotoDescription: String
}

interface ImageProvider {
    suspend fun getImage(picture: PictureData): ImageBitmap
    suspend fun getThumbnail(picture: PictureData): ImageBitmap
    fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage)
    fun delete(picture: PictureData)
    fun edit(picture: PictureData, name: String, description: String): PictureData
}

interface ImageStorage {
    fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage)
    fun delete(picture: PictureData.Camera)
    fun rewrite(picture: PictureData.Camera)
    suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap
    suspend fun getImage(picture: PictureData.Camera): ImageBitmap
}

interface SharePicture {
    fun share(context: PlatformContext, picture: PictureData)
}

internal val LocalLocalization = staticCompositionLocalOf<Localization> {
    noLocalProvidedFor("LocalLocalization")
}

internal val LocalNotification = staticCompositionLocalOf<Notification> {
    noLocalProvidedFor("LocalNotification")
}

internal val LocalImageProvider = staticCompositionLocalOf<ImageProvider> {
    noLocalProvidedFor("LocalImageProvider")
}

internal val LocalInternalEvents = staticCompositionLocalOf<Flow<ExternalImageViewerEvent>> {
    noLocalProvidedFor("LocalInternalEvents")
}

internal val LocalSharePicture = staticCompositionLocalOf<SharePicture> {
    noLocalProvidedFor("LocalSharePicture")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
