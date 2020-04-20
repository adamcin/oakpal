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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Test;

import javax.json.JsonObject;
import java.util.Collection;
import java.util.Collections;

public class SimpleViolationTest {

    @Test
    public void testConstruct() {
        SimpleViolation nulls = new SimpleViolation(null, null, (PackageId[]) null);
        assertNotNull("toString not null", nulls);
    }

    @Test
    public void testFromReported() {
        final Severity severity = Severity.MINOR;
        final PackageId packId = PackageId.fromString("my_pack");
        final String description = "some description";
        final Violation violation = new Violation() {
            @Override
            public Severity getSeverity() {
                return severity;
            }

            @Override
            public Collection<PackageId> getPackages() {
                return Collections.singletonList(packId);
            }

            @Override
            public String getDescription() {
                return description;
            }
        };

        final SimpleViolation simpleViolation = SimpleViolation.fromReported(violation);
        assertSame("expect same severity", severity, simpleViolation.getSeverity());
        assertEquals("expect equal packageIds", Collections.singletonList(packId),
                simpleViolation.getPackages());
        assertSame("expect same description", description, simpleViolation.getDescription());
    }

    @Test
    public void testFromJson() {
        final Severity severity = Severity.MINOR;
        final PackageId packId = PackageId.fromString("my_pack");
        final String description = "some description";
        final Violation violation = new Violation() {
            @Override
            public Severity getSeverity() {
                return severity;
            }

            @Override
            public Collection<PackageId> getPackages() {
                return Collections.singletonList(packId);
            }

            @Override
            public String getDescription() {
                return description;
            }
        };

        final JsonObject json = violation.toJson();

        final SimpleViolation simpleViolation = SimpleViolation.fromJson(json);
        assertSame("expect same severity", severity, simpleViolation.getSeverity());
        assertEquals("expect equal packageIds", Collections.singletonList(packId),
                simpleViolation.getPackages());
        assertEquals("expect equal description", description, simpleViolation.getDescription());
    }

    @Test
    public void testToString() {
        final Severity severity = Severity.MINOR;
        final PackageId packId = PackageId.fromString("my_pack");
        final String description = "some description";
        final Violation violation = new Violation() {
            @Override
            public Severity getSeverity() {
                return severity;
            }

            @Override
            public Collection<PackageId> getPackages() {
                return Collections.singletonList(packId);
            }

            @Override
            public String getDescription() {
                return description;
            }
        };

        final JsonObject json = violation.toJson();
        final SimpleViolation simpleViolation = SimpleViolation.fromJson(json);
        assertTrue("my_pack in toString", simpleViolation.toString().contains("my_pack"));
        assertTrue("description in toString", simpleViolation.toString().contains(description));
    }

    @Test
    public void testEquals() {
        final Severity severity = Severity.MINOR;
        final PackageId packId = PackageId.fromString("my_pack");
        final String description = "some description";
        final Violation violation = new Violation() {
            @Override
            public Severity getSeverity() {
                return severity;
            }

            @Override
            public Collection<PackageId> getPackages() {
                return Collections.singletonList(packId);
            }

            @Override
            public String getDescription() {
                return description;
            }
        };

        final JsonObject json = violation.toJson();
        final SimpleViolation simpleViolation = SimpleViolation.fromJson(json);
        assertNotEquals("SimpleViolation not equal to Violation", simpleViolation, violation);
        final SimpleViolation simpleViolationCopy = SimpleViolation.fromReported(violation);
        assertEquals("SimpleViolation equal to copy SimpleViolation", simpleViolation, simpleViolationCopy);

        // also hashCode
        assertNotEquals("SimpleViolation hashCode not equal to Violation hashCode",
                simpleViolation.hashCode(), violation.hashCode());
        assertEquals("SimpleViolation hashCode equal to copy SimpleViolation hashCode",
                simpleViolation.hashCode(), simpleViolationCopy.hashCode());
    }

}
