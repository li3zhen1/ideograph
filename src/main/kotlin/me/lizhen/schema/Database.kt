package me.lizhen.schema

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ObjectId(@SerialName("\$oid") val oid: String)

@Serializable
class Evidences

@Serializable
data class RelationNode(
    val _id: String,
//    val _class: String,
//    val evidences: Evidences,
    val judgeParent: Boolean,
    val name: String,
    val nodeId: Long,
    val nodeLabel: String,
    val parent: Long,
    val tagScore: Evidences,
//    val trustedAccessibility: Long,
//    val trustedCorrectness: Long,
//    val trustedLevel: Long,
//    val trustedReliability: Long,
//    val trustedSafety: Long,
//    val trustedTimeliness: Long,
//    val trustedValue: Long,
    val types: String
)


@Serializable
data class HasRelationConceptEdge(
    val _id: String,
//    val _class: String,
    val fromId: Long,
    val toId: Long,
    val edgeId: Long,
    val relationId: Long,
    val name: String,
)

@Serializable
data class HasPropertyEdge(
    val _id: String,
    val _class: String,
    val fromId: Long,
    val toId: Long,
    val edgeId: Long,
    val relationId: Long,
    val name: String,
)

@Serializable
data class ConceptNode(
    val _id: String,
//    val _class: String,
    val name: String,
    val nodeId: Long,
    val nodeLabel: String,
    val label: String?,
    val type: String?,
    val labels: String?,
    val types: String?,
    val tags: String?,
    val judgeParent: Boolean,
)

@Serializable
data class PropertyNode(
    val _id: String,
//    val _class: String,
    val name: String,
    val nodeId: Long,
    val nodeLabel: String,
    val label: String?,
    val type: String?,
    val labels: String?,
    val types: String?,
    val tags: String?,
    val judgeParent: Boolean,
)

@Serializable
data class WorkspaceNode(
    val _id: String,
//    val _class: String,
    val name: String,
    val nodeId: Long,
    val nodeLabel: String,
    val types: String?,
    val tags: String?,
    val judgeParent: Boolean,
    val properties: Map<String, String>,
) {
    override fun equals(other: Any?): Boolean {
        if(other is WorkspaceNode)
            return nodeId == other.nodeId
        return false
    }
}


@Serializable
data class WorkspaceEdge(
    val _id: String,
//    val _class: String,
//    val clusterId: Long,
    val edgeId: Long,
    val edgeLabel: String,
//    val evidences: Evidences,
    val fromId: Long,
    val name: String,
    val properties: Evidences,
    val relationId: Long,
    val source: String,
    val tagScore: Evidences,
    val toId: Long,
//    val trustedAccessibility: Long,
//    val trustedCorrectness: Long,
//    val trustedLevel: Long,
//    val trustedReliability: Long,
//    val trustedSafety: Long,
//    val trustedTimeliness: Long,
//    val trustedValue: Long
) {
    override fun equals(other: Any?): Boolean {
        if(other is WorkspaceEdge)
            return (edgeId == other.edgeId
//                    && fromId == other.edgeId && toId == other.edgeId
                    )
        return false
    }
}