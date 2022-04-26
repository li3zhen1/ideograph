package me.lizhen.schema

import kotlinx.serialization.Serializable

@Serializable
class Evidences

@Serializable
data class RelationNode(
    val _id: String,
    val judgeParent: Boolean,
    val name: String,
    val nodeId: Long,
    val nodeLabel: String,
    val parent: Long,
    val tagScore: Evidences,
    val types: String
)


@Serializable
data class HasRelationConceptEdge(
    val _id: String,
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

    override fun hashCode() = _id.hashCode()
}


@Serializable
data class WorkspaceEdge(
    val _id: String,
    val edgeId: Long,
    val edgeLabel: String,
    val fromId: Long,
    val name: String,
    val properties: Map<String, String>,
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

    override fun hashCode() = _id.hashCode()
}