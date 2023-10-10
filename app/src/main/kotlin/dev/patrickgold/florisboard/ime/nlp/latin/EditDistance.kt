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

data class Distance(val dist: Int)

data class Vertex<T>(val key: VertexState<T>)

data class PathId(val id: Int)

data class DirectedEdge<T>(val target: Vertex<T>, val pathId: PathId)

// NB: finding all cycles of length less than some max including the query path (string)!
class WordGraph(wordData: Map<String, Int>) {
    private val edgeList: MutableMap<Vertex<Char>, MutableList<DirectedEdge<Char>>> = mutableMapOf()
    private val pathLookup: MutableMap<PathId, List<Vertex<Char>>> = mutableMapOf()
    private var pathIdCounter: Int = 0

    private fun constructPath(word: String): Pair<PathId, List<Vertex<Char>>> {
        assert(word.isNotBlank())
        val path: MutableList<Vertex<Char>> = mutableListOf()
        val curPath = PathId(pathIdCounter++)
        var source: Vertex<Char> = Vertex(VertexState(State.Start, null))
        for (ch in word) {
            path.add(source)
            edgeList[source] = edgeList.getOrElse(source) { -> mutableListOf() }
            val innerTarget = Vertex(VertexState(State.Inner, ch))
            edgeList[source]!!.add(DirectedEdge(innerTarget, curPath))
            source = innerTarget
        }
        val finalTarget: Vertex<Char> = Vertex(VertexState(State.End, null))
        path.add(finalTarget)
        edgeList[source] = edgeList.getOrElse(source) { -> mutableListOf() }
        edgeList[source]!!.add(DirectedEdge(finalTarget, curPath))

        pathLookup[curPath] = path

        return Pair(curPath, path)
    }

    // Populate graph.
    init {
        // TODO: integrate word frequency!
        for ((word, _freq) in wordData) {
            constructPath(word);
        }
    }

    fun querySimilar(maxDepth: Int, query: String): List<ConsultResult> {
        val (thisPath, path) = constructPath(query)

        // Find all start and end vertices within maxDepth distance!
        val seen: MutableSet<Vertex<Char>> = mutableSetOf()
        val pathsForVertex: MutableMap<Vertex<Char>, MutableList<Pair<PathId, Distance>>> =
            mutableMapOf()
        var queue: List<Vertex<Char>> = path.toList()
        for (curDepth in 0..maxDepth) {
            val curDistance = Distance(curDepth)
            for (v in queue) {
                seen.add(v)
            }
            val newQueue: MutableList<Vertex<Char>> = mutableListOf()
            for (v in queue) {
                edgeList[v] = edgeList.getOrElse(v) { -> mutableListOf() }
                val edges = edgeList[v]!!
                for ((target, curPath) in edges) {
                    pathsForVertex[target] = pathsForVertex.getOrElse(target) { -> mutableListOf() }
                    pathsForVertex[target]!!.add(Pair(curPath, curDistance))
                    newQueue.add(target)
                }
            }
            queue = newQueue.toList()
        }

        val startPathsFound: Map<PathId, Distance> =
            pathsForVertex[Vertex(VertexState(State.Start, null))]!!.toMap()
        val endPathsFound: Map<PathId, Distance> =
            pathsForVertex[Vertex(VertexState(State.End, null))]!!.toMap()
        val fullyFoundPaths: Set<PathId> = startPathsFound.keys.intersect(endPathsFound.keys)
        val ret: MutableList<ConsultResult> = mutableListOf()
        for (pathId in fullyFoundPaths) {
            if (pathId == thisPath) continue

            val startDist: Int = startPathsFound[pathId]!!.dist
            val endDist: Int = endPathsFound[pathId]!!.dist
            val trueDist = maxOf(startDist, endDist)
            assert(trueDist > 0)
            val confidence = (maxDepth - trueDist).toDouble() / maxDepth.toDouble()
            var wordFromPath: String = ""
            for (v in pathLookup[pathId]!!) {
                if (v.key.state == State.Start || v.key.state == State.End) continue
                assert(v.key.state == State.Inner)
                wordFromPath += v.key.key!!
            }
            ret.add(ConsultResult(wordFromPath, confidence))
        }

        return ret
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
