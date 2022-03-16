package me.lizhen.solvers

import me.lizhen.schema.*
import me.lizhen.service.DgraphService
import me.lizhen.service.MongoService
import org.bson.conversions.Bson

import org.litote.kmongo.*

class IdeographContext(
    public val mongoService: MongoService,
    public val dgraphService: DgraphService
) {
    init {
        initializeSchema()
    }

    fun resolvePattern() {

    }

    lateinit var conceptNodes: List<ConceptNode>
    lateinit var relationNodes: List<RelationNode>
    lateinit var hasRelationConceptNodes: List<HasRelationConceptEdge>
    lateinit var conceptTypeDict: Map<String?, ConceptNode>
    lateinit var relationConceptFromDict: Map<Long, List<HasRelationConceptEdge>>

    fun initializeSchema() {
        conceptNodes = mongoService
            .getCollection<ConceptNode>("concept_node")
            .find()
            .toList()


        relationNodes = mongoService
            .getCollection<RelationNode>("relation_node")
            .find()
            .toList()

        hasRelationConceptNodes = mongoService
            .getCollection<HasRelationConceptEdge>("hasRelationConcept_edge")
            .find()
            .toList()

        relationConceptFromDict = hasRelationConceptNodes.groupBy { it.fromId }
        conceptTypeDict = conceptNodes.associateBy { it.name }
    }


    fun getSingleNodeCandidates(node: PatternNode, vararg constraints: PatternConstraint): List<WorkspaceNode> {
        val bsonMap = constraints.map {
            WorkspaceNode::properties.keyProjection(it.property) regex it.value
        }
        val collection = mongoService
            .getCollection<WorkspaceNode>(node.type + "_node")
            .find(and(bsonMap))
            .limit(MONGO_NODE_LIMIT)
            .toList()
        return collection
    }

    fun getConnectedNodeCandidates(
        from: PatternNode,
        to: PatternNode,
        fromNodes: List<WorkspaceNode>,
        vararg toConstraints: List<PatternConstraint>
    ): List<WorkspaceEdge> {
        val acceptableRelationTypes = hasRelationConceptNodes.filter {
            it.fromId == from.getConceptId() && it.toId == to.getConceptId()
        }
        if (acceptableRelationTypes.isEmpty()) throw Error("No acceptable relations.")
        val acceptableFromId = fromNodes.map { it.nodeId }.toList()
        val toNodes = acceptableRelationTypes.flatMap {
            mongoService
                .getCollection<WorkspaceEdge>(it.name + "_edge")
                .find(WorkspaceEdge::fromId `in` acceptableFromId)
                .limit(MONGO_NODE_LIMIT)
                .toList()
        }
        return toNodes
    }

    fun getNodePairsByEdge(pattern: PatternEdge, edges: List<WorkspaceEdge>) {
        val collection = mongoService.getCollection<WorkspaceEdge>(edges.+"_node")
    }

    private fun PatternNode.getConceptId() = conceptTypeDict[this.type]?.nodeId

    companion object {
        const val MONGO_NODE_LIMIT = 100;

    }
}