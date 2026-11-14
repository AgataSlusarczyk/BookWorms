package com.bookworms.server.users

import com.bookworms.server.common.ApiException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.imageio.ImageIO

@ConfigurationProperties("bookworms.photos")
data class PhotoProperties(
	// The folder with profile photos. Relative paths start where the server runs (server/ for bootRun).
	val dir: Path,
)

// Serves the photos from bookworms.photos.dir at /photos/<file>. users.photo_url keeps that path.
@Configuration
@EnableConfigurationProperties(PhotoProperties::class)
class PhotoFilesConfig(
	private val properties: PhotoProperties,
) : WebMvcConfigurer {

	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		// The folder must end with "/", or Spring looks for the files next to it.
		registry.addResourceHandler("/photos/**")
			.addResourceLocations(properties.dir.toAbsolutePath().toUri().toString().trimEnd('/') + "/")
	}
}

// Saves and removes the photo files. The database row is UserService's job.
@Component
class PhotoFiles(
	private val properties: PhotoProperties,
) {

	// Reads the picture, makes it at most 512×512 and saves it as a new JPEG. Gives its path, e.g. "/photos/<file>.jpg".
	// Saving it again (instead of keeping the file as sent) drops everything that isn't the picture,
	// like the place where the photo was taken, and makes sure it really is a picture.
	// Every photo gets a new random name, so phones don't keep showing the old one from their cache,
	// and nobody can guess someone's photo from their id.
	fun save(input: InputStream): String {
		val image = ImageIO.read(input)
			?: throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_PHOTO", "This isn't a JPEG or PNG picture")
		val name = "${UUID.randomUUID()}.jpg"
		Files.createDirectories(properties.dir)
		ImageIO.write(smaller(image), "jpg", properties.dir.resolve(name).toFile())
		return "/photos/$name"
	}

	// The old photo, after a new one replaced it or the user removed it. A missing file is fine.
	fun delete(photoUrl: String?) {
		val name = photoUrl?.removePrefix("/photos/") ?: return
		// Only a plain file name from our folder, never a path that leads somewhere else.
		if (name.isEmpty() || name.contains('/') || name.contains('\\') || name.contains("..")) return
		Files.deleteIfExists(properties.dir.resolve(name))
	}

	// JPEG has no see-through parts, so a PNG's transparent background turns white.
	private fun smaller(image: BufferedImage): BufferedImage {
		val scale = minOf(1.0, MAX_SIZE.toDouble() / maxOf(image.width, image.height))
		val width = maxOf(1, (image.width * scale).toInt())
		val height = maxOf(1, (image.height * scale).toInt())
		val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
		val graphics = result.createGraphics()
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
		graphics.color = Color.WHITE
		graphics.fillRect(0, 0, width, height)
		graphics.drawImage(image, 0, 0, width, height, null)
		graphics.dispose()
		return result
	}

	private companion object {
		const val MAX_SIZE = 512
	}
}
