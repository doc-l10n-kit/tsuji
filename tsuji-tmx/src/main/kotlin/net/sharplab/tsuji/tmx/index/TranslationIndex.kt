package net.sharplab.tsuji.tmx.index

import net.sharplab.tsuji.tmx.model.Tmx
import org.slf4j.LoggerFactory

class TranslationIndex {


    private val index: Map<String, String>
    private val secondaryIndex: Map<String, String>

    constructor(index: Map<String, String>) {
        this.index = index
        this.secondaryIndex = index.mapKeys { normalizeKey(it.key) }
    }

    companion object{
        private val logger = LoggerFactory.getLogger(TranslationIndex::class.java)
        fun create(tmx: Tmx, target: String): TranslationIndex{
            val index = HashMap<String, String>()
            tmx.tmxBody.translationUnits?.forEach {
                var sourceVariant: String? = null
                var targetVariant: String? = null
                it.variants.forEach { variant ->
                    if(variant.lang == tmx.tmxHeader.srcLang){
                        sourceVariant = variant.seg
                    }
                    if(variant.lang == target){
                        targetVariant = variant.seg
                    }
                }
                if(sourceVariant != null && targetVariant != null){
                    val normalizedKey = normalizeKey(sourceVariant)
                    index[normalizedKey] = targetVariant
                }
            }
            return TranslationIndex(index)
        }

        private fun normalizeKey(key: String): String{
            return key.replace("\n", " ").replace("  ", " ").trimEnd()
        }

        private fun normalizeValue(value: String?): String?{
            return value?.trimEnd()
        }

        /**
         * Aligns the trailing newline of a translation with the key it was looked up by.
         *
         * gettext requires msgid and msgstr to either both end with a newline or both not,
         * and msgfmt rejects the whole PO file otherwise. Neither the lookup nor the stored
         * translation unit preserves that invariant on its own: the normalized lookup path
         * trims the trailing newline off the translation, while the exact-match path returns
         * the stored translation untouched, including a trailing newline the key does not have.
         */
        private fun alignTrailingNewline(value: String?, key: String): String?{
            if(value.isNullOrEmpty()){
                return value
            }
            if(key.endsWith("\n") == value.endsWith("\n")){
                return value
            }
            return if(key.endsWith("\n")) value + "\n" else value.trimEnd('\n')
        }
    }

    operator fun get(key: String): String?{
        val value = index[key] ?: normalizeValue(secondaryIndex[normalizeKey(key)])
        return alignTrailingNewline(value, key)
    }


}
