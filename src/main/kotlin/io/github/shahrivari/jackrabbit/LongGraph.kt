package io.github.shahrivari.jackrabbit

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import mu.KotlinLogging
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

private val logger = KotlinLogging.logger {}


class LongGraph(edges: Iterator<LongEdge>) : Iterable<LongEdge> {
    val nodes: LongArray get
    private val adjList = Long2ObjectOpenHashMap<LongAdjacency>()
    private val reporter = ProgressReporter(
            logger, 1_000_000)

    init {
        logger.info { "Reading edges..." }
        for (e in edges) {
            for (v in e.nodes)
                adjList.getOrPut(v) { LongAdjacency(v) }.addEdge(e)
            reporter.progress()
        }
        logger.info { "Sorting nodes..." }
        nodes = adjList.keys.toLongArray()
        nodes.sort()
        logger.info { "Optimizing adjacencies..." }
        adjList.values.forEach { it.sortAndTrim() }
        logger.info { "Graph is built: #nodes:${nodes.size} , #edges:${edgeCount()}" }
    }

    constructor(edges: Iterable<LongEdge>) : this(edges.iterator())

    constructor(path: String) : this(Files.lines(Paths.get(path))
                                         .filter { !it.startsWith("#") }
                                         .map { it -> LongEdge.parse(it) }.iterator())

    fun edgeCount() = adjList.values.map { it.ins.size }.sum()


    fun hasEdge(src: Long, dest: Long): Boolean {
        val adj = adjList[src] ?: return false
        return Arrays.binarySearch(adj.outs.elements(), dest) >= 0
    }

    fun adjacency(v: Long): LongAdjacency {
        val adj = adjList[v]
        require(adj != null) { "The node is not present: $v" }
        return adj
    }

    fun neighbors(v: Long): LongArray = adjacency(v).neighs.elements()

    override fun iterator(): Iterator<LongEdge> = EdgeIterator()

    fun saveAsText(path: String) = FileWriter("/tmp/a.txt").use { writer ->
        this.forEach { writer.write("${it.src}\t${it.dst}\n") }
    }

    inner class EdgeIterator : Iterator<LongEdge> {
        val vIter = nodes.iterator()
        var adj = if (vIter.hasNext()) adjList[vIter.next()] else null
        var wIter = if (adj != null) adj!!.outs.iterator() else null
        override fun hasNext(): Boolean {
            if (adj == null)
                return false
            while (!wIter!!.hasNext()) {
                if (!vIter.hasNext())
                    return false
                adj = adjList[vIter.next()]
                wIter = adj!!.outs.iterator()
                continue
            }
            return true
        }

        override fun next(): LongEdge {
            require(hasNext()) { "There is no element next!" }
            return LongEdge(adj!!.node, wIter!!.nextLong())
        }

    }

}
