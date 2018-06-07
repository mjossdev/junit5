/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.math.BigDecimal;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * Default implementations of configuration strategies for parallel test
 * execution.
 *
 * @since 1.3
 */
@API(status = EXPERIMENTAL, since = "1.3")
public enum DefaultParallelExecutionConfigurationStrategy implements ParallelExecutionConfigurationStrategy {

	/**
	 * Uses the mandatory {@value CONFIG_FIXED_PARALLELISM_PROPERTY_NAME} configuration
	 * parameter as desired parallelism.
	 */
	FIXED {
		@Override
		public ParallelExecutionConfiguration createConfiguration(ConfigurationParameters configurationParameters) {
			int parallelism = configurationParameters.get(CONFIG_FIXED_PARALLELISM_PROPERTY_NAME,
				Integer::valueOf).orElseThrow(
					() -> new JUnitException(CONFIG_FIXED_PARALLELISM_PROPERTY_NAME + " must be set"));
			return new DefaultParallelExecutionConfiguration(parallelism, parallelism, 256 + parallelism, parallelism,
				30);
		}
	},

	/**
	 * Computes the desired parallelism based on the number of available
	 * processors/cores multiplied by the {@value CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME}
	 * configuration parameter.
	 */
	DYNAMIC {
		@Override
		public ParallelExecutionConfiguration createConfiguration(ConfigurationParameters configurationParameters) {
			BigDecimal factor = configurationParameters.get(CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME,
				BigDecimal::new).orElse(BigDecimal.ONE);
			Preconditions.condition(factor.compareTo(BigDecimal.ZERO) > 0,
				() -> CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME + " must be greater than 0");
			int parallelism = Math.max(1,
				factor.multiply(BigDecimal.valueOf(Runtime.getRuntime().availableProcessors())).intValue());
			return new DefaultParallelExecutionConfiguration(parallelism, parallelism, 256 + parallelism, parallelism,
				30);
		}
	},

	/**
	 * Allows to specify a custom {@link ParallelExecutionConfigurationStrategy}
	 * implementation via the mandatory {@value CONFIG_CUSTOM_CLASS_PROPERTY_NAME}
	 * configuration parameter to determine the desired configuration.
	 */
	CUSTOM {
		@Override
		public ParallelExecutionConfiguration createConfiguration(ConfigurationParameters configurationParameters) {
			String className = configurationParameters.get(CONFIG_CUSTOM_CLASS_PROPERTY_NAME).orElseThrow(
				() -> new JUnitException(CONFIG_CUSTOM_CLASS_PROPERTY_NAME + " must be set"));
			Class<?> strategyClass = ReflectionUtils.loadClass(className).orElseThrow(
				() -> new JUnitException("Could not load class for " + CONFIG_CUSTOM_CLASS_PROPERTY_NAME));
			Preconditions.condition(ParallelExecutionConfigurationStrategy.class.isAssignableFrom(strategyClass),
				CONFIG_CUSTOM_CLASS_PROPERTY_NAME + " does not implement "
						+ ParallelExecutionConfigurationStrategy.class);
			ParallelExecutionConfigurationStrategy strategy = (ParallelExecutionConfigurationStrategy) ReflectionUtils.newInstance(
				strategyClass);
			return strategy.createConfiguration(configurationParameters);
		}
	};

	/**
	 * Property name used to determine the desired configuration strategy.
	 *
	 * <p>Value must be one of {@code dynamic}, {@code fixed}, or
	 * {@code custom}.
	 */
	public static final String CONFIG_STRATEGY_PROPERTY_NAME = "strategy";

	/**
	 * Property name used to determine the desired parallelism by the
	 * {@link #FIXED} configuration strategy.
	 *
	 * <p>No default value; must be an integer.
	 *
	 * @see #FIXED
	 */
	public static final String CONFIG_FIXED_PARALLELISM_PROPERTY_NAME = "fixed.parallelism";

	/**
	 * Property name of the factor used to determine the desired parallelism by the
	 * {@link #DYNAMIC} configuration strategy.
	 *
	 * <p>Value must be a decimal number; defaults to {@code 1}.
	 *
	 * @see #DYNAMIC
	 */
	public static final String CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME = "dynamic.factor";

	/**
	 * Property name used to specify the fully qualified class name of the
	 * {@link ParallelExecutionConfigurationStrategy} to be used by the
	 * {@link #CUSTOM} configuration strategy.
	 *
	 * @see #CUSTOM
	 */
	@API(status = EXPERIMENTAL, since = "5.3")
	public static final String CONFIG_CUSTOM_CLASS_PROPERTY_NAME = "custom.class";

	static ParallelExecutionConfigurationStrategy getStrategy(ConfigurationParameters configurationParameters) {
		return valueOf(configurationParameters.get(CONFIG_STRATEGY_PROPERTY_NAME).orElse("dynamic").toUpperCase());
	}

}
