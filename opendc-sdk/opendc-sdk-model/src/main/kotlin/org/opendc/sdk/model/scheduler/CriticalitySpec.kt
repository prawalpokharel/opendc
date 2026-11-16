/*
 * Copyright (c) 2026 AtLarge Research
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
 * Where the criticality of a task comes from: how much of its workflow depends on it.
 */
@Serializable
public enum class CriticalitySourceSpec {
    /** The number of tasks downstream of the task in its workflow. */
    @SerialName("descendants")
    DESCENDANTS,

    /** The longest chain of task durations that starts at the task (the upward rank of list schedulers). */
    @SerialName("criticalPath")
    CRITICAL_PATH,

    /** Scores per task id, read from the CSV file at `scoresPath` (columns `task_id,score`). */
    @SerialName("scores")
    SCORES,
}

/**
 * Checks that [scoresPath] is given exactly when [source] reads scores from a file.
 */
internal fun criticalityIssues(
    source: CriticalitySourceSpec,
    scoresPath: String?,
): List<ValidationIssue> =
    buildList {
        if (source == CriticalitySourceSpec.SCORES && scoresPath.isNullOrBlank()) {
            add(ValidationIssue("scoresPath", "is required when source is scores"))
        }
        if (source != CriticalitySourceSpec.SCORES && scoresPath != null) {
            add(ValidationIssue("scoresPath", "is only used when source is scores"))
        }
    }

/**
 * Places the waiting tasks from the most to the least critical, instead of first in, first out. Tasks with the same
 * criticality keep their order of arrival.
 *
 * @property source Where the criticality of a task comes from.
 * @property scoresPath The CSV file with a score per task id, when [source] is [CriticalitySourceSpec.SCORES].
 */
@Serializable
public data class TaskOrderSpec(
    public val source: CriticalitySourceSpec = CriticalitySourceSpec.DESCENDANTS,
    public val scoresPath: String? = null,
) : Validatable {
    override fun validate(): List<ValidationIssue> = criticalityIssues(source, scoresPath)
}
