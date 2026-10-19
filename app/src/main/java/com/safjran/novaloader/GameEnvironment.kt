package com.safjran.novaloader

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Copies the installed game's APK set and extracted native libraries into NovaLoader's
 * private workspace. This is staging only: Android cannot execute an APK from this
 * directory, and this class deliberately does not patch, re-sign, or install the game.
 */
internal class GameEnvironment(private val context: Context) {
    private val root = File(context.filesDir, "game-environment")
    private val gameFiles = File(root, "game-files")
    val modsDirectory: File = File(root, "mods")

    data class Result(
        val versionName: String,
        val versionCode: Long,
        val apkCount: Int,
        val nativeLibraryCount: Int,
        val directory: File
    )

    fun prepare(): Result {
        val packageName = TARGET_PACKAGE
        val packageInfo = getPackageInfo(packageName)
        val appInfo = packageInfo.applicationInfo
            ?: throw IllegalStateException("Android no ha proporcionado ApplicationInfo.")
        val apkPaths = (listOfNotNull(appInfo.sourceDir) + appInfo.splitSourceDirs.orEmpty().toList())
            .distinct()
        require(apkPaths.isNotEmpty()) { "Android no ha indicado la ruta del APK instalado." }
        apkPaths.forEach { path ->
            require(File(path).isFile && File(path).canRead()) {
                "No se puede leer el archivo instalado: $path"
            }
        }

        root.mkdirs()
        modsDirectory.mkdirs()
        val staging = File(root, "game-files-staging")
        val backup = File(root, "game-files-backup")
        staging.deleteRecursively()
        backup.deleteRecursively()
        check(staging.mkdirs()) { "No se pudo crear el entorno temporal." }

        try {
            val apkDirectory = File(staging, "apks")
            check(apkDirectory.mkdirs()) { "No se pudo crear la carpeta de APK." }
            val apkRecords = JSONArray()
            apkPaths.forEachIndexed { index, path ->
                val source = File(path)
                val destinationName = if (index == 0) "base.apk" else "split_%02d.apk".format(index)
                val destination = File(apkDirectory, destinationName)
                copyFile(source, destination)
                apkRecords.put(JSONObject()
                    .put("file", "apks/$destinationName")
                    .put("bytes", destination.length())
                    .put("sha256", sha256(destination)))
            }

            val nativeDestination = File(staging, "native-libs")
            val nativeSource = appInfo.nativeLibraryDir?.let(::File)
            val copiedLibraries = if (nativeSource != null && nativeSource.isDirectory) {
                copyDirectory(nativeSource, nativeDestination)
            } else {
                0
            }

            val versionCode = if (Build.VERSION.SDK_INT >= 28) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            val metadata = JSONObject()
                .put("format", "nova-game-environment")
                .put("formatVersion", 1)
                .put("sourcePackage", packageName)
                .put("versionName", packageInfo.versionName ?: "unknown")
                .put("versionCode", versionCode)
                .put("deviceAbis", JSONArray(Build.SUPPORTED_ABIS.toList()))
                .put("apks", apkRecords)
                .put("nativeLibraryDirectoryCopied", nativeSource?.isDirectory == true)
                .put("nativeLibraryCount", copiedLibraries)
                .put("executionStatus", "staged-not-executable")
                .put("note", "APK files and native libraries are copied for inspection. They are not patched or launched from this directory.")
            File(staging, "environment.json").writeText(metadata.toString(2), Charsets.UTF_8)

            if (gameFiles.exists()) {
                check(gameFiles.renameTo(backup)) { "No se pudo apartar la copia anterior." }
            }
            if (!staging.renameTo(gameFiles)) {
                if (backup.exists()) backup.renameTo(gameFiles)
                throw IllegalStateException("No se pudo activar la nueva copia del entorno.")
            }
            backup.deleteRecursively()

            return Result(
                versionName = packageInfo.versionName ?: "desconocida",
                versionCode = versionCode,
                apkCount = apkPaths.size,
                nativeLibraryCount = copiedLibraries,
                directory = gameFiles
            )
        } catch (error: Exception) {
            staging.deleteRecursively()
            if (!gameFiles.exists() && backup.exists()) backup.renameTo(gameFiles)
            throw error
        }
    }

    private fun getPackageInfo(packageName: String) =
        if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        }

    private fun copyDirectory(source: File, destination: File): Int {
        if (!source.isDirectory) return 0
        destination.mkdirs()
        var count = 0
        source.listFiles().orEmpty().forEach { child ->
            val target = File(destination, child.name)
            if (child.isDirectory) count += copyDirectory(child, target)
            else if (child.isFile) {
                copyFile(child, target)
                count++
            }
        }
        return count
    }

    private fun copyFile(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        FileInputStream(source).buffered().use { input ->
            FileOutputStream(destination).buffered().use { output -> input.copyTo(output) }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).buffered().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val TARGET_PACKAGE = "com.robtopx.geometryjump"
    }
}
