package net.sharplab.tsuji.core.service

data class PoStats(
    val fuzzyMessages: Int,
    val totalMessages: Int,
    val fuzzyWords: Int,
    val totalWords: Int,
    val achievement: Int // 0-100
)
