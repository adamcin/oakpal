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

package net.adamcin.oakpal.core.checks;

import javax.json.Json;
import javax.json.JsonObject;

import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class CompatBaseFactory implements ProgressCheckFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public final ProgressCheck newInstance(final JSONObject config) throws Exception {
        logger.info("ProgressCheckFactory.newInstance(org.json.JSONObject config) is deprecated as of 1.2.0.");
        logger.info("Please use newInstance(javax.json.JsonObject config) instead.");
        return newInstance(Json.createObjectBuilder(config.toMap()).build());
    }

    @Override
    public abstract ProgressCheck newInstance(final JsonObject config) throws Exception;
}
