package net.sharplab.tsuji.core.driver.translator.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JekyllFrontMatterUtilTest {

    @Test
    fun `extractFields should extract title and synopsis`() {
        val message = "title: Hello World\nsynopsis: A great guide\ncategory: guides"
        val fields = JekyllFrontMatterUtil.extractFields(message)
        assertThat(fields).containsExactly("Hello World", "A great guide")
    }

    @Test
    fun `extractFields should extract title only when no synopsis`() {
        val message = "title: Hello World\ncategory: guides"
        val fields = JekyllFrontMatterUtil.extractFields(message)
        assertThat(fields).containsExactly("Hello World")
    }

    @Test
    fun `extractFields should extract synopsis only when no title`() {
        val message = "synopsis: A great guide\ncategory: guides"
        val fields = JekyllFrontMatterUtil.extractFields(message)
        assertThat(fields).containsExactly("A great guide")
    }

    @Test
    fun `extractFields should return empty list when no translatable fields`() {
        val message = "category: guides\nlayout: post"
        val fields = JekyllFrontMatterUtil.extractFields(message)
        assertThat(fields).isEmpty()
    }

    @Test
    fun `replaceFields should replace title and synopsis`() {
        val message = "title: Hello World\nsynopsis: A great guide\ncategory: guides"
        val result = JekyllFrontMatterUtil.replaceFields(message, listOf("こんにちは", "素晴らしいガイド"))
        assertThat(result).isEqualTo("title: こんにちは\nsynopsis: 素晴らしいガイド\ncategory: guides")
    }

    @Test
    fun `replaceFields should replace title only`() {
        val message = "title: Hello World\ncategory: guides"
        val result = JekyllFrontMatterUtil.replaceFields(message, listOf("こんにちは"))
        assertThat(result).isEqualTo("title: こんにちは\ncategory: guides")
    }

    @Test
    fun `replaceFields should preserve other fields`() {
        val message = "layout: post\ntitle: Hello\ndate: 2026-01-01\nsynopsis: World"
        val result = JekyllFrontMatterUtil.replaceFields(message, listOf("こんにちは", "世界"))
        assertThat(result).contains("layout: post")
        assertThat(result).contains("date: 2026-01-01")
        assertThat(result).contains("title: こんにちは")
        assertThat(result).contains("synopsis: 世界")
    }
}
