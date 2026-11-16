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

import org.opendc.simulator.compute.task.SimTask

/**
 * How much other work depends on a [SimTask]: the higher the value, the more of its workflow waits on it, and the more
 * a failure of the task delays. A [org.opendc.simulator.compute.scheduler.weights.CriticalityWeigher] adds it up per
 * host, and as the [org.opendc.simulator.compute.scheduler.ComputeScheduler.taskPriority] it places the most critical
 * waiting task first.
 */
public fun interface TaskCriticality {
    /**
     * The criticality of [task], at least 0.
     */
    public fun of(task: SimTask): Double
}

/**
 * Base class for criticalities computed from the workflow graph of a task, which the trace gives through the
 * [SimTask.children] of each task. A child is looked up through the [org.opendc.simulator.compute.service.ComputeService]
 * of the task, so only tasks that have been submitted count.
 *
 * Each value is computed once and kept, at the index of the task id. A task's children cannot finish before the task
 * itself, so the value stays valid while the task waits or runs. A value is not kept when a child was not found,
 * because the child may simply not be submitted yet.
 */
public abstract class GraphCriticality : TaskCriticality {
    private var values = DoubleArray(1024) { Double.NaN }

    final override fun of(task: SimTask): Double {
        val id = task.id
        if (id < values.size) {
            val known = values[id]
            if (!known.isNaN()) {
                return known
            }
        }

        val (value, complete) = compute(task)
        if (complete && id >= 0) {
            if (id >= values.size) {
                val old = values.size
                values = values.copyOf(maxOf(id + 1, old * 2))
                values.fill(Double.NaN, old, values.size)
            }
            values[id] = value
        }
        return value
    }

    /**
     * Compute the criticality of [task], and whether every child it depends on was found.
     */
    protected abstract fun compute(task: SimTask): Pair<Double, Boolean>

    /**
     * Whether the value of the task with [id] is computed and kept.
     */
    protected fun isKnown(id: Int): Boolean = id >= 0 && id < values.size && !values[id].isNaN()

    /**
     * The submitted child with [id] of [task], or `null` when it is not (or no longer) known to the service.
     */
    protected fun child(
        task: SimTask,
        id: Int,
    ): SimTask? = task.service?.findTask(id)
}

/**
 * The number of tasks downstream of a task: its children, their children, and so on, each counted once.
 */
public class DescendantCountCriticality : GraphCriticality() {
    override fun compute(task: SimTask): Pair<Double, Boolean> {
        val seen = HashSet<Int>()
        val stack = ArrayDeque<SimTask>()
        stack.addLast(task)
        var complete = true

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val children = current.children ?: continue
            for (id in children) {
                if (!seen.add(id)) {
                    continue
                }
                val next = child(current, id)
                if (next == null) {
                    complete = false
                } else {
                    stack.addLast(next)
                }
            }
        }

        return Pair(seen.size.toDouble(), complete)
    }

    override fun toString(): String = "DescendantCountCriticality"
}

/**
 * The length in seconds of the longest chain of tasks that starts at a task, the task included, using the
 * [SimTask.duration] each task has in the trace (a task without a duration counts as 0). This is the upward rank of
 * list schedulers such as HEFT, with no communication costs.
 */
public class CriticalPathCriticality : GraphCriticality() {
    override fun compute(task: SimTask): Pair<Double, Boolean> {
        var longest = 0.0
        var complete = true
        val children = task.children
        if (children != null) {
            for (id in children) {
                val next = child(task, id)
                if (next == null) {
                    complete = false
                } else {
                    longest = maxOf(longest, of(next))
                    complete = complete && isKnown(next.id)
                }
            }
        }
        return Pair(task.duration.toMs() / 1000.0 + longest, complete)
    }

    override fun toString(): String = "CriticalPathCriticality"
}

/**
 * Criticalities given per task id, for example computed offline or declared by an operator. A task without a score
 * gets [default].
 */
public class ScoreCriticality(
    scores: Map<Int, Double>,
    private val default: Double = 0.0,
) : TaskCriticality {
    private val scores = HashMap(scores)

    override fun of(task: SimTask): Double = scores[task.id] ?: default

    override fun toString(): String = "ScoreCriticality(${scores.size} tasks)"
}
