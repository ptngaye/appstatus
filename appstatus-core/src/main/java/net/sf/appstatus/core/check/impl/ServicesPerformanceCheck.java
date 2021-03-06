/*
 * Copyright 2010 Capgemini and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sf.appstatus.core.check.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.sf.appstatus.core.AppStatus;
import net.sf.appstatus.core.check.AbstractCheck;
import net.sf.appstatus.core.check.IAppStatusAware;
import net.sf.appstatus.core.check.ICheckResult;
import net.sf.appstatus.core.services.IService;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Nicolas Richeton
 *
 */
public class ServicesPerformanceCheck extends AbstractCheck implements IAppStatusAware {

	private AppStatus appStatus;
	private int delay = 10;
	private int limitError = 3000;
	private int limitWarn = 1000;

	@Override
	public ICheckResult checkStatus() {
		List<IService> services = appStatus.getServiceManager().getServices();
		List<String> warns = new ArrayList<String>();
		List<String> errors = new ArrayList<String>();

		for (IService s : services) {
			// Do not report if hit count is too small
			if (this.delay > s.getHits()) {
				continue;
			}

			if (s.getAvgResponseTime() > limitError || s.getAvgResponseTimeWithCache() > limitError) {
				errors.add("Service <b>" + s.getGroup() + "#" + s.getName() + "</b> average response time ("
						+ Math.round(s.getAvgResponseTime()) + "ms without cache / "
						+ Math.round(s.getAvgNestedCallsWithCache()) + "ms with cache) is over error limit ("
						+ limitError + "ms)");
			} else if (s.getAvgResponseTime() > limitWarn || s.getAvgResponseTimeWithCache() > limitWarn) {
				warns.add("Service <b>" + s.getGroup() + "#" + s.getName() + "</b> average response time ("
						+ Math.round(s.getAvgResponseTime()) + "ms without cache /"
						+ Math.round(s.getAvgNestedCallsWithCache()) + "ms with cache) is over warn limit ("
						+ limitWarn + "ms)");
			}
		}

		ICheckResult result = null;
		if (errors.size() > 0) {
			String description = StringUtils.join(errors, "<br/>");
			if (warns.size() > 0) {
				description = description + " <br/>Additional warnings: " + StringUtils.join(warns, "<br/>");
			}
			result = result(this).code(ICheckResult.ERROR).fatal().description(description).build();
		} else if (warns.size() > 0) {
			result = result(this).code(ICheckResult.ERROR).description(StringUtils.join(warns, "<br/>")).build();
		} else {
			result = result(this).code(ICheckResult.OK).description("All average times under " + limitWarn + "ms")
					.build();
		}
		return result;
	}

	public String getGroup() {
		return "Services";
	}

	public String getName() {
		return "Performance";
	}

	public void setAppStatus(AppStatus appStatus) {
		this.appStatus = appStatus;
	}

	@Override
	public void setConfiguration(Properties configuration) {
		super.setConfiguration(configuration);

		String error = getConfiguration().getProperty("servicePerformanceCheck.limitError");
		if (error != null) {
			limitError = Integer.valueOf(error);
		}

		String warn = getConfiguration().getProperty("servicePerformanceCheck.limitWarn");
		if (warn != null) {
			limitWarn = Integer.valueOf(warn);
		}

		String delay = getConfiguration().getProperty("servicePerformanceCheck.delay");
		if (delay != null) {
			this.delay = Integer.valueOf(delay);
		}
	}
}
