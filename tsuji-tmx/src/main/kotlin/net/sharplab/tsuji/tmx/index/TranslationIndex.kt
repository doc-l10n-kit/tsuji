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
    }

    operator fun get(key: String): String?{
        return index[key] ?: normalizeValue(secondaryIndex[normalizeKey(key)])
    }


}