package net.sharplab.tsuji.core.driver.translator.gemini

data class BatchTranslationRequest(
    val texts: List<String>
)

data class BatchTranslationResponse(
    val translations: List<String>
)
