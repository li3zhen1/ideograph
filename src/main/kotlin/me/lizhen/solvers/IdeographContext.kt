package me.lizhen.solvers

import com.mongodb.reactivestreams.client.ClientSession
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import me.lizhen.schema.*
import me.lizhen.service.*
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.*


@Serializable
data class IdeographSchema(
    val conceptNodes: List<ConceptNode>,
    val propertyNodes: List<PropertyNode>,
    val hasPropertyEdges: List<HasPropertyEdge>,
    val hasRelationConceptEdges: List<HasRelationConceptEdge>,
)

@Serializable
data class PatternSolution(
    val nodes: Map<String, WorkspaceNode>,
    val edges: Map<String, WorkspaceEdge>,
)

@Serializable
data class PatternSolutionResponse(
    val solutions: List<PatternSolution>,
    val elapsedTimeInMillis: Long,
    val message: String?
)

fun PatternSolution.validate(pattern: Pattern): Boolean {

    return nodes.all { (pid, wn) ->

        val pn = pattern.nodes.find {
            it.patternId == pid
        }!!

        val constraints = pattern.constraints?.filter {
            it.targetPatternId == pn.patternId
        }!!
        if(constraints.isEmpty()) return true;

        return@all constraints.validate(wn)
    }

}

/**
 * null:
 *      yet to be evaluated
 */
@Suppress("NOTHING_TO_INLINE")
@Serializable
data class PatternSolutionUnderEvaluation(
    val nodes: List<Pair<String, WorkspaceNode?>>,
    val edges: List<Pair<String, WorkspaceEdge?>>,
) {
    inline val isValid get() = nodes.all { it.second != null } && edges.all { it.second != null }
    inline val isAllEvaluatedDistinct
        get() = nodes.mapNotNull { it.second?.nodeId }.run { distinct().size == size }
                && edges.mapNotNull { it.second?.edgeId }.run { distinct().size == size }

    //    inline fun getNode(patternNode: PatternNode) = nodes.firstOrNull { n -> n.first == patternNode.patternId }
    inline fun getEdge(patternEdge: PatternEdge) = nodes.firstOrNull { n -> n.first == patternEdge.patternId }
    inline fun getEdgeNodeTriple(patternEdge: PatternEdge):
            Triple<Pair<String, WorkspaceNode?>?, Pair<String, WorkspaceNode?>?, Pair<String, WorkspaceNode?>?> {
        val from = nodes.firstOrNull { n -> n.first == patternEdge.fromPatternId }
        val to = nodes.firstOrNull { n -> n.first == patternEdge.toPatternId }
        val e = getEdge(patternEdge)
        return Triple(from, to, e)
    }

    inline fun getNodeIndices(patternEdge: PatternEdge): Pair<Int, Int> {
        val from = nodes.indexOfFirst { n -> n.first == patternEdge.fromPatternId }
        val to = nodes.indexOfFirst { n -> n.first == patternEdge.toPatternId }
        return Pair(from, to)
    }

    inline fun getNodeIndices(patternEdgeId: String): Pair<Int, Int> {
        val from = nodes.indexOfFirst { n -> n.first == patternEdgeId }
        val to = nodes.indexOfFirst { n -> n.first == patternEdgeId }
        return Pair(from, to)
    }

    inline fun checkEdge(patternEdge: PatternEdge) = checkTripleEvaluable(getEdgeNodeTriple(patternEdge))

    inline fun checkTripleEvaluable(
        triple:
        Triple<Pair<String, WorkspaceNode?>?,
                Pair<String, WorkspaceNode?>?,
                Pair<String, WorkspaceNode?>?>
    ): Boolean {
        val (from, to, e) = triple //getEdgeNodeTriple(patternEdge)
        if (from == null || to == null || e == null) {
            throw Error("Illegal edge id.")
        }
        return e.second == null && (from.second == null && to.second != null)
                || (from.second != null && to.second == null)
    }

    inline fun getUpdatedEdges(
        patternEdge: PatternEdge,
        workspaceEdge: WorkspaceEdge
    ): List<Pair<String, WorkspaceEdge?>> {
        val index = edges.indexOfFirst { e -> e.first == patternEdge.patternId }
        return edges.toMutableList().apply {
            set(index, Pair(patternEdge.patternId, workspaceEdge))
        }.toList()
    }

    inline fun getUpdatedNodes(
        patternNode: PatternNode,
        workspaceNode: WorkspaceNode
    ): List<Pair<String, WorkspaceNode?>> {
        val index = nodes.indexOfFirst { e -> e.first == patternNode.patternId }
        return nodes.toMutableList().apply {
            set(index, Pair(patternNode.patternId, workspaceNode))
        }
    }

    inline fun getUpdatedNodes(
        maps: List<Pair<String, WorkspaceNode?>>
    ): List<Pair<String, WorkspaceNode?>> {
        return nodes.toMap().toMutableMap().apply {
            maps.forEach { (patternId, value) ->
                this[patternId] = value
            }
        }.toList()
    }

    inline fun completed(): PatternSolution? {
        if (isValid) {
            return PatternSolution(
                nodes.associate { it.first to it.second!! },
                edges.associate { it.first to it.second!! },
            )
                //.also { println(it) }
        }
        return null
    }

    inline fun uniqKey(): String = nodes.joinToString { it.second?.nodeId.toString() }


    companion object {
//        const val EDGE_EVALUATED = -1
//        const val EDGE_FROM_EVALUABLE = 0
//        const val EDGE_TO_EVALUABLE = 1
    }
}


