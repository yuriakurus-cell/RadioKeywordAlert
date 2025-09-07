package com.example.radiokeywordalert

import java.text.Normalizer
import kotlin.math.max

object KeywordMatcher {

    data class Match(val keyword: String, val score: Int)

    private fun normalize(s: String): String {
        val lower = s.lowercase()
        val norm = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return Regex("\p{M}+").replace(norm, "")
            .replace("[^\p{L}\p{Nd}\s]".toRegex(), " ")
            .replace("\s+".toRegex(), " ")
            .trim()
    }

    // Simple token-overlap score (0..100)
    fun bestMatch(transcript: String, keywordsCsv: String): Match? {
        val t = normalize(transcript)
        if (t.isBlank()) return null
        val tTokens = t.split(" ").toSet()

        val keywords = keywordsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var best: Match? = null
        for (kw in keywords) {
            val nkw = normalize(kw)
            val kwTokens = nkw.split(" ").toSet()
            val overlap = tTokens.intersect(kwTokens).size
            val denom = max(kwTokens.size, 1)
            val score = (overlap * 100) / denom
            if (best == null || score > best!!.score) {
                best = Match(kw, score)
            }
        }
        return best
    }
}
