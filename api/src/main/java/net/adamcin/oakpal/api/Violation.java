/*
 * Copyright 2020 Mark Adamcin
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

package net.adamcin.oakpal.api;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.osgi.annotation.versioning.ProviderType;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Collection;

/**
 * Report type for validations.
 */
@ProviderType
public interface Violation extends JsonObjectConvertible {

    /**
     * Describe the severity of the violation.
     *
     * @return the severity of the violation
     */
    Severity getSeverity();

    /**
     * Provides a list of one or more Packages responsible for the violation.
     *
     * @return a list of package IDs responsible for the violation.
     */
    Collection<PackageId> getPackages();

    /**
     * Describes the nature of the violation.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Serializes the Violation to a JsonObject.
     *
     * @return the json representation of the violation
     */
    @Override
    default JsonObject toJson() {
        JsonObjectBuilder json = Json.createObjectBuilder();
        if (this.getSeverity() != null) {
            json.add(ApiConstants.violationKeys().severity(), this.getSeverity().toString());
        }
        if (this.getDescription() != null) {
            json.add(ApiConstants.violationKeys().description(), this.getDescription());
        }
        if (this.getPackages() != null && !this.getPackages().isEmpty()) {
            JsonArrayBuilder array = Json.createArrayBuilder();
            for (PackageId packageId : this.getPackages()) {
                array.add(packageId.toString());
            }
            json.add(ApiConstants.violationKeys().packages(), array.build());
        }
        return json.build();
    }
}
