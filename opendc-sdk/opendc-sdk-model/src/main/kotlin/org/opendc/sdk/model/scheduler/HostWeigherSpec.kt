/*
 * Copyright (c) 2025 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.sdk.model.scheduler

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue

/**
 * Scores candidate hosts so the scheduler can rank them; the [multiplier] weights the score.
 */
@Serializable
public sealed interface HostWeigherSpec : Validatable {
    /** The weight applied to this weigher's contribution to the final host score. */
    public val multiplier: Double

    override fun validate(): List<ValidationIssue> =
        if (multiplier.isFinite()) emptyList() else listOf(ValidationIssue("multiplier", "must be finite"))
}

/**
 * Weighs hosts by their available memory.
 *
 * @property multiplier The weight applied to the memory score.
 */
@Serializable
@SerialName("ram")
public data class RamWeigherSpec(override val multiplier: Double = 1.0) : HostWeigherSpec

/**
 * Weighs hosts by the combination of their available cores and memory.
 *
 * @property multiplier The weight applied to the core-and-memory score.
 */
@Serializable
@SerialName("coreRam")
public data class CoreRamWeigherSpec(override val multiplier: Double = 1.0) : HostWeigherSpec

/**
 * Weighs hosts by the number of task instances they already run.
 *
 * @property multiplier The weight applied to the instance-count score.
 */
@Serializable
@SerialName("instanceCount")
public data class InstanceCountWeigherSpec(override val multiplier: Double = 1.0) : HostWeigherSpec

/**
 * Weighs hosts by their available per-core capacity.
 *
 * @property multiplier The weight applied to the vCPU-capacity score.
 */
@Serializable
@SerialName("vcpuCapacity")
public data class VCpuCapacityWeigherSpec(override val multiplier: Double = 1.0) : HostWeigherSpec

/**
 * Weighs hosts by their available vCPUs.
 *
 * @property multiplier The weight applied to the vCPU score.
 */
@Serializable
@SerialName("vcpu")
public data class VCpuWeigherSpec(override val multiplier: Double = 1.0) : HostWeigherSpec

/**
 * Weighs hosts by their available vGPUs.
 *
 * @property multiplier The weight applied to the vGPU score.
 */
@Serializable
@SerialName("vgpu")
public data class VGpuWeigherSpec(override val multiplier: Double = 1.0) : HostWeigherSpec

/**
 * Weighs hosts by the total criticality of the tasks they run. With a negative [multiplier] (the default), hosts that
 * carry less critical work rank higher, which spreads critical tasks over the hosts.
 *
 * @property source Where the criticality of a task comes from.
 * @property scoresPath The CSV file with a score per task id, when [source] is [CriticalitySourceSpec.SCORES].
 * @property multiplier The weight applied to the criticality score.
 */
@Serializable
@SerialName("criticality")
public data class CriticalityWeigherSpec(
    public val source: CriticalitySourceSpec = CriticalitySourceSpec.DESCENDANTS,
    public val scoresPath: String? = null,
    override val multiplier: Double = -1.0,
) : HostWeigherSpec {
    override fun validate(): List<ValidationIssue> = super.validate() + criticalityIssues(source, scoresPath)
}

/**
 * Gives each host a random weight every time it is ranked, which spreads tasks at random. A baseline for comparing
 * weighers.
 *
 * @property multiplier The weight applied to the random score.
 */
@Serializable
@SerialName("random")
public data class RandomWeigherSpec(override val multiplier: Double = 1.0) : HostWeigherSpec
