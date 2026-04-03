package net.sharplab.tsuji.app.cli

import net.sharplab.tsuji.app.exception.TsujiAppException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

abstract class BaseCommand : Runnable {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    final override fun run() {
        try {
            execute()
        } catch (e: TsujiAppException) {
            logger.error("tsuji failed with Error: ${e.message}", e)
            exitProcess(1)
        } catch (e: Exception) {
            logger.error("tsuji failed with Unhandled exception: ${e.message}\n${e.stackTraceToString()}", e)
            exitProcess(1)
        }
    }

    abstract fun execute()
}