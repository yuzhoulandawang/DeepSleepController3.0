package com.example.deepsleep.root

import com.example.deepsleep.BuildConfig
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootCommander {

    init {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }

    private suspend fun ensureShell(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.getShell()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun requestRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.getShell()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        if (!ensureShell()) return@withContext false
        try {
            val result = Shell.cmd("id").exec()
            if (!result.isSuccess) return@withContext false
            val output = result.out.joinToString("\n")
            if (output.contains("uid=0")) {
                val testResult = Shell.cmd("whoami").exec()
                val whoamiOutput = testResult.out.joinToString("\n")
                return@withContext whoamiOutput.trim() == "root"
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRootInfo(): RootInfo = withContext(Dispatchers.IO) {
        try {
            val idResult = Shell.cmd("id").exec()
            val whoamiResult = Shell.cmd("whoami").exec()
            val suResult = Shell.cmd("su -c 'echo test'").exec()
            RootInfo(
                hasRoot = checkRoot(),
                idOutput = idResult.out.joinToString("\n"),
                whoamiOutput = whoamiResult.out.joinToString("\n"),
                suTestSuccess = suResult.isSuccess,
                errorMessage = if (!checkRoot()) "设备未获取 root 权限或授权被拒绝" else null
            )
        } catch (e: Exception) {
            RootInfo(
                hasRoot = false,
                idOutput = "",
                whoamiOutput = "",
                suTestSuccess = false,
                errorMessage = "检查 root 权限时发生异常: ${e.message}"
            )
        }
    }

    suspend fun exec(command: String): Shell.Result = withContext(Dispatchers.IO) {
        ensureShell()
        Shell.cmd(command).exec()
    }

    suspend fun exec(vararg commands: String): Shell.Result = withContext(Dispatchers.IO) {
        ensureShell()
        Shell.cmd(*commands).exec()
    }

    suspend fun execBatch(commands: List<String>): Shell.Result = withContext(Dispatchers.IO) {
        ensureShell()
        Shell.cmd(*commands.toTypedArray()).exec()
    }

    suspend fun safeWrite(path: String, value: String): Boolean = withContext(Dispatchers.IO) {
        ensureShell()
        val result = Shell.cmd("printf '%s' \"$value\" > $path").exec()
        result.isSuccess
    }

    suspend fun readFile(path: String): String? = withContext(Dispatchers.IO) {
        ensureShell()
        val result = Shell.cmd("cat $path 2>/dev/null").exec()
        if (result.isSuccess) result.out.joinToString("\n") else null
    }

    suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        ensureShell()
        Shell.cmd("[ -f $path ]").exec().isSuccess
    }

    suspend fun mkdir(path: String): Boolean = withContext(Dispatchers.IO) {
        ensureShell()
        Shell.cmd("mkdir -p $path").exec().isSuccess
    }
}

data class RootInfo(
    val hasRoot: Boolean,
    val idOutput: String,
    val whoamiOutput: String,
    val suTestSuccess: Boolean,
    val errorMessage: String? = null
)