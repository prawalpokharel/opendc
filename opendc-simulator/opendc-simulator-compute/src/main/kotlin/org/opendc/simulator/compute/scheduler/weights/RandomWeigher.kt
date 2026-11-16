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
import java.util.random.RandomGenerator

/**
 * A [HostWeigher] that gives each host a new random weight every time it is ranked, which spreads tasks over the hosts
 * at random. Useful as a baseline when comparing weighers.
 *
 * @param random The source of the weights.
 * @param multiplier The weight multiplier.
 */
public class RandomWeigher(
    private val random: RandomGenerator,
    override val multiplier: Double = 1.0,
) : HostWeigher {
    override fun getWeight(host: SimHost): Double = random.nextDouble()

    override fun toString(): String = "RandomWeigher"
}
