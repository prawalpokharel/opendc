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

package org.opendc.simulator.compute.scheduler.weights

import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.scheduler.criticality.TaskCriticality

/**
 * A [HostWeigher] that weighs a host by the total [TaskCriticality] of the tasks it runs.
 *
 * With a negative [multiplier] (the default), hosts that carry less critical work rank higher, so critical tasks are
 * spread over the hosts instead of piling up on a few. A weigher only sees the host, not the task being placed, so it
 * steers every task this way.
 *
 * @param criticality The criticality of each task, for example [org.opendc.simulator.compute.scheduler.criticality.DescendantCountCriticality].
 * @param multiplier The weight multiplier.
 */
public class CriticalityWeigher(
    private val criticality: TaskCriticality,
    override val multiplier: Double = -1.0,
) : HostWeigher {
    override fun getWeight(host: SimHost): Double {
        var total = 0.0
        for (task in host.getInstances()) {
            total += criticality.of(task)
        }
        return total
    }

    override fun toString(): String = "CriticalityWeigher($criticality)"
}
