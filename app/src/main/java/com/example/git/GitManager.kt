package com.example.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class GitManager {

    suspend fun isGitRepo(directory: File): Boolean = withContext(Dispatchers.IO) {
        File(directory, ".git").exists()
    }

    suspend fun initRepo(directory: File) = withContext(Dispatchers.IO) {
        Git.init().setDirectory(directory).call().use { }
    }

    suspend fun clone(uri: String, directory: File, token: String) = withContext(Dispatchers.IO) {
        val cloneCommand = Git.cloneRepository()
            .setURI(uri)
            .setDirectory(directory)

        if (token.isNotEmpty()) {
            cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
        }

        cloneCommand.call().use { }
    }

    suspend fun commit(directory: File, message: String) = withContext(Dispatchers.IO) {
        Git.open(directory).use { git ->
            git.add().addFilepattern(".").call()
            git.commit().setMessage(message).call()
        }
    }

    suspend fun push(directory: File, token: String) = withContext(Dispatchers.IO) {
        Git.open(directory).use { git ->
            val pushCommand = git.push()
            if (token.isNotEmpty()) {
                pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
            }
            pushCommand.call()
        }
    }

    suspend fun pull(directory: File, token: String) = withContext(Dispatchers.IO) {
        Git.open(directory).use { git ->
            val pullCommand = git.pull()
            if (token.isNotEmpty()) {
                pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
            }
            pullCommand.call()
        }
    }
    
    suspend fun getStatus(directory: File): String = withContext(Dispatchers.IO) {
        if (!isGitRepo(directory)) return@withContext "Not a Git repository"
        try {
            Git.open(directory).use { git ->
                val status = git.status().call()
                "Uncommitted changes: ${status.hasUncommittedChanges()}\n" +
                "Untracked files: ${status.untracked.size}\n" +
                "Modified files: ${status.modified.size}"
            }
        } catch (e: Exception) {
            "Error reading status: ${e.message}"
        }
    }

    suspend fun addRemote(directory: File, name: String, uri: String) = withContext(Dispatchers.IO) {
        Git.open(directory).use { git ->
            git.remoteAdd().setName(name).setUri(org.eclipse.jgit.transport.URIish(uri)).call()
        }
    }
}
