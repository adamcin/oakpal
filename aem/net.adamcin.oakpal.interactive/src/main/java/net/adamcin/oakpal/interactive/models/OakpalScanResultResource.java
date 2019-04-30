/*
 * Copyright 2019 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.oakpal.interactive.models;

import java.util.List;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.interactive.OakpalScanInput;
import net.adamcin.oakpal.interactive.OakpalScanResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;

@Model(adaptables = {Resource.class},
        adapters = {OakpalScanResult.class, OakpalScanResultResource.class})
public class OakpalScanResultResource implements OakpalScanResult {

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private OakpalScanInputResource input;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private List<CheckReport> reports;

    @Override
    public OakpalScanInput getInput() {
        return input;
    }

    @Override
    public List<CheckReport> getReports() {
        return reports;
    }
}
