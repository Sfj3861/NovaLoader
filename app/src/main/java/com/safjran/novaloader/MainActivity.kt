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
            setBackgroundColor(Color.rgb(13, 17, 31))
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        scroll.addView(content)

        val title = TextView(this).apply {
            text = "NovaLoader"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        content.addView(title)

        val subtitle = TextView(this).apply {
            text = "Geometry Dash · Gestor de paquetes · v0.1.0"
            textSize = 14f
            setTextColor(Color.rgb(174, 188, 215))
            setPadding(0, dp(5), 0, dp(18))
        }
        content.addView(subtitle)

        val notice = TextView(this).apply {
            text = "Versión inicial: importa y valida paquetes. Todavía no carga mods dentro del juego."
            textSize = 14f
            setTextColor(Color.rgb(241, 224, 174))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(49, 43, 34), dp(12))
        }
        content.addView(notice, fullWidth())

        val importButton = Button(this).apply {
            text = "Importar .nova / .geode"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(83, 92, 235), dp(12))
            setOnClickListener { choosePackage() }
        }
        val importParams = fullWidth().apply {
            topMargin = dp(18)
            bottomMargin = dp(8)
            height = dp(52)
        }
        content.addView(importButton, importParams)

        val refreshButton = Button(this).apply {
            text = "Actualizar biblioteca"
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
            text = "MI BIBLIOTECA"
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
            text = "Los archivos se guardan en el almacenamiento privado de NovaLoader. No se extraen ni ejecutan."
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
                        " (" + manifest.optString("version", "sin versión") + ")."
                } else {
                    "Archivo .geode guardado sin ejecutar; la compatibilidad de carga está pendiente."
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
                        normalized.split('/').none { it == ".." } &&
                        !entry.name.contains('\\')
                ) { "La ruta de una entrada no es segura." }
                require(names.add(entry.name)) { "El ZIP contiene nombres de entrada duplicados." }

                if (entry.name == "manifest.json" && !entry.isDirectory) {
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

            return json
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
                background = rounded(Color.rgb(25, 31, 51), dp(12))
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
            setPadding(dp(14), dp(12), dp(14), dp(10))
            background = rounded(Color.rgb(25, 31, 51), dp(12))
        }
        val title = TextView(this).apply {
            textSize = 16f
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
                    " · ID: " + manifest.optString("id") + "\n" + file.name
            } catch (_: Exception) {
                title.text = file.name
                details.text = ".nova · No se han podido leer los metadatos"
            }
        } else {
            title.text = file.name
            details.text = ".geode · almacenado; todavía no ejecutable"
        }

        card.addView(title, fullWidth())
        card.addView(details, fullWidth())
        val removeButton = Button(this).apply {
            text = "Eliminar de la biblioteca"
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
        card.addView(removeButton, fullWidth())
        val params = fullWidth().apply { bottomMargin = dp(10) }
        packageList.addView(card, params)
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
        private const val MAX_ARCHIVE_BYTES = 100L * 1024L * 1024L
        private const val MAX_ARCHIVE_ENTRIES = 1000
        private const val MAX_MANIFEST_BYTES = 256 * 1024
        private val ID_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{1,63}$")
    }
}
