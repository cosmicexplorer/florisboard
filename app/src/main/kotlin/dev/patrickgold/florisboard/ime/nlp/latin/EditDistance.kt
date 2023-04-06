/*
 * Copyright (C) 2023 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.nlp.latin

import org.apache.commons.text.similarity.LevenshteinDistance

class EditDistance(private val wordData: Map<String, Int>) {

    fun consult(max_distance: Int?, query: String): List<Pair<String, Int>> {
        val lev = LevenshteinDistance(max_distance)

        return wordData
            // TODO: integrate word frequency!
            .map { (word, _freq) -> Pair(word, lev.apply(query, word)) }
            // Filter out words that breached the max distance limit (returning -1).
            .filter { (_, distance) -> distance >= 0 }
    }
}
