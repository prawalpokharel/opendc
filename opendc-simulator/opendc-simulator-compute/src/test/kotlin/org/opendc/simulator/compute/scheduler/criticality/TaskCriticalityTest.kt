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

package org.opendc.simulator.compute.scheduler.criticality

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.common.units.TimeDelta
import org.opendc.simulator.compute.service.ComputeService
import org.opendc.simulator.compute.task.SimTask

/**
 * Test suite for the [TaskCriticality] implementations.
 */
internal class TaskCriticalityTest {
    /**
     * A diamond workflow 0 -> {1, 2} -> 3 with durations of 10, 20, 30 and 40 seconds. Task ids in [missing] are not
     * known to the service yet.
     */
    private fun diamond(missing: MutableSet<Int> = mutableSetOf()): Map<Int, SimTask> {
        val service = mockk<ComputeService>()
        val children = mapOf(0 to intArrayOf(1, 2), 1 to intArrayOf(3), 2 to intArrayOf(3), 3 to null)
        val durations = mapOf(0 to 10, 1 to 20, 2 to 30, 3 to 40)
        val tasks =
            children.mapValues { (id, kids) ->
                val task = mockk<SimTask>()
                every { task.id } returns id
                every { task.children } returns kids
                every { task.duration } returns TimeDelta.ofSec(durations.getValue(id))
                every { task.service } returns service
                task
            }
        every { service.findTask(any()) } answers {
            val id = firstArg<Int>()
            if (id in missing) null else tasks[id]
        }
        return tasks
    }

    @Test
    fun testDescendantCount() {
        val tasks = diamond()
        val criticality = DescendantCountCriticality()

        assertAll(
            // Task 3 is downstream of 0 through two paths, but counts once
            { assertEquals(3.0, criticality.of(tasks.getValue(0))) },
            { assertEquals(1.0, criticality.of(tasks.getValue(1))) },
            { assertEquals(1.0, criticality.of(tasks.getValue(2))) },
            { assertEquals(0.0, criticality.of(tasks.getValue(3))) },
        )
    }

    @Test
    fun testCriticalPath() {
        val tasks = diamond()
        val criticality = CriticalPathCriticality()

        assertAll(
            { assertEquals(40.0, criticality.of(tasks.getValue(3))) },
            { assertEquals(60.0, criticality.of(tasks.getValue(1))) },
            { assertEquals(70.0, criticality.of(tasks.getValue(2))) },
            // 10 s for task 0, then the longer branch through task 2
            { assertEquals(80.0, criticality.of(tasks.getValue(0))) },
        )
    }

    @Test
    fun testMissingChildIsNotKept() {
        val missing = mutableSetOf(3)
        val tasks = diamond(missing)
        val descendants = DescendantCountCriticality()
        val path = CriticalPathCriticality()

        // Task 3 is not submitted yet: it counts as a descendant, but its duration is unknown
        assertEquals(3.0, descendants.of(tasks.getValue(0)))
        assertEquals(30.0, path.of(tasks.getValue(2)))

        // Once task 3 is known, the values are computed again instead of the incomplete ones being reused
        missing.clear()
        assertEquals(70.0, path.of(tasks.getValue(2)))
        assertEquals(80.0, path.of(tasks.getValue(0)))
    }

    @Test
    fun testScores() {
        val tasks = diamond()
        val criticality = ScoreCriticality(mapOf(0 to 0.5, 1 to 2.0), default = 1.0)

        assertAll(
            { assertEquals(0.5, criticality.of(tasks.getValue(0))) },
            { assertEquals(2.0, criticality.of(tasks.getValue(1))) },
            // Tasks without a score get the default
            { assertEquals(1.0, criticality.of(tasks.getValue(2))) },
        )
    }
}
