/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/test/java/blockmap/com/ibm/research/bench/ExperimentExecutor.java $
 *
 * IBM Data Engine for NoSQL - Power Systems Edition User Library Project
 *
 * Contributors Listed Below - COPYRIGHT 2015,2016,2017
 * [+] International Business Machines Corp.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * IBM_PROLOG_END_TAG
 */
package com.ibm.research.bench;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Jan S. Rellermeyer, IBM Research
 */
public final class ExperimentExecutor implements Runnable, Context {

	protected static DecimalFormat decFormat = new DecimalFormat("###.##"); //$NON-NLS-1$

	private final String name;
	private final Experiment experiment;

	private final int minCount;
	protected final int maxCount;
	private final int warmup;

	protected Exception e;
	protected final int targetStdDevPercentage;

	private long overhead = 0;

	private final Map<String, Measurement> measurements = new LinkedHashMap<String, Measurement>();

	public ExperimentExecutor(final String name, final Experiment experiment,
			final int minCount, final int maxCount, final int warmup,
			final int targetStdDevPercentage) {
		this.name = name;
		this.experiment = experiment;
		this.minCount = minCount;
		this.maxCount = maxCount;
		this.warmup = warmup;
		this.targetStdDevPercentage = targetStdDevPercentage;
	}

	public void reset() {
		e = null;
		measurements.clear();
		overhead = 0;
	}

	public void run() {
		reset();
		// warmup
		try {
			final MeasurementToken dummy = new MeasurementToken() {
			};
			final Context dummyContext = new Context() {
				public MeasurementToken startMeasurement(final String name) {
					return dummy;
				}

				public void endMeasurement(final MeasurementToken token) {
					// do nothing
				}
			};

			for (int i = 0; i < warmup; i++) {
				experiment.setUp();
				experiment.run(dummyContext);
				experiment.tearDown();
			}
		} catch (final Exception e) {
			// ignore
		}

		final Measurement token = (Measurement) startMeasurement("total"); //$NON-NLS-1$

		try {
			for (int a = 0; a < minCount || !token.isStdDevOK(); a++) {
				overhead = 0;
				experiment.setUp();
				// enclosing measurement
				token.start();
				experiment.run(this);
				token.end(overhead);
				experiment.tearDown();
			}
		} catch (final Exception e) {
			this.e = e;
		}
	}

	public void printResults() throws Exception {
		if (e != null) {
			System.out
					.println("Exception stack is not NULL. We will throw one!"); //$NON-NLS-1$
			throw e;
		}

		final Measurement total = measurements.remove("total"); //$NON-NLS-1$

		System.out.println("// " //$NON-NLS-1$
				+ name
				+ " // " //$NON-NLS-1$
				+ " RESULT " //$NON-NLS-1$
				+ total.getResult()
				+ "us, STDDEV=" //$NON-NLS-1$
				+ ExperimentExecutor.decFormat.format(total.getStdDev())
				+ " (" //$NON-NLS-1$
				+ ExperimentExecutor.decFormat.format(total
						.getStdErrDevPercentage()) + "%)"); //$NON-NLS-1$

		for (final Measurement m : measurements.values()) {
			System.out.println("\t" //$NON-NLS-1$
					+ m.name
					+ " RESULT " //$NON-NLS-1$
					+ m.getResult()
					+ "us, STDDEV=" //$NON-NLS-1$
					+ ExperimentExecutor.decFormat.format(m.getStdDev())
					+ " (" //$NON-NLS-1$
					+ ExperimentExecutor.decFormat.format(m
							.getStdErrDevPercentage()) + "%)"); //$NON-NLS-1$

		}
	}

	protected final class Measurement implements MeasurementToken {

		protected String name;
		protected int count = 0;
		protected long sum;
		protected long squareSum;
		protected double mean = 0.0;

		protected long startTime;

		protected Measurement(final String name) {
			this.name = name;
		}

		protected void start() {
			startTime = System.nanoTime() / 1000;
		}

		protected void end() {
			final long measurement = System.nanoTime() / 1000 - startTime;

			count++;
			sum += measurement;
			squareSum += measurement * measurement;
		}

		protected void end(final long overhead) {
			final long measurement = System.nanoTime() / 1000 - startTime
					- overhead;
			count++;
			sum += measurement;
			squareSum += measurement * measurement;
		}

		protected double getStdDev0() {
			mean = sum / count;
			// final double stdSum = mean * mean * count - 2 * mean * sum
			// + squareSum;
			final double std = Math
					.sqrt((mean * mean * count - 2 * mean * sum + squareSum)
							/ (count - 1));

			return std;
		}

		protected double getStdErrDevPercentage() {
			final double stdErrDevPerc = getStdErrDev() / mean * 100;
			return stdErrDevPerc;
		}

		protected double getStdErrDev() {
			final double stdDev = getStdDev0();
			final double stdErrDev = stdDev / Math.sqrt(count);
			return stdErrDev;
		}

		protected double getStdDev() throws Exception {
			if (e != null) {
				throw e;
			}
			return getStdDev0();
		}

		protected boolean isStdDevOK() throws Exception {
			if (count > maxCount) {
				throw new Exception(
						"Benchmark " //$NON-NLS-1$
						+ name
						+ " did not reach the target threshold of " //$NON-NLS-1$
						+ targetStdDevPercentage
						+ "%, was " //$NON-NLS-1$
						+ ExperimentExecutor.decFormat
						.format(getStdErrDevPercentage()) + "%"); //$NON-NLS-1$
			}
			return getStdErrDevPercentage() < targetStdDevPercentage;
		}

		protected long getResult() throws Exception {
			if (e != null) {
				throw e;
			}
			return sum / count;
		}
	}

	public MeasurementToken startMeasurement(final String name) {
		final long time = System.nanoTime();
		Measurement measurement = measurements.get(name);
		if (measurement == null) {
			measurement = new Measurement(name);
			measurements.put(name, measurement);
		}

		measurement.start();

		overhead += (System.nanoTime() - time);
		return measurement;
	}

	public void endMeasurement(final MeasurementToken token) {
		final long time = System.nanoTime();
		((Measurement) token).end();
		overhead += (System.nanoTime() - time);
	}
}
