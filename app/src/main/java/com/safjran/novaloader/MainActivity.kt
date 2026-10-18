package com.safjran.novaloader

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.View
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class MainActivity : Activity() {
    private lateinit var packageList: LinearLayout
    private lateinit var statusText: TextView
    private var pendingExport: File? = null
    private val packageDirectory: File by lazy {
        File(filesDir, "packages").apply { mkdirs() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refreshPackages()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(24, 26, 31))
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        scroll.addView(content)

        val title = TextView(this).apply {
            text = "NovaLoader"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        content.addView(title)

        val subtitle = TextView(this).apply {
            text = "Android · v0.1.1"
            textSize = 14f
            setTextColor(Color.rgb(157, 163, 173))
            setPadding(0, dp(5), 0, dp(18))
        }
        content.addView(subtitle)

        val importButton = Button(this).apply {
            text = "+  Importar paquete"
            isAllCaps = false
            setTextColor(Color.rgb(20, 25, 18))
            background = rounded(Color.rgb(139, 195, 74), dp(5))
            setOnClickListener { choosePackage() }
        }
        val importParams = fullWidth().apply {
            topMargin = dp(14)
            bottomMargin = dp(6)
            height = dp(48)
        }
        content.addView(importButton, importParams)

        val refreshButton = Button(this).apply {
            text = "Actualizar lista"
            isAllCaps = false
            setOnClickListener { refreshPackages() }
        }
        content.addView(refreshButton, fullWidth())

        statusText = TextView(this).apply {
            text = "Listo."
            textSize = 13f
            setTextColor(Color.rgb(173, 225, 199))
            setPadding(0, dp(12), 0, dp(14))
        }
        content.addView(statusText)

        val sectionTitle = TextView(this).apply {
            text = "BIBLIOTECA"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setTextColor(Color.rgb(152, 167, 204))
            setPadding(0, dp(12), 0, dp(10))
        }
        content.addView(sectionTitle)

        packageList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(packageList, fullWidth())

        val footer = TextView(this).apply {
            text = "Los paquetes se guardan de forma privada. No se extraen ni ejecutan."
            textSize = 12f
            setTextColor(Color.rgb(136, 149, 177))
            setPadding(0, dp(24), 0, 0)
        }
        content.addView(footer)

        setContentView(scroll)
    }

    private fun choosePackage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/octet-stream"))
        }
        try {
            startActivityForResult(intent, REQUEST_PICK_PACKAGE)
        } catch (_: Exception) {
            statusText.text = "No se ha podido abrir el selector de archivos."
        }
    }

    @Deprecated("Uses the compatible document-picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EXPORT_PACKAGE) {
            val exportUri = if (resultCode == RESULT_OK) data?.data else null
            val file = pendingExport
            pendingExport = null
            if (exportUri != null && file != null) {
                try {
                    val output = contentResolver.openOutputStream(exportUri)
                        ?: throw IllegalArgumentException("No se pudo abrir el destino de exportación.")
                    file.inputStream().buffered().use { input ->
                        output.buffered().use { target -> input.copyTo(target) }
                    }
                    statusText.text = "Paquete exportado: " + file.name
                } catch (e: Exception) {
                    statusText.text = "No se pudo exportar: " + (e.message ?: "error de escritura")
                }
            }
            return
        }
        if (requestCode != REQUEST_PICK_PACKAGE || resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        try {
            val displayName = queryDisplayName(uri)
            val extension = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            require(extension == "nova" || extension == "geode") {
                "El archivo debe terminar en .nova o .geode."
            }

            val safeName = displayName
                .substringAfterLast('/')
                .replace(Regex("[^A-Za-z0-9._ -]"), "_")
                .take(120)
                .ifBlank { "package." + extension }

            val temporary = File.createTempFile("nova-import-", ".part", packageDirectory)
            try {
                val input = contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("No se pudo leer el archivo.")
                input.use { copyWithLimit(it, temporary, MAX_ARCHIVE_BYTES) }

                val details = if (extension == "nova") {
                    val manifest = validateNovaArchive(temporary)
                    "Validado: " + manifest.optString("name") +
                        " (" + manifest.optString("version", "sin versión") + ")." +
                        manifest.optString("_novaCompatibilityNote").takeIf { it.isNotBlank() }
                            ?.let { "\n" + it }.orEmpty() +
                        "\nGuardado sin ejecutar; NOVA todavía no dispone de runtime dentro del juego."
                } else {
                    val manifest = inspectGeodeArchive(temporary)
                    val modName = manifest.optString("name").ifBlank { manifest.optString("id") }
                    "Mod Geode reconocido: " + modName +
                        " (" + manifest.optString("version", "versión desconocida") + ")." +
                        "\nID: " + manifest.optString("id") +
                        "\nGuardado sin ejecutar; la carga dentro del juego todavía no está implementada."
                }

                val destination = uniqueDestination(safeName)
                if (!temporary.renameTo(destination)) {
                    throw IllegalStateException("No se pudo guardar el paquete.")
                }
                statusText.text = "Importación correcta: " + destination.name + "\n" + details
            } finally {
                if (temporary.exists()) temporary.delete()
            }
            refreshPackages(updateStatus = false)
        } catch (e: Exception) {
            statusText.text = "No se ha importado: " + (e.message ?: "archivo no válido")
        }
    }

    private fun exportPackage(file: File) {
        pendingExport = file
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, file.name)
        }
        try {
            startActivityForResult(intent, REQUEST_EXPORT_PACKAGE)
        } catch (_: Exception) {
            pendingExport = null
            statusText.text = "No se ha podido abrir el destino de exportación."
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        var result = ""
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) result = cursor.getString(index).orEmpty()
            }
        } finally {
            cursor?.close()
        }
        if (result.isBlank()) result = uri.lastPathSegment?.substringAfterLast(':').orEmpty()
        return result.ifBlank { "package.nova" }
    }

    private fun copyWithLimit(input: InputStream, destination: File, maxBytes: Long) {
        destination.outputStream().buffered().use { output ->
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count.toLong()
                require(total <= maxBytes) { "El archivo supera el límite de 100 MB." }
                output.write(buffer, 0, count)
            }
        }
    }

    private fun uniqueDestination(fileName: String): File {
        val first = File(packageDirectory, fileName)
        if (!first.exists()) return first
        val base = fileName.substringBeforeLast('.', fileName)
        val extension = fileName.substringAfterLast('.', "")
        var index = 2
        while (true) {
            val suffix = if (extension.isBlank()) "" else ".$extension"
            val candidate = File(packageDirectory, base + "-" + index + suffix)
            if (!candidate.exists()) return candidate
            index++
        }
    }

    private fun validateNovaArchive(file: File): JSONObject {
        ZipFile(file).use { zip ->
            var entriesSeen = 0
            var expandedBytes = 0L
            var manifestEntry: ZipEntry? = null
            val names = HashSet<String>()
            val entries = zip.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                entriesSeen++
                require(entriesSeen <= MAX_ARCHIVE_ENTRIES) {
                    "El paquete contiene demasiadas entradas."
                }

                val normalized = entry.name.replace('\\', '/')
                require(
                    !normalized.startsWith("/") &&
                        !normalized.startsWith("//") &&
                        !Regex("^[A-Za-z]:").containsMatchIn(normalized) &&
                        normalized.split('/').none { it == ".." || it == "." } &&
                        !entry.name.contains('\\') &&
                        !entry.name.contains('\u0000')
                ) { "La ruta de una entrada no es segura." }
                require(names.add(entry.name)) { "El ZIP contiene nombres de entrada duplicados." }

                require(!entry.name.contains('\u0000')) {
                    "La ruta de una entrada contiene un carácter no válido."
                }
                if (!entry.isDirectory) {
                    val uncompressedSize = entry.size
                    if (uncompressedSize >= 0L) {
                        expandedBytes += uncompressedSize
                        require(expandedBytes <= MAX_EXPANDED_BYTES) {
                            "El contenido expandido supera el límite de 64 MiB."
                        }
                    }
                }

                if (entry.name == "manifest.json" && !entry.isDirectory) {
                    require(manifestEntry == null) {
                        "El ZIP contiene más de un manifest.json en la raíz."
                    }
                    manifestEntry = entry
                }
            }

            val manifest = manifestEntry ?: throw IllegalArgumentException(
                "Falta manifest.json en la raíz del archivo .nova."
            )
            require(manifest.size in 1..MAX_MANIFEST_BYTES.toLong()) {
                "manifest.json está vacío o supera los 256 KB."
            }

            val manifestText = zip.getInputStream(manifest).use {
                String(readLimited(it, MAX_MANIFEST_BYTES), Charsets.UTF_8)
            }
            val json = JSONObject(manifestText)
            require(json.optString("format") == "nova") {
                "El campo format debe ser nova."
            }
            require(json.optInt("formatVersion", -1) == 1) {
                "Versión del formato .nova no compatible."
            }

            val id = json.optString("id")
            require(ID_PATTERN.matches(id)) { "El identificador del paquete no es válido." }
            require(json.optString("name").isNotBlank()) { "Falta el nombre del paquete." }
            require(json.optString("version").isNotBlank()) { "Falta la versión del paquete." }

            // Compatibility is informational only: the archive is still never executed.
            val game = json.optJSONObject("game")
            val versions = game?.optJSONArray("versions")
            val platforms = game?.optJSONArray("platforms")
            val architectures = game?.optJSONArray("architectures")
            val supportedAbis = android.os.Build.SUPPORTED_ABIS.toSet()
            if (architectures != null && architectures.length() > 0) {
                val abiMatch = (0 until architectures.length())
                    .map { architectures.optString(it) }
                    .any { it in supportedAbis }
                require(abiMatch) {
                    "El paquete no declara una arquitectura compatible con este dispositivo."
                }
            }
            if (platforms != null && platforms.length() > 0) {
                val androidTarget = (0 until platforms.length())
                    .map { platforms.optString(it).lowercase(Locale.ROOT) }
                    .any { it == "android" }
                require(androidTarget) { "El paquete no declara Android como plataforma objetivo." }
            }
            if (versions != null && versions.length() > 0) {
                val declaredVersions = (0 until versions.length())
                    .map { versions.optString(it) }
                    .joinToString(", ")
                json.put("_novaCompatibilityNote", "Versiones de Geometry Dash declaradas: " + declaredVersions)
            }

            return json
        }
    }

    /**
     * Reads Geode's package manifest for display and basic archive validation only.
     * This does not load native code, resolve dependencies or execute the mod.
     */
    private fun inspectGeodeArchive(file: File): JSONObject {
        ZipFile(file).use { zip ->
            var entriesSeen = 0
            var expandedBytes = 0L
            var modEntry: ZipEntry? = null
            val names = HashSet<String>()
            val entries = zip.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                entriesSeen++
                require(entriesSeen <= MAX_ARCHIVE_ENTRIES) {
                    "El paquete Geode contiene demasiadas entradas."
                }

                val normalized = entry.name.replace('\\', '/')
                require(
                    normalized.isNotBlank() &&
                        !normalized.startsWith("/") &&
                        !Regex("^[A-Za-z]:").containsMatchIn(normalized) &&
                        normalized.split('/').none { it == ".." || it == "." } &&
                        !entry.name.contains('\\') &&
                        !entry.name.contains('\u0000')
                ) { "El archivo Geode contiene una ruta no segura." }
                require(names.add(entry.name)) { "El ZIP Geode contiene entradas duplicadas." }

                if (!entry.isDirectory && entry.size >= 0L) {
                    expandedBytes += entry.size
                    require(expandedBytes <= MAX_EXPANDED_BYTES) {
                        "El contenido expandido del paquete Geode supera 64 MiB."
                    }
                }
                if (entry.name == "mod.json" && !entry.isDirectory) {
                    require(modEntry == null) { "El paquete Geode contiene más de un mod.json." }
                    modEntry = entry
                }
            }

            val manifest = modEntry ?: throw IllegalArgumentException(
                "Falta mod.json en la raíz; el archivo no parece un paquete Geode válido."
            )
            require(manifest.size in 1..MAX_MANIFEST_BYTES.toLong()) {
                "mod.json está vacío o supera los 256 KB."
            }
            val jsonText = zip.getInputStream(manifest).use {
                String(readLimited(it, MAX_MANIFEST_BYTES), Charsets.UTF_8)
            }
            val json = JSONObject(jsonText)
            require(json.optString("id").isNotBlank()) { "mod.json no contiene un ID válido." }
            require(json.optString("version").isNotBlank()) { "mod.json no contiene una versión." }
            return json
        }
    }

    private fun showPackageDetails(file: File) {
        try {
            val isNova = file.extension.equals("nova", ignoreCase = true)
            val manifest = if (isNova) validateNovaArchive(file) else inspectGeodeArchive(file)
            val id = manifest.optString("id", "desconocido")
            val name = manifest.optString("name").ifBlank { id }
            val version = manifest.optString("version", "desconocida")
            val author = if (isNova) manifest.optString("author") else manifest.optString("developer")
            val description = manifest.optString("description")
            val details = buildString {
                append("Formato: ").append(if (isNova) "NOVA" else "Geode (.geode)")
                append("\nID: ").append(id)
                append("\nVersión: ").append(version)
                if (author.isNotBlank()) append("\nAutor: ").append(author)
                if (description.isNotBlank()) append("\n\n").append(description)
                append("\n\nTamaño: ").append(file.length() / 1024).append(" KiB")
                append("\nEstado: guardado en la biblioteca; no se ejecuta dentro del juego.")
            }
            AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage(details)
                .setPositiveButton("Cerrar", null)
                .setNegativeButton("Eliminar") { _, _ ->
                    AlertDialog.Builder(this)
                        .setTitle("Eliminar paquete")
                        .setMessage("¿Quieres borrar " + file.name + " de la biblioteca?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar") { _, _ ->
                            if (file.delete()) {
                                statusText.text = "Paquete eliminado."
                                refreshPackages(updateStatus = false)
                            } else {
                                statusText.text = "No se ha podido eliminar el archivo."
                            }
                        }
                        .show()
                }
                .show()
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Paquete no válido")
                .setMessage(e.message ?: "No se pudieron leer los metadatos.")
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun readLimited(input: InputStream, maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "manifest.json supera el límite permitido." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun refreshPackages(updateStatus: Boolean = true) {
        packageDirectory.mkdirs()
        packageList.removeAllViews()
        val packages = packageDirectory.listFiles()
            ?.filter {
                it.isFile && (
                    it.extension.equals("nova", ignoreCase = true) ||
                        it.extension.equals("geode", ignoreCase = true)
                    )
            }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) }
            .orEmpty()

        if (packages.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Todavía no hay paquetes. Importa un archivo .nova o .geode para empezar."
                textSize = 14f
                setTextColor(Color.rgb(174, 188, 215))
                setPadding(dp(14), dp(16), dp(14), dp(16))
                background = rounded(Color.rgb(34, 37, 43), dp(4))
            }
            packageList.addView(empty, fullWidth())
        } else {
            for (file in packages) addPackageRow(file)
        }
        if (updateStatus) statusText.text = "Paquetes guardados: " + packages.size
    }

    private fun addPackageRow(file: File) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(8), dp(2), dp(8))
        }
        val title = TextView(this).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val details = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(174, 188, 215))
            setPadding(0, dp(4), 0, dp(8))
        }

        if (file.extension.equals("nova", ignoreCase = true)) {
            try {
                val manifest = validateNovaArchive(file)
                title.text = manifest.optString("name", file.name)
                details.text = ".nova · " + manifest.optString("version", "?") +
                    " · " + manifest.optString("id")
            } catch (_: Exception) {
                title.text = file.name
                details.text = ".nova · No se han podido leer los metadatos"
            }
        } else {
            try {
                val manifest = inspectGeodeArchive(file)
                title.text = manifest.optString("name").ifBlank { manifest.optString("id") }.ifBlank { file.name }
                details.text = ".geode · " + manifest.optString("version", "?") +
                    " · " + manifest.optString("id") + " · almacenado, no ejecutable"
            } catch (_: Exception) {
                title.text = file.name
                details.text = ".geode · archivo no reconocido o mod.json inválido"
            }
        }

        card.setOnClickListener { showPackageDetails(file) }
        card.addView(title, fullWidth())
        card.addView(details, fullWidth())
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val exportButton = Button(this).apply {
            text = "Exportar"
            isAllCaps = false
            setOnClickListener { exportPackage(file) }
        }
        val removeButton = Button(this).apply {
            text = "Eliminar"
            isAllCaps = false
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Eliminar paquete")
                    .setMessage("¿Quieres borrar " + file.name + " de la biblioteca de NovaLoader?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Eliminar") { _, _ ->
                        if (file.delete()) {
                            statusText.text = "Paquete eliminado."
                            refreshPackages(updateStatus = false)
                        } else {
                            statusText.text = "No se ha podido eliminar el archivo."
                        }
                    }
                    .show()
            }
        }
        actions.addView(exportButton, LinearLayout.LayoutParams(0, dp(44), 1f))
        actions.addView(removeButton, LinearLayout.LayoutParams(0, dp(44), 1f))
        card.addView(actions, fullWidth())
        packageList.addView(card, fullWidth())
        val separator = View(this).apply { setBackgroundColor(Color.rgb(55, 59, 66)) }
        packageList.addView(separator, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ))
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun fullWidth(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_PICK_PACKAGE = 8041
        private const val REQUEST_EXPORT_PACKAGE = 8042
        private const val MAX_ARCHIVE_BYTES = 100L * 1024L * 1024L
        private const val MAX_ARCHIVE_ENTRIES = 1000
        private const val MAX_EXPANDED_BYTES = 64L * 1024L * 1024L
        private const val MAX_MANIFEST_BYTES = 256 * 1024
        private val ID_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{1,63}$")
    }
}
