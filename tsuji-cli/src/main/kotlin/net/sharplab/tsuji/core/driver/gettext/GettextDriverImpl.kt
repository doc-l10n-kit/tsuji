package net.sharplab.tsuji.core.driver.gettext

import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class GettextDriverImpl(private val externalProcessDriver: ExternalProcessDriver) : GettextDriver {

    override fun normalize(path: Path) {
        // Remove obsolete lines starting with #|
        externalProcessDriver.execute(
            command = listOf("sed", "-e", "/^#|/d", "-i", path.toString()),
            timeoutValue = 30,
            timeoutUnit = TimeUnit.SECONDS
        )

        externalProcessDriver.execute(
            command = listOf("msgcat", "--to-code=utf-8", "--no-wrap", "-o", path.toString(), path.toString()),
            timeoutValue = 60,
            timeoutUnit = TimeUnit.SECONDS
        )
    }
}
