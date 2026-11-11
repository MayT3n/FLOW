package com.flow.focus

import com.flow.focus.data.JsonStore
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonStoreRecoveryTest {
    @Test
    fun corruptStoreIsQuarantinedWithoutOverwritingPreviousRecoveryCopy() {
        val directory = createTempDir(prefix = "matfocus-json-recovery-")
        try {
            val source = File(directory, "flow_data.json")
            val previousBackup = File(directory, "flow_data.json.corrupt-previous")
            source.writeText("{invalid-json", Charsets.UTF_8)
            previousBackup.writeText("older-recovery-copy", Charsets.UTF_8)

            val loaded = JsonStore(source).load()

            assertTrue(loaded.tasks.isEmpty())
            assertFalse("invalid source should be moved out of the live data path", source.exists())
            assertEquals("older-recovery-copy", previousBackup.readText(Charsets.UTF_8))
            val backups = directory.listFiles().orEmpty().filter {
                it.name.startsWith("flow_data.json.corrupt-")
            }
            assertEquals(2, backups.size)
            assertTrue(backups.any { it.readText(Charsets.UTF_8) == "{invalid-json" })
        } finally {
            directory.deleteRecursively()
        }
    }
}
