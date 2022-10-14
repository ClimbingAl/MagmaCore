/*
 * Copyright 2021 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package uk.gov.gchq.magmacore.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import uk.gov.gchq.magmacore.database.MagmaCoreDatabase;
import uk.gov.gchq.magmacore.database.MagmaCoreJenaDatabase;
import uk.gov.gchq.magmacore.exception.MagmaCoreException;
import uk.gov.gchq.magmacore.hqdm.model.Individual;
import uk.gov.gchq.magmacore.hqdm.model.PointInTime;
import uk.gov.gchq.magmacore.hqdm.model.StateOfPerson;
import uk.gov.gchq.magmacore.hqdm.model.Thing;
import uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM;
import uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI;
import uk.gov.gchq.magmacore.hqdm.services.SpatioTemporalExtentServices;

/**
 * Check that {@link MagmaCoreService} works correctly.
 */
public class MagmaCoreServiceTest {

    /**
     * Test that triples can be deleted.
     */
    @Test
    public void test() {
        final MagmaCoreJenaDatabase database = new MagmaCoreJenaDatabase();

        final IRI individualIri = new IRI(SignPatternTestData.TEST_BASE, "individual");
        final Individual individual = SpatioTemporalExtentServices.createIndividual(individualIri.getIri());

        individual.addValue(HQDM.MEMBER_OF, "classOfIndividual");

        database.begin();
        database.create(individual);
        database.commit();

        individual.removeValue(HQDM.MEMBER_OF, "classOfIndividual");

        database.begin();
        database.update(individual);
        database.commit();

        database.begin();
        final Thing individualFromDb = database.get(individualIri);
        database.commit();

        assertNull(individualFromDb);
    }

    /**
     * Test that findBySignValue can be used to find the right Things represented by a sign value for
     * the given {@link uk.gov.gchq.magmacore.hqdm.model.Pattern} and
     * {@link uk.gov.gchq.magmacore.hqdm.model.RecognizingLanguageCommunity} at the given
     * {@link uk.gov.gchq.magmacore.hqdm.model.PointInTime}.
     */
    @Test
    public void testFindBySignSuccess() throws MagmaCoreException {

        // Create and populate an in-memory database.
        final MagmaCoreDatabase db = new MagmaCoreJenaDatabase();
        SignPatternTestData.createSignPattern(db);

        // Use it to create the services
        final MagmaCoreService service = new MagmaCoreService(db);

        // Create the PointInTime we're looking for
        final PointInTime now = SpatioTemporalExtentServices.createPointInTime("now");
        now.addValue(HQDM.ENTITY_NAME, Instant.now().toString());

        // Find the required Things by sign in a transaction.
        db.begin();
        final List<? extends Thing> found1 = service.findBySignValue(SignPatternTestData.community1,
                SignPatternTestData.pattern1, "person1", now);
        final List<? extends Thing> found2 = service.findBySignValue(SignPatternTestData.community2,
                SignPatternTestData.pattern2, "person2", now);
        db.commit();

        // Assert the results are correct.
        assertNotNull(found1);
        assertNotNull(found2);
        assertFalse(found1.isEmpty());
        assertFalse(found2.isEmpty());

        final StateOfPerson personState1 = (StateOfPerson) found1.iterator().next();
        final StateOfPerson personState2 = (StateOfPerson) found2.iterator().next();

        assertEquals(SignPatternTestData.stateOfPerson1.getId(), personState1.getId());
        assertEquals(SignPatternTestData.stateOfPerson2.getId(), personState2.getId());

        final Set<Object> parent1 = personState1.value(HQDM.TEMPORAL_PART_OF);
        final Set<Object> parent2 = personState2.value(HQDM.TEMPORAL_PART_OF);

        // Check that the `temporal_part_of` relationship is correct.
        assertEquals(1, parent1.size());
        assertEquals(1, parent2.size());

        assertEquals(SignPatternTestData.person1.getId(), ((IRI) parent1.iterator().next()).getIri());
        assertEquals(SignPatternTestData.person2.getId(), ((IRI) parent2.iterator().next()).getIri());
    }

    /**
     * Check that we get an empty result if the sign value is null.
     */
    @Test
    public void testFindBySignWithNullSignValue() throws MagmaCoreException {

        // Create and populate an in-memory database.
        final MagmaCoreDatabase db = new MagmaCoreJenaDatabase();
        SignPatternTestData.createSignPattern(db);

        // Use it to create the services
        final MagmaCoreService service = new MagmaCoreService(db);

        // Create the PointInTime we're looking for
        final PointInTime now = SpatioTemporalExtentServices.createPointInTime("now");
        now.addValue(HQDM.ENTITY_NAME, Instant.now().toString());

        // Find the required Things by sign in a transaction.
        db.begin();
        final List<? extends Thing> found = service.findBySignValue(SignPatternTestData.community1,
                SignPatternTestData.pattern1, null, now);
        db.commit();

        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    /**
     * Check that we get an empty result if the pointInTime does not have an ENTITY_NAME.
     */
    @Test
    public void testFindBySignWithBadPointInTime() throws MagmaCoreException {

        // Create and populate an in-memory database.
        final MagmaCoreDatabase db = new MagmaCoreJenaDatabase();
        SignPatternTestData.createSignPattern(db);

        // Use it to create the services
        final MagmaCoreService service = new MagmaCoreService(db);

        // Create the PointInTime we're looking for
        final PointInTime now = SpatioTemporalExtentServices.createPointInTime("now");

        // Find the required Things by sign in a transaction.
        db.begin();
        final List<? extends Thing> found = service.findBySignValue(SignPatternTestData.community1,
                SignPatternTestData.pattern1, "person1", now);
        db.commit();

        assertNotNull(found);
        assertTrue(found.isEmpty());
    }
}