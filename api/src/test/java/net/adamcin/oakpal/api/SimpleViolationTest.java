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
import org.junit.Test;

import javax.json.JsonObject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void builder_testFactory() {
        assertNotNull("expect instance of builder", SimpleViolation.builder());
        final ResourceBundle myBundle = ResourceBundle.getBundle(getClass().getName());
        assertNotNull("expect instance of builder with bundle", SimpleViolation.builder(myBundle));
    }

    @Test
    public void builder_testNoBundle() {
        final SimpleViolation.Builder builder = SimpleViolation.builder();
        final SimpleViolation defaults = builder.build();
        assertEquals("expect severity", Severity.MAJOR, defaults.getSeverity());
        assertNull("expect null description", defaults.getDescription());
        assertEquals("expect empty packages", 0, defaults.getPackages().size());
        final SimpleViolation minor = builder.withSeverity(Severity.MINOR).build();
        assertEquals("expect severity", Severity.MINOR, minor.getSeverity());
        assertNull("expect null description", minor.getDescription());
        assertEquals("expect empty packages", 0, minor.getPackages().size());
        final SimpleViolation severe = builder.withSeverity(Severity.SEVERE).build();
        assertEquals("expect severity", Severity.SEVERE, severe.getSeverity());
        assertNull("expect null description", severe.getDescription());
        assertEquals("expect empty packages", 0, severe.getPackages().size());
        final SimpleViolation major = builder.withSeverity(Severity.MAJOR).build();
        assertEquals("expect severity", Severity.MAJOR, major.getSeverity());
        assertNull("expect null description", major.getDescription());
        assertEquals("expect empty packages", 0, major.getPackages().size());

        final SimpleViolation described = builder.withDescription("described").build();
        assertEquals("expect description", "described", described.getDescription());
        final PackageId firstPackage = PackageId.fromString("my_packages:first");
        final PackageId secondPackage = PackageId.fromString("my_packages:second");
        final SimpleViolation withPackagesVarargs = builder.withPackage(firstPackage, secondPackage).build();
        final SimpleViolation withPackagesList = builder.withPackages(Arrays.asList(firstPackage, secondPackage)).build();
        assertEquals("expect equal packages", withPackagesList, withPackagesVarargs);
        final SimpleViolation withPackagesAppended = builder.withPackage(firstPackage, secondPackage).build();
        final SimpleViolation withPackagesDuplicated = builder.withPackages(Arrays.asList(firstPackage, secondPackage,
                firstPackage, secondPackage)).build();
        assertEquals("expect packages appended", withPackagesDuplicated, withPackagesAppended);

        final SimpleViolation withArgumentsVarargs = builder.withDescription("test {0} {1}")
                .withArgument("one", "two")
                .build();
        assertEquals("expect description varargs", "test one two", withArgumentsVarargs.getDescription());
        final SimpleViolation withArgumentsList = builder.withArguments(Arrays.asList("one", "two")).build();
        assertEquals("expect description list", "test one two", withArgumentsList.getDescription());
        final SimpleViolation withArgumentsListReversed = builder.withArguments(Arrays.asList("two", "one")).build();
        assertEquals("expect description list reversed", "test two one",
                withArgumentsListReversed.getDescription());
        assertEquals("expect description list reversed varargs appended", "test two one",
                builder.withArgument("one", "two").build().getDescription());
    }

    @Test
    public void builder_testWithBundle() {
        final String key = "first {0} then {1}";
        final List<Object> arguments = Arrays.asList("one", "two");
        final SimpleViolation.Builder builder = SimpleViolation
                .builder(ResourceBundle.getBundle(SimpleViolationTest.class.getName()));
        final SimpleViolation forward = builder.withArguments(arguments)
                .withDescription(key).build();
        assertEquals("expect forward string", "first one then two", forward.getDescription());
        final SimpleViolation.Builder builderReversed = SimpleViolation
                .builder(ResourceBundle.getBundle(SimpleViolationTest.class.getName() + "Reversed"));
        final SimpleViolation reversed = builderReversed.withArguments(arguments)
                .withDescription(key).build();
        assertEquals("expect reverse string", "first two then one", reversed.getDescription());
    }
}
