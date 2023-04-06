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

interface QuerySimilar {
    fun consult(query: String): List<ConsultResult>
}

enum class State {
    Start,
    End,
    Inner,
}

data class VertexState<T>(val state: State, val key: T?) {
    init {
        if (key != null) {
            assert(state == State.Inner)
        } else {
            assert(state == State.Start || state == State.End)
        }
    }
}

data class Vertex<T>(val key: VertexState<T>)

data class DirectedEdge<T>(val target: Vertex<T>)

// NB: finding all cycles of length less than some max including the query path (string)!
class WordGraph(wordData: Map<String, Int>) {
    private val edgeList: MutableMap<Vertex<Char>, MutableList<DirectedEdge<Char>>> = mutableMapOf();

    // Populate graph.
    init {
        // TODO: integrate word frequency!
        for ((word, _freq) in wordData) {
            assert(word.isNotBlank())
            var source: Vertex<Char> = Vertex(VertexState(State.Start, null))
            for (ch in word) {
                edgeList[source] = edgeList.getOrElse(source) { -> mutableListOf() }
                val innerTarget = Vertex(VertexState(State.Inner, ch));
                edgeList[source]?.add(DirectedEdge(innerTarget))
                source = innerTarget
            }
            val finalTarget: Vertex<Char> = Vertex(VertexState(State.End, null))
            edgeList[source] = edgeList.getOrElse(source) { -> mutableListOf() }
            edgeList[source]?.add(DirectedEdge(finalTarget))
        }
    }

    fun querySimilar(maxDepth: Int, query: String): List<ConsultResult> {
        val path: MutableList<Vertex<Char>> = mutableListOf(Vertex(VertexState(State.Start, null)))
        for (ch in query) {
            path.add(Vertex(VertexState(State.Inner, ch)))
        }
        path.add(Vertex(VertexState(State.End, null)))

        val seen: MutableSet<Vertex<Char>> = mutableSetOf()
        // TODO: find all start and end vertices within maxDepth distance
    }
}

data class ConsultResult(val entry: String, val confidence: Double)

class EditDistance(wordData: Map<String, Int>): QuerySimilar {
    val graph = WordGraph(wordData)

    override fun consult(query: String): List<ConsultResult> {
        val maxDepth = 5;
        return graph.querySimilar(maxDepth, query)
    }
}
