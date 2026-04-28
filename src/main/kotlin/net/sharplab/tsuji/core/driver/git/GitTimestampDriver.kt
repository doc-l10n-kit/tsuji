package net.sharplab.tsuji.core.driver.git

import java.nio.file.Path

/**
 * Driver for retrieving the last-modified timestamp of a file from a remote Git hosting service.
 */
interface GitTimestampDriver {
    /**
     * Gets the timestamp of the last commit that modified the given file.
     * @param filePath Absolute path to the file
     * @param workDir A directory within the git repository (used to resolve remote URL and repo root)
     * @return GitTimestamp with epoch seconds and ISO date string
     */
    fun getTimestamp(filePath: Path, workDir: Path): GitTimestamp
}
