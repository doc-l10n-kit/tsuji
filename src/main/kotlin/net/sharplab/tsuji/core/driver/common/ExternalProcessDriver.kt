package net.sharplab.tsuji.core.driver.common

import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Driver for executing external processes.
 */
interface ExternalProcessDriver {
    /**
     * Executes an external command.
     * @param command The command and its arguments.
     * @param directory The working directory.
     * @param env Environment variables to add.
     * @param timeoutValue Timeout value.
     * @param timeoutUnit Timeout unit.
     */
    fun execute(
        command: List<String>,
        directory: Path? = null,
        env: Map<String, String> = emptyMap(),
        timeoutValue: Long = 30,
        timeoutUnit: TimeUnit = TimeUnit.MINUTES
    )

    /**
     * Executes an external command and returns its stdout as a string.
     */
    fun executeAndGetOutput(
        command: List<String>,
        directory: Path? = null,
        env: Map<String, String> = emptyMap(),
        timeoutValue: Long = 30,
        timeoutUnit: TimeUnit = TimeUnit.MINUTES
    ): String
}
