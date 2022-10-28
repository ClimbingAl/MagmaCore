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
import uk.gov.gchq.magmacore.hqdm.model.Class;
import uk.gov.gchq.magmacore.hqdm.model.ClassOfStateOfOrganizationComponent;
import uk.gov.gchq.magmacore.hqdm.model.Event;
import uk.gov.gchq.magmacore.hqdm.model.OrganizationComponent;
import uk.gov.gchq.magmacore.hqdm.model.PossibleWorld;
import uk.gov.gchq.magmacore.hqdm.model.SpatioTemporalExtent;
import uk.gov.gchq.magmacore.hqdm.model.StateOfOrganizationComponent;
import uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI;
import uk.gov.gchq.magmacore.hqdm.rdfservices.RdfSpatioTemporalExtentServices;

/**
 * Builder for constructing instances of StateOfOrganizationComponent.
 */
public class StateOfOrganizationComponentBuilder {

    private final StateOfOrganizationComponent stateOfOrganizationComponent;

    /**
     * Constructs a Builder for a new StateOfOrganizationComponent.
     *
     * @param iri IRI of the StateOfOrganizationComponent.
     */
    public StateOfOrganizationComponentBuilder(final IRI iri) {
        stateOfOrganizationComponent = RdfSpatioTemporalExtentServices.createStateOfOrganizationComponent(iri.getIri());
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
    public final StateOfOrganizationComponentBuilder aggregated_Into(final SpatioTemporalExtent spatioTemporalExtent) {
        stateOfOrganizationComponent.addValue(AGGREGATED_INTO,
                new IRI(spatioTemporalExtent.getId()));
        return this;
    }

    /**
     * A {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#PART_OF} relationship type where a
     * {@link SpatioTemporalExtent} has exactly one {@link Event} that is its beginning.
     *
     * @param event The Event.
     * @return This builder.
     */
    public final StateOfOrganizationComponentBuilder beginning(final Event event) {
        stateOfOrganizationComponent.addValue(BEGINNING, new IRI(event.getId()));
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
    public final StateOfOrganizationComponentBuilder consists__Of(final SpatioTemporalExtent spatioTemporalExtent) {
        stateOfOrganizationComponent.addValue(CONSISTS__OF, new IRI(spatioTemporalExtent.getId()));
        return this;
    }

    /**
     * A {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#PART_OF} relationship type where a
     * {@link SpatioTemporalExtent} has exactly one {@link Event} that is its ending.
     *
     * @param event The Event.
     * @return This builder.
     */
    public final StateOfOrganizationComponentBuilder ending(final Event event) {
        stateOfOrganizationComponent.addValue(ENDING, new IRI(event.getId()));
        return this;
    }

    /**
     * A relationship type where a {@link uk.gov.gchq.magmacore.hqdm.model.Thing} may be a member of one
     * or more {@link Class}.
     *
     * @param clazz The Class.
     * @return This builder.
     */
    public final StateOfOrganizationComponentBuilder member__Of(final Class clazz) {
        stateOfOrganizationComponent.addValue(MEMBER__OF, new IRI(clazz.getId()));
        return this;
    }

    /**
     * A {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#MEMBER_OF} relationship type where a
     * {@link StateOfOrganizationComponent} may be a
     * {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#MEMBER_OF} one or more
     * {@link ClassOfStateOfOrganizationComponent}.
     *
     * @param classOfStateOfOrganizationComponent The ClassOfStateOfOrganizationComponent.
     * @return This builder.
     */
    public final StateOfOrganizationComponentBuilder member_Of(
            final ClassOfStateOfOrganizationComponent classOfStateOfOrganizationComponent) {
        stateOfOrganizationComponent.addValue(MEMBER_OF,
                new IRI(classOfStateOfOrganizationComponent.getId()));
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
    public final StateOfOrganizationComponentBuilder part__Of(final SpatioTemporalExtent spatioTemporalExtent) {
        stateOfOrganizationComponent.addValue(PART__OF, new IRI(spatioTemporalExtent.getId()));
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
    public final StateOfOrganizationComponentBuilder part_Of_Possible_World_M(final PossibleWorld possibleWorld) {
        stateOfOrganizationComponent.addValue(PART_OF_POSSIBLE_WORLD,
                new IRI(possibleWorld.getId()));
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
    public final StateOfOrganizationComponentBuilder temporal__Part_Of(
            final SpatioTemporalExtent spatioTemporalExtent) {
        stateOfOrganizationComponent.addValue(TEMPORAL__PART_OF,
                new IRI(spatioTemporalExtent.getId()));
        return this;
    }

    /**
     * A {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#TEMPORAL_PART_OF} relationship type where a
     * {@link StateOfOrganizationComponent} may be a
     * {@link uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI.HQDM#TEMPORAL_PART_OF} one or more
     * {@link OrganizationComponent}.
     *
     * @param organizationComponent The OrganizationComponent.
     * @return This builder.
     */
    public final StateOfOrganizationComponentBuilder temporal_Part_Of(
            final OrganizationComponent organizationComponent) {
        stateOfOrganizationComponent.addValue(TEMPORAL_PART_OF,
                new IRI(organizationComponent.getId()));
        return this;
    }

    /**
     * Returns an instance of StateOfOrganizationComponent created from the properties set on this
     * builder.
     *
     * @return The built StateOfOrganizationComponent.
     * @throws HqdmException If the StateOfOrganizationComponent is missing any mandatory properties.
     */
    public StateOfOrganizationComponent build() throws HqdmException {
        if (stateOfOrganizationComponent.hasValue(AGGREGATED_INTO)
                && stateOfOrganizationComponent.value(AGGREGATED_INTO).isEmpty()) {
            throw new HqdmException("Property Not Set: aggregated_into");
        }
        if (stateOfOrganizationComponent.hasValue(BEGINNING)
                && stateOfOrganizationComponent.value(BEGINNING).isEmpty()) {
            throw new HqdmException("Property Not Set: beginning");
        }
        if (stateOfOrganizationComponent.hasValue(ENDING)
                && stateOfOrganizationComponent.value(ENDING).isEmpty()) {
            throw new HqdmException("Property Not Set: ending");
        }
        if (stateOfOrganizationComponent.hasValue(MEMBER__OF)
                && stateOfOrganizationComponent.value(MEMBER__OF).isEmpty()) {
            throw new HqdmException("Property Not Set: member__of");
        }
        if (stateOfOrganizationComponent.hasValue(MEMBER_OF)
                && stateOfOrganizationComponent.value(MEMBER_OF).isEmpty()) {
            throw new HqdmException("Property Not Set: member_of");
        }
        if (stateOfOrganizationComponent.hasValue(PART__OF)
                && stateOfOrganizationComponent.value(PART__OF).isEmpty()) {
            throw new HqdmException("Property Not Set: part__of");
        }
        if (!stateOfOrganizationComponent.hasValue(PART_OF_POSSIBLE_WORLD)) {
            throw new HqdmException("Property Not Set: part_of_possible_world");
        }
        if (stateOfOrganizationComponent.hasValue(TEMPORAL__PART_OF)
                && stateOfOrganizationComponent.value(TEMPORAL__PART_OF).isEmpty()) {
            throw new HqdmException("Property Not Set: temporal__part_of");
        }
        if (stateOfOrganizationComponent.hasValue(TEMPORAL_PART_OF)
                && stateOfOrganizationComponent.value(TEMPORAL_PART_OF).isEmpty()) {
            throw new HqdmException("Property Not Set: temporal_part_of");
        }
        return stateOfOrganizationComponent;
    }
}