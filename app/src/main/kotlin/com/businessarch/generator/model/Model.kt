package com.businessarch.generator.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

fun getId(): String {
    val n = Random.nextLong(36_36_36_36) // ограничим диапазон
    return n.toString(36)
}
@Serializable
data class AsObject(
    val id: String = getId(),
    val name: String,                    // String или GetRef
    val platform: String? = null,        // String или GetRef
    val role: List<String>? = null
)

// ---------- FP ----------

@Serializable
data class FpObject(
    val id: String = getId(),
    val name: String,                    // String или GetRef
    @SerialName("AS")
    val asName: String? = null,          // String или GetRef
    val role: List<String>? = null
)

// ---------- Function ----------

@Serializable
data class FunctionObject(
    val id: String = getId(),
    val name: String,
    @SerialName("AS")
    val asName: String? = null,
    @SerialName("FP")
    val fpName: String? = null
)

// ---------- BO ----------

@Serializable
data class BoObject(
    val id: String = getId(),
    val name: String,                    // String или GetRef
)

// ---------- PP (если используется отдельно) ----------

@Serializable
data class PpObject(
    val id: String = getId(),
    val name: String,
    @SerialName("AS")
    val asName: String? = null,
    @SerialName("FP")
    val fpName: String? = null,
)

// ---------- Link ----------

@Serializable
data class LinkEnd(
    val type: String = "AS",  // "AS" | "FP" | "BO"
    val name: String? = null,
    val platform: String? = null,
    @SerialName("AS")
    val asName: String? = null,
    val role: List<String>? = null
)

@Serializable
data class LinkObject(
    val id: String = getId(),
    val source: LinkEnd,
    val target: LinkEnd,
)

// Var теперь просто Map<String, String>
// Вместо [{"name": "x", "value": "y"}] используем {"x": "y"}

// ---------- Итоговый объект arch ----------

@Serializable
data class ArchResult(
    val Var: Map<String, String> = emptyMap(),
    val AS: List<AsObject> = emptyList(),
    val FP: List<FpObject> = emptyList(),
    @SerialName("Function")
    val function: List<FunctionObject> = emptyList(),
    @SerialName("Link")
    val link: List<LinkObject> = emptyList(),
    val BO: List<BoObject> = emptyList(),
    val PP: List<PpObject> = emptyList()
)