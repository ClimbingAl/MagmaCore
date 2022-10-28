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

package uk.gov.gchq.magmacore.hqdm.rdfbuilders;

import static uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM.AGGREGATED_INTO;
import static uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM.BEGINNING;
import static uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM.CONSISTS__OF;
import static uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM.ENDING;
import static uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM.MEMBER_OF;
import static uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM.MEMBER__OF;
import static uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM.PART_OF_POSSIBLE_WORLD;
import static uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM.PART__OF;
import static uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM.TEMPORAL_PART_OF;
import static uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM.TEMPORAL__PART_OF;

import uk.gov.gchq.magmacore.hqdm.exception.HqdmException;
import uk.gov.gchq.magmacore.hqdm.model.BiologicalObject;
import uk.gov.gchq.magmacore.hqdm.model.Class;
import uk.gov.gchq.magmacore.hqdm.model.ClassOfStateOfBiologicalObject;
import uk.gov.gchq.magmacore.hqdm.model.Event;
import uk.gov.gchq.magmacore.hqdm.model.PossibleWorld;
import uk.gov.gchq.magmacore.hqdm.model.SpatioTemporalExtent;
import uk.gov.gchq.magmacore.hqdm.model.StateOfBiologicalObject;
import uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI;
import uk.gov.gchq.magmacore.hqdm.rdfservices.RdfSpatioTemporalExtentServices;

/**
 * Builder for constructing instances of StateOfBiologicalObject.
 */
public class StateOfBiologicalObjectBuilder {

    private final StateOfBiologicalObject stateOfBiologicalObject;

    /**
     * Constructs a Builder for a new StateOfBiologicalObject.
     *
     * @param iri IRI of the StateOfBiologicalObject.
     */
    public StateOfBiologicalObjectBuilder(final IRI iri) {
        stateOfBiologicalObject = RdfSpatioTemporalExtentServices.createStateOfBiologicalObject(iri.getIri());
    }

    /**
     * A relationship type where a {@link SpatioTemporalExtent} may be aggregated into one or more
     * others.
     * <p>
     * Note: This has the same meaning as, but different representation to, the
     * {@link uk.gov.gchq.magmacore.hqdm.model.Aggregation} entity type.
     * </p>
     *
     * @param spatioTemporalExtent The SpatioTemporalExtent.
     * @return This builder.
     */
    public final StateOfBiologicalObjectBuilder aggregated_Into(final SpatioTemporalExtent spatioTemporalExtent) {
        stateOfBiologicalObject.addValue(AGGREGATED_INTO, new IRI(spatioTemporalExtent.getId()));
        return this;
    }

    /**
     * A {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#PART_OF} relationship type where a
     * {@link SpatioTemporalExtent} has exactly one {@link Event} that is its beginning.
     *
     * @param event The Event.
     * @return This builder.
     */
    public final StateOfBiologicalObjectBuilder beginning(final Event event) {
        stateOfBiologicalObject.addValue(BEGINNING, new IRI(event.getId()));
        return this;
    }

    /**
     * A relationship type where a {@link SpatioTemporalExtent} may consist of one or more others.
     *
     * <p>
     * Note: This is the inverse of {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#PART__OF}.
     * </p>
     *
     * @param spatioTemporalExtent The SpatioTemporalExtent.
     * @return This builder.
     */
    public final StateOfBiologicalObjectBuilder consists__Of(final SpatioTemporalExtent spatioTemporalExtent) {
        stateOfBiologicalObject.addValue(CONSISTS__OF, new IRI(spatioTemporalExtent.getId()));
        return this;
    }

    /**
     * A {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#PART_OF} relationship type where a
     * {@link SpatioTemporalExtent} has exactly one {@link Event} that is its ending.
     *
     * @param event The Event.
     * @return This builder.
     */
    public final StateOfBiologicalObjectBuilder ending(final Event event) {
        stateOfBiologicalObject.addValue(ENDING, new IRI(event.getId()));
        return this;
    }

    /**
     * A relationship type where a {@link uk.gov.gchq.magmacore.hqdm.model.Thing} may be a member of one
     * or more {@link Class}.
     *
     * @param clazz The Class.
     * @return This builder.
     */
    public final StateOfBiologicalObjectBuilder member__Of(final Class clazz) {
        stateOfBiologicalObject.addValue(MEMBER__OF, new IRI(clazz.getId()));
        return this;
    }

    /**
     * A {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#MEMBER_OF} relationship type where a
     * {@link StateOfBiologicalObject} may be a
     * {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#MEMBER_OF} one or more
     * {@link ClassOfStateOfBiologicalObject}.
     *
     * @param classOfStateOfBiologicalObject The ClassOfStateOfBiologicalObject.
     * @return This builder.
     */
    public final StateOfBiologicalObjectBuilder member_Of(
            final ClassOfStateOfBiologicalObject classOfStateOfBiologicalObject) {
        stateOfBiologicalObject.addValue(MEMBER_OF,
                new IRI(classOfStateOfBiologicalObject.getId()));
        return this;
    }

