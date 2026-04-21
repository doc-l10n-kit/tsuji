package net.sharplab.tsuji.core.driver.translator.util

import net.sharplab.tsuji.po.model.MessageType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MessageTypeNoteGeneratorTest {

    @Test
    fun `generateNote returns configured note for Title1`() {
        val headingNote = "This is a heading"
        val generator = MessageTypeNoteGenerator(headingNote)
        val result = generator.generateNote(MessageType.Title1)
        assertEquals(headingNote, result)
    }

    @Test
    fun `generateNote returns configured note for Title2`() {
        val headingNote = "Use heading style"
        val generator = MessageTypeNoteGenerator(headingNote)
        val result = generator.generateNote(MessageType.Title2)
        assertEquals(headingNote, result)
    }

    @Test
    fun `generateNote returns configured note for Title3`() {
        val headingNote = "体言止めで翻訳"
        val generator = MessageTypeNoteGenerator(headingNote)
        val result = generator.generateNote(MessageType.Title3)
        assertEquals(headingNote, result)
    }

    @Test
    fun `generateNote returns null for Title when headingNote is null`() {
        val generator = MessageTypeNoteGenerator(null)
        val result = generator.generateNote(MessageType.Title1)
        assertNull(result)
    }

    @Test
    fun `generateNote returns null for PlainText`() {
        val generator = MessageTypeNoteGenerator("some note")
        val result = generator.generateNote(MessageType.PlainText)
        assertNull(result)
    }

    @Test
    fun `generateNote returns null for None`() {
        val generator = MessageTypeNoteGenerator("some note")
        val result = generator.generateNote(MessageType.None)
        assertNull(result)
    }

    @Test
    fun `generateNote returns null for DelimitedBlock`() {
        val generator = MessageTypeNoteGenerator("some note")
        val result = generator.generateNote(MessageType.DelimitedBlock)
        assertNull(result)
    }

    @Test
    fun `mergeNotes returns single note when only one is provided`() {
        val result = MessageTypeNoteGenerator.mergeNotes("Note 1")
        assertEquals("Note 1", result)
    }

    @Test
    fun `mergeNotes merges two notes with space`() {
        val result = MessageTypeNoteGenerator.mergeNotes("Note 1", "Note 2")
        assertEquals("Note 1 Note 2", result)
    }

    @Test
    fun `mergeNotes filters out null notes`() {
        val result = MessageTypeNoteGenerator.mergeNotes("Note 1", null, "Note 2")
        assertEquals("Note 1 Note 2", result)
    }

    @Test
    fun `mergeNotes filters out blank notes`() {
        val result = MessageTypeNoteGenerator.mergeNotes("Note 1", "  ", "Note 2")
        assertEquals("Note 1 Note 2", result)
    }

    @Test
    fun `mergeNotes returns null when all notes are null`() {
        val result = MessageTypeNoteGenerator.mergeNotes(null, null)
        assertNull(result)
    }

    @Test
    fun `mergeNotes returns null when all notes are blank`() {
        val result = MessageTypeNoteGenerator.mergeNotes("", "  ")
        assertNull(result)
    }

    @Test
    fun `mergeNotes handles empty varargs`() {
        val result = MessageTypeNoteGenerator.mergeNotes()
        assertNull(result)
    }

    @Test
    fun `mergeNotes merges multiple notes`() {
        val result = MessageTypeNoteGenerator.mergeNotes("Note 1", "Note 2", "Note 3")
        assertEquals("Note 1 Note 2 Note 3", result)
    }
}