fun PatternSolution.uniqKey(): String = nodes.toList().joinToString("") {
    it.second.nodeId.toString()
} + edges.toList().joinToString("") {
    it.second.edgeId.toString()
}

/**
 * assuming is paired
 */
fun List<PatternConstraint>?.validate(workspaceNode: WorkspaceNode): Boolean {
    if (this == null) return true
    return this.all {
        workspaceNode.properties[it.property].orEmpty().matches(Regex(it.value))
    }
}


@Suppress("CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION")
class IdeographContext(
    val mongoService: MongoService,
    asyncInitialize: Boolean = true
//    val dgraphService: DgraphService
) {
    init {
        if (asyncInitialize)
            initializeSchemaAsync()
        else initializeSchema()
    }


    lateinit var conceptNodes: List<ConceptNode>
    lateinit var conceptTypeDict: Map<String?, ConceptNode>
    lateinit var relationNodes: List<RelationNode>
    lateinit var propertyNodes: List<PropertyNode>
    lateinit var hasRelationConceptEdges: List<HasRelationConceptEdge>
    lateinit var relationConceptDict: Map<Long, HasRelationConceptEdge>
    lateinit var relationConceptFromDict: Map<Long, List<HasRelationConceptEdge>>
    lateinit var hasPropertyEdges: List<HasPropertyEdge>
    lateinit var propertyFromDict: Map<Long, List<HasPropertyEdge>>

    @Deprecated("Use solvePatternBatched instead.")
    suspend fun solvePattern(pattern: Pattern): List<PatternSolution> {
        val patternNodeDict = pattern.nodes.associateBy { it.patternId }
        val patternEdgeDict = pattern.edges.orEmpty().associateBy { it.patternId }
        val nodeConstraintPair = pattern.nodes.associate {
            Pair(
                it.patternId, pattern.constraints?.filter { pc -> pc.targetPatternId == it.patternId }.orEmpty()
            )
        }

        val patternNodeCandidateCounts = nodeConstraintPair.mapValues {
            patternNodeDict[it.key]?.let { n ->
                runBlocking { countConstrainedNodes(n, *it.value.toTypedArray()) }
            } ?: Long.MAX_VALUE
        }

        val patternNodeQueues = patternNodeCandidateCounts.entries.sortedBy { it.value }

        if (patternNodeQueues[0].value >= MONGO_NODE_LIMIT) {
            throw Error("Pattern node candidates limit exceeded: $patternNodeQueues")
        }

        val solutionPool = mutableListOf(
            PatternSolutionUnderEvaluation(
                nodes = pattern.nodes.map { it.patternId to null },
                edges = pattern.edges.orEmpty().map { it.patternId to null }
            )
        )

//        var batchIndices = listOf(0)


        while (true) {
            if (solutionPool.isEmpty()) break
            val solutionToSolve = solutionPool.removeAt(0)
            if (solutionToSolve.isValid) // all solutions are evaluated
                break

            // find target to evaluate

            val patternNodeToEvaluate = solutionToSolve.nodes.firstOrNull {
                it.second == null && patternNodeCandidateCounts[it.first]!! < EVALUABLE_CANDIDATE_LIMIT
            }

            if (patternNodeToEvaluate != null) {
                nodeConstraintPair[patternNodeToEvaluate.first]?.let {
                    val nCandidates = queryNodeWithConstraints(
                        patternNodeDict[patternNodeToEvaluate.first]!!,
                        *it.toTypedArray()
                    )
                    val filledSolutions = nCandidates.map { wn ->
                        val newNodes = solutionToSolve.nodes.toMutableList()
                        val index = newNodes.indexOfFirst { n -> n.first == patternNodeToEvaluate.first }
                        newNodes[index] = patternNodeToEvaluate.first to wn
                        solutionToSolve.copy(nodes = newNodes)
                    }
                    solutionPool += filledSolutions
                }
            } else {
                solutionToSolve.edges.firstOrNull { (patternId, candidate) ->
                    val patternEdge = patternEdgeDict[patternId]!!

                    if (candidate != null) return@firstOrNull false
                    val (fromIndex, toIndex) = solutionToSolve.getNodeIndices(patternEdge)
                    val fromSol = solutionToSolve.nodes[fromIndex]
                    val toSol = solutionToSolve.nodes[toIndex]

                    if ((fromSol.second != null) && (toSol.second == null)) {
                        val eCandidates = queryEdges(patternEdge.type, nodeFrom = listOf(fromSol.second!!))
                        if (eCandidates.isEmpty()) return@firstOrNull false
                        else {
                            val toPattern = patternNodeDict[patternEdge.toPatternId]!!
                            val toTypeName = toPattern.type
                            val toCandidates = queryNodeWithConstraints(
                                toTypeName,
                                eCandidates.map { e -> e.toId }
                            )
                            val filteredToCandidates = toCandidates.filter {
                                nodeConstraintPair[toPattern.patternId].validate(it)
                            }
                            val filledSolutions = filteredToCandidates.map {
                                val correspondingEdge = eCandidates.firstOrNull { e -> e.toId == it.nodeId }!!
                                return@map solutionToSolve.copy(
                                    nodes = solutionToSolve.getUpdatedNodes(toPattern, it),
                                    edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
                                )
                            }
                            solutionPool += filledSolutions
                            return@firstOrNull true
                        }
                    } else if ((fromSol.second == null) && (toSol.second != null)) {
                        val eCandidates = queryEdges(patternEdge.type, nodeTo = listOf(toSol.second!!))
                        if (eCandidates.isEmpty()) return@firstOrNull false
                        else {
                            val fromPattern = patternNodeDict[patternEdge.fromPatternId]!!
                            val fromTypeName = fromPattern.type
                            val fromCandidates = queryNodeWithConstraints(
                                fromTypeName,
                                eCandidates.map { e -> e.fromId }
                            )
                            val filteredFromCandidates = fromCandidates.filter {
                                nodeConstraintPair[fromPattern.patternId].validate(it)
                            }
                            val filledSolutions = filteredFromCandidates.map {
                                val correspondingEdge = eCandidates.firstOrNull { e -> e.fromId == it.nodeId }!!
                                return@map solutionToSolve.copy(
                                    nodes = solutionToSolve.getUpdatedNodes(fromPattern, it),
                                    edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
                                )
                            }
                            solutionPool += filledSolutions
                            return@firstOrNull true
                        }
                    } else if (fromSol.second != null) {
                        val eCandidates = queryEdges(
                            patternEdge.type,
                            nodeFrom = listOf(fromSol.second!!),
                            nodeTo = listOf(toSol.second!!)
                        )
                        if (eCandidates.isEmpty()) return@firstOrNull false
                        else {
                            val filledSolutions = eCandidates.map { ec ->
                                return@map solutionToSolve.copy(
                                    edges = solutionToSolve.getUpdatedEdges(patternEdge, ec)
                                )
                            }
                            solutionPool += filledSolutions
                            return@firstOrNull true
                        }
                    } else return@firstOrNull false

                }
            }

        }

        return solutionPool.mapNotNull { it.completed() }
//        patternNodeQueues.forEach { entry ->
//            val patternNode = patternNodeDict[entry.key]
//            val targetingConstraints = nodeConstraintPair[entry.key]
//
//            if (entry.value <= EVALUABLE_CANDIDATE_LIMIT) {
//
//            }
//        }
//
//
//        val nodeCandidateLists = nodeConstraintPair.mapValues {
//            patternNodeDict[it.key]?.let { n ->
//                queryNodeWithConstraints(n, *it.value.toTypedArray())
//            }
//        }
//
//        nodeCandidateLists.forEach {
//            println(it.value?.size)
//        }
//
//        val nodesPool = nodeCandidateLists.flatMap { it.value.orEmpty() }.associateBy { it.nodeId }
//
//        /**
//         * assuming only one
//         */
//        val edgeCandidateLists = pattern.edges?.map { pe ->
//            val fromCandidates = nodeCandidateLists[pe.fromPatternId]
//            val toCandidates = nodeCandidateLists[pe.toPatternId]
//            if (fromCandidates.isNullOrEmpty() || toCandidates.isNullOrEmpty())
//                return@map emptyList()
//
//            val edgeTypeCandidates = patternNodeDict[pe.fromPatternId]?.let { from ->
//                patternNodeDict[pe.toPatternId]?.let { to ->
//                    getEdgeTypeCandidates(from, to)
//                }
//            }
//
//            println(edgeTypeCandidates?.map { it.name })
//
//            edgeTypeCandidates?.flatMap {
//                if (toCandidates.size >= MONGO_NODE_LIMIT) {
//                    return@flatMap queryEdges(it.name, fromCandidates)
//                } else {
//
//                    return@flatMap queryEdges(it.name, fromCandidates, toCandidates)
//                }
//            }.orEmpty()
//        }
//
//        if (edgeCandidateLists?.size != 1) {
//            return emptyList()
//        }
//
//
//        val solutions = edgeCandidateLists[0].mapNotNull { edge ->
//            val fromNode = nodesPool[edge.fromId]
//            val toNode = nodesPool[edge.toId]
//            if (fromNode == null || toNode == null) return@mapNotNull null
//            PatternSolution(
//                nodes = listOf(fromNode, toNode),
//                edges = listOf(edge)
//            )
//        }
//
//        return solutions
    }

    suspend fun countConstrainedNodes(node: PatternNode, vararg constraints: PatternConstraint): Long {
        return mongoService
            .getCollection<WorkspaceNode>(node.type + "_node")
            .countDocuments(
                and(
                    constraints.map {
                        WorkspaceNode::properties.keyProjection(it.property) regex it.value
                    }
                )
            )
    }

    suspend fun countConstrainedNodes(
        session: ClientSession,
        node: PatternNode,
        vararg constraints: PatternConstraint
    ): Long = mongoService
        .getCollection<WorkspaceNode>(node.type + "_node")
        .countDocuments(
            clientSession = session,
            filter = and(
                constraints.map {
                    WorkspaceNode::properties.keyProjection(it.property) regex it.value
                }
            )
        )

    suspend fun queryNodeWithConstraints(
        node: PatternNode,
        vararg constraints: PatternConstraint
    ): List<WorkspaceNode> {
        // WorkspaceNode::properties.keyProjection("呼叫人*") regex "李.+",
        // WorkspaceNode::properties.keyProjection("联系电话") regex "1569755338[0-9]+",
        val find = mongoService
            .getCollection<WorkspaceNode>(node.type + "_node")
            .find(
                and(
                    constraints.map {
                        WorkspaceNode::properties.keyProjection(it.property) regex it.value
                    }
                )
            )
            .batchSize(BATCH_SIZE)
//        if (find.count() > MONGO_NODE_LIMIT) throw Error("Query maximum exceeded.")
        return find.toList()
    }


    suspend fun queryNodeWithConstraints(
        session: ClientSession,
        node: PatternNode,
        vararg constraints: PatternConstraint
    ) = mongoService
        .getCollection<WorkspaceNode>(node.type + "_node")
        .find(
            session,
            and(
                constraints.map {
                    WorkspaceNode::properties.keyProjection(it.property) regex it.value
                }
            )
        )
        .batchSize(BATCH_SIZE)
    

    suspend fun queryNodeWithConstraints(
        nodeTypeName: String,
        nodeIds: List<Long>,
        vararg constraints: PatternConstraint
    ): List<WorkspaceNode> {
        val find = mongoService
            .getCollection<WorkspaceNode>(nodeTypeName + "_node")
            .find(
                WorkspaceNode::nodeId `in` nodeIds,
                and(
                    constraints.map {
                        WorkspaceNode::properties.keyProjection(it.property) regex it.value
                    }
                )
            )
            .batchSize(BATCH_SIZE)
//        if (find.count() > MONGO_NODE_LIMIT) throw Error("Query maximum exceeded.")
        return find.toList()
    }

    suspend fun queryNodeWithConstraints(
        clientSession: ClientSession,
        nodeTypeName: String,
        nodeIds: List<Long>,
        vararg constraints: PatternConstraint,
//        additionalConstraints: Bson? = null
    ) = mongoService
        .getCollection<WorkspaceNode>(nodeTypeName + "_node")
        .find(
            clientSession,
            and(
                constraints.map {
                    WorkspaceNode::properties.keyProjection(it.property) regex it.value
                } + (WorkspaceNode::nodeId `in` nodeIds) //+ additionalConstraints
            )
        )
        .batchSize(BATCH_SIZE)


    private suspend fun queryEdges(
        edgeTypeName: String,
        nodeFrom: List<WorkspaceNode>? = null,
        nodeTo: List<WorkspaceNode>? = null
    ): List<WorkspaceEdge> = queryEdgesByNodeId(
        edgeTypeName,
        nodeFrom?.map { it.nodeId },
        nodeTo?.map { it.nodeId }
    )


    fun queryEdges(
        clientSession: ClientSession,
        edgeTypeName: String,
        nodeFrom: List<WorkspaceNode>? = null,
        nodeTo: List<WorkspaceNode>? = null
    ): CoroutineFindPublisher<WorkspaceEdge> = queryEdgesByNodeId(
        clientSession,
        edgeTypeName,
        nodeFrom?.map { it.nodeId },
        nodeTo?.map { it.nodeId }
    )

    private suspend fun queryEdgesByNodeId(
        edgeTypeName: String,
        nodeFrom: List<Long>? = null,
        nodeTo: List<Long>? = null
    ): List<WorkspaceEdge> {

        val fromIds = nodeFrom?.run {
            if (nodeFrom.size == 1)
                WorkspaceEdge::fromId eq nodeFrom[0]
            else
                WorkspaceEdge::fromId `in` this
        }
        val toIds = nodeTo?.run {
            if (nodeTo.size == 1)
                WorkspaceEdge::toId eq nodeTo[0]
            else
                WorkspaceEdge::toId `in` this
        }

        val filters = listOfNotNull(fromIds, toIds)

        return mongoService
            .database
            .getCollection<WorkspaceEdge>(edgeTypeName + "_edge")
            .find(and(filters))

            .batchSize(BATCH_SIZE)
            .toList()
    }


    private fun queryEdgesByNodeId(
        clientSession: ClientSession,
        edgeTypeName: String,
        nodeFrom: List<Long>? = null,
        nodeTo: List<Long>? = null
    ): CoroutineFindPublisher<WorkspaceEdge> {

        val fromIds = nodeFrom?.run {
            if (nodeFrom.size == 1)
                WorkspaceEdge::fromId eq nodeFrom[0]
            else
                WorkspaceEdge::fromId `in` this
        }
        val toIds = nodeTo?.run {
            if (nodeTo.size == 1)
                WorkspaceEdge::toId eq nodeTo[0]
            else
                WorkspaceEdge::toId `in` this
        }

        val filters = listOfNotNull(fromIds, toIds)

        return mongoService
            .database
            .getCollection<WorkspaceEdge>(edgeTypeName + "_edge")
            .find(clientSession, and(filters))

            .batchSize(BATCH_SIZE)
    }

    @Deprecated("use evaluateNodes")
    fun getEdgeTypeCandidates(from: PatternNode, to: PatternNode) = hasRelationConceptEdges.filter {
        it.fromId == from.getConceptId() && it.toId == to.getConceptId()
    }

    @Deprecated("use evaluateNodes")
    suspend fun getConnectedNodeCandidates(
        from: PatternNode,
        to: PatternNode,
        fromNodes: List<WorkspaceNode>,
        vararg toConstraints: List<PatternConstraint>
    ): List<WorkspaceEdge> {
        val acceptableRelationTypes = hasRelationConceptEdges.filter {
            it.fromId == from.getConceptId() && it.toId == to.getConceptId()
        }
        if (acceptableRelationTypes.isEmpty()) throw Error("No acceptable relations.")
        val acceptableFromId = fromNodes.map { it.nodeId }.toList()
        val toNodes = acceptableRelationTypes.flatMap {
            mongoService
                .getCollection<WorkspaceEdge>(it.name + "_edge")
                .find(WorkspaceEdge::fromId `in` acceptableFromId)
                .batchSize(BATCH_SIZE)
                .limit(MONGO_NODE_LIMIT)
                .toList()
        }
        return toNodes
    }


    @Deprecated("use evaluateNodes")
    suspend fun getNodePairsByEdge(edges: List<WorkspaceEdge>): List<WorkspaceNode> {
        val groupedEdges = edges.groupBy { it.relationId }.mapKeys { relationConceptDict[it.key]?.name }

        val collection: List<WorkspaceNode> = groupedEdges.flatMap {
            if (it.key.isNullOrEmpty()) return listOf()
            return mongoService.getCollection<WorkspaceNode>(it.key + "_node")
                .find()
                .batchSize(BATCH_SIZE)
                .toList()
        }
        return collection
    }


    private fun PatternNode.getConceptId() = conceptTypeDict[this.type]?.nodeId

    val schema get() = IdeographSchema(conceptNodes, propertyNodes, hasPropertyEdges, hasRelationConceptEdges)

    companion object {
        const val MONGO_NODE_LIMIT = 10000
        const val EVALUABLE_CANDIDATE_LIMIT = 1000
        const val BATCH_SIZE = 100
    }
}