package com.example.files

import java.io.File

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isGitRepo: Boolean,
    val size: Long,
    val lastModified: Long
)

class FileManager {
    fun getFiles(directory: File): List<FileItem> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()
        val children = directory.listFiles() ?: return emptyList()
        return children.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                isGitRepo = if (file.isDirectory) File(file, ".git").exists() else false,
                size = file.length(),
                lastModified = file.lastModified()
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
}