    /**
     * An {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#AGGREGATED_INTO} relationship type where a
     * {@link SpatioTemporalExtent} may be part of another and the whole has emergent properties and is
     * more than just the sum of its parts.
     *
     * @param spatioTemporalExtent The SpatioTemporalExtent.
     * @return This builder.
     */
    public final StateOfBiologicalObjectBuilder part__Of(final SpatioTemporalExtent spatioTemporalExtent) {
        stateOfBiologicalObject.addValue(PART__OF, new IRI(spatioTemporalExtent.getId()));
        return this;
    }

    /**
     * A {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#PART_OF} relationship type where a
     * {@link SpatioTemporalExtent} may be {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#PART_OF}
     * one or more {@link PossibleWorld}.
     *
     * <p>
     * Note: The relationship is optional because a {@link PossibleWorld} is not
     * {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#PART_OF} any other
     * {@link SpatioTemporalExtent}.
     * </p>
     *
     * @param possibleWorld The PossibleWorld.
     * @return This builder.
     */
    public final StateOfBiologicalObjectBuilder part_Of_Possible_World_M(final PossibleWorld possibleWorld) {
        stateOfBiologicalObject.addValue(PART_OF_POSSIBLE_WORLD, new IRI(possibleWorld.getId()));
        return this;
    }

    /**
     * A {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#PART_OF} relationship type where a
     * {@link SpatioTemporalExtent} may be a temporal part of one or more other
     * {@link SpatioTemporalExtent}.
     *
     * @param spatioTemporalExtent The SpatioTemporalExtent.
     * @return This builder.
     */
    public final StateOfBiologicalObjectBuilder temporal__Part_Of(final SpatioTemporalExtent spatioTemporalExtent) {
        stateOfBiologicalObject.addValue(TEMPORAL__PART_OF, new IRI(spatioTemporalExtent.getId()));
        return this;
    }

    /**
     * A {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#TEMPORAL_PART_OF} relationship type where a
     * {@link StateOfBiologicalObject} may be a
     * {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#TEMPORAL_PART_OF} one or more
     * {@link BiologicalObject}.
     *
     * @param biologicalObject The BiologicalObject.
     * @return This builder.
     */
    public final StateOfBiologicalObjectBuilder temporal_Part_Of(final BiologicalObject biologicalObject) {
        stateOfBiologicalObject.addValue(TEMPORAL_PART_OF, new IRI(biologicalObject.getId()));
        return this;
    }

    /**
     * Returns an instance of StateOfBiologicalObject created from the properties set on this builder.
     *
     * @return The built StateOfBiologicalObject.
     * @throws HqdmException If the StateOfBiologicalObject is missing any mandatory properties.
     */
    public StateOfBiologicalObject build() throws HqdmException {
        if (stateOfBiologicalObject.hasValue(AGGREGATED_INTO)
                && stateOfBiologicalObject.value(AGGREGATED_INTO).isEmpty()) {
            throw new HqdmException("Property Not Set: aggregated_into");
        }
        if (stateOfBiologicalObject.hasValue(BEGINNING)
                && stateOfBiologicalObject.value(BEGINNING).isEmpty()) {
            throw new HqdmException("Property Not Set: beginning");
        }
        if (stateOfBiologicalObject.hasValue(ENDING)
                && stateOfBiologicalObject.value(ENDING).isEmpty()) {
            throw new HqdmException("Property Not Set: ending");
        }
        if (stateOfBiologicalObject.hasValue(MEMBER__OF)
                && stateOfBiologicalObject.value(MEMBER__OF).isEmpty()) {
            throw new HqdmException("Property Not Set: member__of");
        }
        if (stateOfBiologicalObject.hasValue(MEMBER_OF)
                && stateOfBiologicalObject.value(MEMBER_OF).isEmpty()) {
            throw new HqdmException("Property Not Set: member_of");
        }
        if (stateOfBiologicalObject.hasValue(PART__OF)
                && stateOfBiologicalObject.value(PART__OF).isEmpty()) {
            throw new HqdmException("Property Not Set: part__of");
        }
        if (!stateOfBiologicalObject.hasValue(PART_OF_POSSIBLE_WORLD)) {
            throw new HqdmException("Property Not Set: part_of_possible_world");
        }
        if (stateOfBiologicalObject.hasValue(TEMPORAL__PART_OF)
                && stateOfBiologicalObject.value(TEMPORAL__PART_OF).isEmpty()) {
            throw new HqdmException("Property Not Set: temporal__part_of");
        }
        if (stateOfBiologicalObject.hasValue(TEMPORAL_PART_OF)
                && stateOfBiologicalObject.value(TEMPORAL_PART_OF).isEmpty()) {
            throw new HqdmException("Property Not Set: temporal_part_of");
        }
        return stateOfBiologicalObject;
    }
}