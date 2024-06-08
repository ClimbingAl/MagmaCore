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

import java.io.InputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;

import uk.gov.gchq.magmacore.database.MagmaCoreDatabase;
import uk.gov.gchq.magmacore.database.query.QueryResult;
import uk.gov.gchq.magmacore.database.query.QueryResultList;
import uk.gov.gchq.magmacore.database.validation.ValidationReportEntry;
import uk.gov.gchq.magmacore.exception.MagmaCoreException;
import uk.gov.gchq.magmacore.hqdm.model.Individual;
import uk.gov.gchq.magmacore.hqdm.model.KindOfAssociation;
import uk.gov.gchq.magmacore.hqdm.model.Participant;
import uk.gov.gchq.magmacore.hqdm.model.Pattern;
import uk.gov.gchq.magmacore.hqdm.model.PointInTime;
import uk.gov.gchq.magmacore.hqdm.model.RecognizingLanguageCommunity;
import uk.gov.gchq.magmacore.hqdm.model.RepresentationByPattern;
import uk.gov.gchq.magmacore.hqdm.model.Role;
import uk.gov.gchq.magmacore.hqdm.model.Thing;
import uk.gov.gchq.magmacore.hqdm.rdf.iri.HQDM;
import uk.gov.gchq.magmacore.hqdm.rdf.iri.IRI;
import uk.gov.gchq.magmacore.hqdm.rdf.iri.IriBase;
import uk.gov.gchq.magmacore.service.dto.ParticipantDetails;
import uk.gov.gchq.magmacore.service.dto.SignPatternDto;
import uk.gov.gchq.magmacore.service.sparql.MagmaCoreServiceQueries;
import uk.gov.gchq.magmacore.service.transformation.DbChangeSet;
import uk.gov.gchq.magmacore.service.transformation.DbCreateOperation;
import uk.gov.gchq.magmacore.service.transformation.DbDeleteOperation;
import uk.gov.gchq.magmacore.service.transformation.DbTransformation;
import uk.gov.gchq.magmacore.service.verify.DataIntegrityReport;

/**
 * Service for interacting with a {@link MagmaCoreDatabase}.
 *
 * <p>
 * Note that Transaction control is manual unless using any of the following:
 * </p>
 * <ol>
 * <li>getInTransaction()</li>
 * <li>runInReadTransaction()</li>
 * <li>runInWriteTransaction()</li>
 * <li>exportTtl()</li>
 * <li>importTtl()</li>
 * <li>verifyModel()</li>
 * </ol>
 * <p>
 * Nested transactions are not supported so ensure that no transaction is in
 * progress before
 * using any of the above methods.
 * </p>
 */
public class MagmaCoreService {

    private final MagmaCoreDatabase database;

    /**
     * Constructs a MagmaCoreService for a {@link MagmaCoreDatabase}.
     *
     * @param database {@link MagmaCoreDatabase} to build the service for.
     */
    MagmaCoreService(final MagmaCoreDatabase database) {
        this.database = database;
    }

    /**
     * Find the details of participants in associations of a specific kind between
     * two
     * {@link Individual} objects at a point in time.
     *
     * @param individual1 The first {@link Individual}.
     * @param individual2 The second {@link Individual}.
     * @param kind        The {@link KindOfAssociation}.
     * @param pointInTime The {@link PointInTime} that the associations should
     *                    exist.
     * @return A {@link Set} of {@link ParticipantDetails}.
     */
    public Set<ParticipantDetails> findParticipantDetails(final Individual individual1, final Individual individual2,
            final KindOfAssociation kind, final PointInTime pointInTime) {

        final Instant when = Instant.parse(pointInTime.oneValue(HQDM.ENTITY_NAME));

        final QueryResultList queryResultList = database
                .executeQuery(String.format(MagmaCoreServiceQueries.FIND_PARTICIPANT_DETAILS_QUERY, individual1.getId(),
                        individual2.getId(),
                        kind.getId(), individual1.getId(), individual2.getId(), kind.getId()));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        // Process all of the participants.
        return database.toTopObjects(queryResults)
                .stream()
                // Map them to ParticipantDetails objects.
                .map(p -> {
                    // Get the Roles of the Participant.
                    final Set<Role> roles = p.values(HQDM.MEMBER_OF_KIND)
                            .stream()
                            .map(o -> (IRI) o)
                            .map(roleIri -> database.get(roleIri))
                            .map(role -> (Role) role)
                            .collect(Collectors.toSet());
                    return new ParticipantDetails((Participant) p, roles);
                })
                .collect(Collectors.toSet());
    }

    /**
     * Filter a {@link QueryResultList} by a {@link PointInTime}. The
     * {@link QueryResultList} should
     * have `start` and `finish` columns to allow filtering.
     *
     * @param when            {@link Instant}.
     * @param queryResultList {@link QueryResultList}.
     * @return {@link QueryResultList}.
     */
    private QueryResultList filterByPointInTime(final Instant when, final QueryResultList queryResultList) {
        final List<QueryResult> queryResults = queryResultList.getQueryResults()
                .stream()
                .filter(qr -> {
                    final RDFNode start = qr.get("start");
                    final RDFNode finish = qr.get("finish");
                    final Instant from = (start != null) ? Instant.parse(start.toString())
                            : Instant.MIN;
                    final Instant to = (finish != null) ? Instant.parse(finish.toString())
                            : Instant.MAX;

                    return (when.equals(from) || when.isAfter(from))
                            && (when.equals(to) || when.isBefore(to));
                })
                .collect(Collectors.toList());
        return new QueryResultList(queryResultList.getVarNames(), queryResults);
    }

    /**
     * Find the Set of {@link Thing} represented by the given sign value.
     *
     * <p>
     * This could probably be replaced with a (rather complex) SPARQL query if
     * {@link MagmaCoreDatabase}
     * allowed the execution of such queries.
     * </p>
     *
     * @param community   The {@link RecognizingLanguageCommunity} that recognizes
     *                    the sign value.
     * @param pattern     The {@link Pattern} the sign conforms to.
     * @param value       {@link String} the sign value to look for.
     * @param pointInTime {@link PointInTime} the point in time we are interested
     *                    in.
     * @return {@link List} of {@link Thing} represented by the value.
     * @throws MagmaCoreException if the number of {@link RepresentationByPattern}
     *                            found is not 1.
     */
    public List<? extends Thing> findBySignValue(
            final RecognizingLanguageCommunity community,
            final Pattern pattern,
            final String value,
            final PointInTime pointInTime) throws MagmaCoreException {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database
                .executeQuery(String.format(MagmaCoreServiceQueries.FIND_BY_SIGN_VALUE_QUERY,
                        value,
                        community.getId(),
                        pattern.getId()));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);
    }

    /**
     * Find the Set of {@link Thing} represented by the given partial sign value.
     *
     * <p>
     * The search is case-insensitive.
     * This could probably be replaced with a (rather complex) SPARQL query if
     * {@link MagmaCoreDatabase}
     * allowed the execution of such queries.
     * </p>
     *
     * @param community   The {@link RecognizingLanguageCommunity} that recognizes
     *                    the sign value.
     * @param pattern     The {@link Pattern} the sign conforms to.
     * @param value       {@link String} the partial sign value to look for.
     * @param pointInTime {@link PointInTime} the point in time we are interested
     *                    in.
     * @return {@link List} of {@link Thing} represented by the value.
     * @throws MagmaCoreException if the number of {@link RepresentationByPattern}
     *                            found is not 1.
     */
    public List<? extends Thing> findByPartialSignValue(
            final RecognizingLanguageCommunity community,
            final Pattern pattern,
            final String value,
            final PointInTime pointInTime) throws MagmaCoreException {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database
                .executeQuery(String.format(MagmaCoreServiceQueries.FIND_BY_PARTIAL_SIGN_VALUE_CASE_INSENSITIVE_QUERY,
                        value,
                        community.getId(),
                        pattern.getId()));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);
    }

    /**
     * Find Things of a giver rdf:type and Class and their signs that are of a
     * particular pattern.
     *
     * @param type        IRI.
     * @param clazz       IRI.
     * @param pattern     IRI.
     * @param pointInTime {@link PointInTime}.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByTypeClassAndSignPattern(
            final IRI type,
            final IRI clazz,
            final IRI pattern,
            final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database
                .executeQuery(String.format(MagmaCoreServiceQueries.FIND_OBJECTS_BY_TYPE_CLASS_AND_SIGN_PATTERN,
                        type, clazz, pattern, type, clazz, pattern));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);
    }

    /**
     * Find Things of a giver rdf:type and Kind and their signs that are of a
     * particular pattern.
     *
     * @param type        IRI.
     * @param kind        IRI.
     * @param pattern     IRI.
     * @param pointInTime {@link PointInTime}.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByTypeKindAndSignPattern(
            final IRI type,
            final IRI kind,
            final IRI pattern,
            final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database
                .executeQuery(String.format(MagmaCoreServiceQueries.FIND_OBJECTS_BY_TYPE_AND_SIGN_PATTERN,
                        type, kind, pattern, type, kind, pattern));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);
    }

    /**
     * Find Individuals with states participating in associations of a specified
     * kind, their roles and
     * signs.
     *
     * @param kindOfAssociation {@link IRI}.
     * @param pointInTime       {@link PointInTime}.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByKindOfAssociation(final IRI kindOfAssociation, final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database
                .executeQuery(String.format(MagmaCoreServiceQueries.FIND_BY_KIND_OF_ASSOCIATION,
                        kindOfAssociation, kindOfAssociation, kindOfAssociation));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);

    }

    /**
     * Find the items associated to an item by an association of a specified kind.
     *
     * @param item              IRI
     * @param kindOfAssociation IRI
     * @return {@link List} of {@link Thing}
     */
    public List<? extends Thing> findAssociated(final IRI item, final IRI kindOfAssociation) {

        final QueryResultList queryResultList = database
                .executeQuery(String.format(MagmaCoreServiceQueries.FIND_ASSOCIATED,
                        kindOfAssociation, item, item,
                        kindOfAssociation, item, item,
                        kindOfAssociation, item, item));

        return database.toTopObjects(queryResultList);

    }

    /**
     * Find the items associated to an item by an association of a specified kind
     * that are valid at a PointInTime.
     *
     * @param item              IRI
     * @param kindOfAssociation IRI
     * @param pointInTime       {@link PointInTime}
     * @return {@link List} of {@link Thing}
     */
    public List<? extends Thing> findAssociated(final IRI item, final IRI kindOfAssociation,
            final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database
                .executeQuery(String.format(MagmaCoreServiceQueries.FIND_ASSOCIATED,
                        kindOfAssociation, item, item,
                        kindOfAssociation, item, item,
                        kindOfAssociation, item, item));

        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);
        return database.toTopObjects(queryResults);

    }

    /**
     * A case-sensitive search for entities in a specified class with a sign
     * containing the given text.
     *
     * @param text        The String to search for.
     * @param classIri    The IRI of the class that the entities should be a
     *                    member_of.
     * @param pointInTime When the entities should have the matching sign.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByPartialSignAndClassCaseSensitive(final String text, final IRI classIri,
            final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database
                .executeQuery(
                        String.format(MagmaCoreServiceQueries.FIND_MEMBERS_OF_CLASS_BY_PARTIAL_SIGN_CASE_SENSITIVE,
                                text, classIri,
                                text, classIri,
                                text, classIri));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);

    }

    /**
     * A case-insensitive search for entities in a specified class with a sign
     * containing the given text.
     *
     * @param text        The String to search for.
     * @param classIri    The IRI of the class that the entities should be a
     *                    member_of.
     * @param pointInTime When the entities should have the matching sign.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByPartialSignAndClass(final String text, final IRI classIri,
            final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database
                .executeQuery(
                        String.format(MagmaCoreServiceQueries.FIND_MEMBERS_OF_CLASS_BY_PARTIAL_SIGN_CASE_INSENSITIVE,
                                text, classIri,
                                text, classIri,
                                text, classIri));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);

    }

    /**
     * A case-insensitive search for entities in a specified class with a sign
     * containing the given text
     * that are referenced by an activity.
     *
     * @param wholeIri    The object that the required entities are composed into.
     * @param text        The String to search for.
     * @param classIri    The IRI of the class that the entities should be a
     *                    member_of.
     * @param pointInTime When the entities should have the matching sign.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByPartialSignByActivityReferenceAndClass(final IRI wholeIri,
            final String text, final IRI classIri,
            final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database.executeQuery(String.format(
                MagmaCoreServiceQueries.FIND_MEMBERS_OF_CLASS_BY_ACTIVITY_AND_PARTIAL_SIGN_CASE_INSENSITIVE,
                text, classIri, wholeIri,
                text, classIri, wholeIri,
                text, classIri, wholeIri));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);

    }

    /**
     * A case-sensitive search for entities in a specified class with a sign
     * containing the given text
     * that are referenced by an activity.
     *
     * @param wholeIri    The object that the required entities are composed into.
     * @param text        The String to search for.
     * @param classIri    The IRI of the class that the entities should be a
     *                    member_of.
     * @param pointInTime When the entities should have the matching sign.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByPartialSignByActivityReferenceAndClassCaseSensitive(final IRI wholeIri,
            final String text, final IRI classIri,
            final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database.executeQuery(String.format(
                MagmaCoreServiceQueries.FIND_MEMBERS_OF_CLASS_BY_ACTIVITY_AND_PARTIAL_SIGN_CASE_SENSITIVE,
                text, classIri, wholeIri,
                text, classIri, wholeIri,
                text, classIri, wholeIri));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);

    }

    /**
     * A case-sensitive search for entities in a specified class with a sign
     * containing the given text
     * that are parts of a given whole.
     *
     * @param wholeIri    The object that the required entities are composed into.
     * @param text        The String to search for.
     * @param classIri    The IRI of the class that the entities should be a
     *                    member_of.
     * @param pointInTime When the entities should have the matching sign.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByPartialSignCompositionAndClassCaseSensitive(final IRI wholeIri,
            final String text, final IRI classIri,
            final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database.executeQuery(String.format(
                MagmaCoreServiceQueries.FIND_MEMBERS_OF_CLASS_BY_COMPOSITION_AND_PARTIAL_SIGN_CASE_SENSITIVE,
                text, classIri, wholeIri,
                text, classIri, wholeIri,
                text, classIri, wholeIri));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);

    }

    /**
     * A case-insensitive search for entities in a specified class with a sign
     * containing the given text
     * that are parts of a given whole.
     *
     * @param wholeIri    The object that the required entities are composed into.
     * @param text        The String to search for.
     * @param classIri    The IRI of the class that the entities should be a
     *                    member_of.
     * @param pointInTime When the entities should have the matching sign.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByPartialSignCompositionAndClass(final IRI wholeIri,
            final String text, final IRI classIri,
            final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database.executeQuery(String.format(
                MagmaCoreServiceQueries.FIND_MEMBERS_OF_CLASS_BY_COMPOSITION_AND_PARTIAL_SIGN_CASE_INSENSITIVE,
                text, classIri, wholeIri,
                text, classIri, wholeIri,
                text, classIri, wholeIri));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);

        return database.toTopObjects(queryResults);

    }

    /**
     * Find the signs and their patterns for an entity.
     *
     * @param entityIri   The entity {@link IRI}.
     * @param pointInTime A {@link PointInTime}.
     * @return A {@link List} of {@link SignPatternDto} objects.
     */
    public List<SignPatternDto> findSignsForEntity(final IRI entityIri, final PointInTime pointInTime) {

        final String pointInTimeValue = pointInTime.oneValue(HQDM.ENTITY_NAME);
        if (pointInTimeValue == null) {
            return List.of();
        }

        final Instant when = Instant.parse(pointInTimeValue);

        final QueryResultList queryResultList = database.executeQuery(String.format(
                MagmaCoreServiceQueries.FIND_SIGNS_FOR_ENTITY, entityIri));

        // Filter by the pointInTime
        final QueryResultList queryResults = filterByPointInTime(when, queryResultList);
        return queryResults.getQueryResults()
                .stream()
                .map(MagmaCoreService::toSignPatternDto)
                .collect(Collectors.toList());
    }

    /**
     * Find the Thing referenced by a field value where the thing is a member of the
     * given class.
     *
     * @param fieldIri   The HQDM predicate IRI.
     * @param fieldValue The field value - typically a {@link String} or
     *                   {@link IRI}.
     * @param classIri   The class {@link IRI}.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByFieldValueAndClass(
            final IRI fieldIri,
            final Object fieldValue,
            final IRI classIri) {

        final QueryResultList queryResultList = database.executeQuery(String.format(
                MagmaCoreServiceQueries.FIND_BY_FIELD_VALUE_AND_CLASS, fieldIri, fieldValue, classIri));

        return database.toTopObjects(queryResultList);
    }

    /**
     * Convert a specific Query Result into a SignPatternDto.
     *
     * @param qr {@link QueryResult}.
     * @return {@link SignPatternDto}.
     */
    private static SignPatternDto toSignPatternDto(final QueryResult qr) {
        return new SignPatternDto(
                qr.get("sign_value").toString(),
                qr.get("pattern_name").toString(),
                qr.get("rep_by_pattern_name").toString());
    }

    /**
     * Find an object by its {@link HQDM#ENTITY_NAME}.
     *
     * @param <T>        HQDM entity type.
     * @param entityName Entity name value to search for.
     * @return {@link Thing} that was found.
     * @throws RuntimeException If no or multiple results were found.
     */
    public <T extends Thing> T findByEntityName(final String entityName) {
        final List<Thing> searchResult = findByPredicateIriAndValue(HQDM.ENTITY_NAME, entityName);

        if (searchResult.size() == 1) {
            return (T) searchResult.get(0);
        } else if (searchResult.isEmpty()) {
            throw new RuntimeException("No entity found with name: " + entityName);
        } else {
            throw new RuntimeException("Multiple entities found with name: " + entityName);
        }
    }

    /**
     * Find objects by a predicate.
     *
     * @param <T>       HQDM entity type.
     * @param predicate the predicate {@link IRI}
     * @return a List of {@link Thing} that were found.
     */
    public <T extends Thing> List<T> findByPredicateIriOnly(final IRI predicate) {
        return (List<T>) database.findByPredicateIriOnly(predicate);
    }

    /**
     * Find objects by a predicate value.
     *
     * @param <T>       HQDM entity type.
     * @param predicate the predicate {@link IRI}
     * @param value     The value of the predicate.
     * @return a List of {@link Thing} that were found.
     */
    public <T extends Thing> List<T> findByPredicateIriAndValue(final IRI predicate, final Object value) {
        return (List<T>) database.findByPredicateIriAndValue(predicate, value);
    }

    /**
     * Find members of a given class.
     *
     * @param classIri The class {@link IRI}.
     * @return A {@link List} of {@link Thing}.
     */
    public List<? extends Thing> findByClass(final IRI classIri) {
        return database.findByPredicateIri(HQDM.MEMBER_OF, classIri);
    }

    /**
     * Create a new {@link Thing} in the database.
     *
     * @param thing {@link Thing} to create.
     */
    public void create(final Thing thing) {
        database.create(thing);
    }

    /**
     * Delete an entity from the collection.
     *
     * @param object Entity to delete.
     */
    void delete(final Thing object) {
        database.delete(object);
    }

    /**
     * Apply a set of deletes to the database.
     *
     * @param deletes a {@link List} of {@link DbDeleteOperation}
     */
    void delete(final List<DbDeleteOperation> deletes) {
        database.delete(deletes);
    }

    /**
     * Convert a {@link Collection} of {@link Thing} objects to a
     * {@link DbTransformation} that can be
     * used to persist them. Typically this should be followed by a call to
     * `runInTransaction`.
     *
     * @param things a {@link Collection} of {@link Thing} objects to be persisted.
     * @return {@link DbTransformation}
     */
    public DbTransformation createDbTransformation(final Collection<? extends Thing> things) {
        return new DbTransformation(things.stream()
                .map(MagmaCoreService::toDbChangeSet)
                .toList());
    }

    /**
     * Convert a {@link Thing} to a {@link DbChangeSet} by creating a
     * {@link DbCreateOperation} for each
     * of its predicate valiues.
     *
     * @param thing a {@link Thing}
     * @return a {@link DbChangeSet}
     */
    private static DbChangeSet toDbChangeSet(final Thing thing) {

        // Map the Thing's predicates to DbCreateOperation objects.
        final IRI iri = thing.getId();
        final List<DbCreateOperation> creates = thing.getPredicates()
                .entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                        .stream()
                        .map(value -> {
                            return new DbCreateOperation(iri, (IRI) entry.getKey(), value);
                        }))
                .toList();

        return new DbChangeSet(List.of(), creates);
    }

    /**
     * Update an existing {@link Thing} in the database.
     *
     * @param thing {@link Thing} to update.
     */
    public void update(final Thing thing) {
        database.update(thing);
    }

    /**
     * Apply a set of deletes then a set of creates to the database.
     *
     * @param deletes A {@link List} of {@link DbDeleteOperation}.
     * @param creates A {@link List} of {@link DbCreateOperation}.
     */
    public void update(final List<DbDeleteOperation> deletes, final List<DbCreateOperation> creates) {
        database.delete(deletes);
        database.create(creates);
    }

    /**
     * Get a {@link Thing} by its IRI.
     *
     * @param iri IRI of the thing.
     * @return {@link Thing} to get.
     */
    public Thing get(final IRI iri) {
        return database.get(iri);
    }

    /**
     * Get a {@link Thing} by its {@link IRI} in a transactional database.
     *
     * @param iri {@link IRI} of the {@link Thing}.
     * @return {@link Thing} to get.
     */
    public Thing getInTransaction(final IRI iri) {
        try {
            database.beginRead();
            final Thing result = database.get(iri);
            database.commit();
            return result;
        } catch (final Exception e) {
            database.abort();
            throw e;
        }
    }

    /**
     * Run a {@link Function} in a transaction.
     *
     * @param func {@link Function} to run.
     */
    public void runInReadTransaction(final Function<MagmaCoreService, MagmaCoreService> func) {
        try {
            database.beginRead();
            func.apply(this);
            database.commit();
        } catch (final Exception e) {
            database.abort();
            throw e;
        }
    }

    /**
     * Run a {@link Function} in a transaction.
     *
     * @param func {@link Function} to run.
     */
    public void runInWriteTransaction(final Function<MagmaCoreService, MagmaCoreService> func) {
        try {
            database.beginWrite();
            func.apply(this);
            database.commit();
        } catch (final Exception e) {
            database.abort();
            throw e;
        }
    }

    /**
     * Find entities by their names.
     *
     * @param entityNames {@link List} of entity names.
     * @return {@link Map} of {@link String} to {@link Thing}.
     */
    public Map<String, Thing> findByEntityNameInTransaction(final List<String> entityNames) {
        try {
            database.beginRead();
            final HashMap<String, Thing> result = new HashMap<String, Thing>();

            for (final String name : entityNames) {
                result.put(name, findByEntityName(name));
            }

            database.commit();

            return result;
        } catch (final Exception e) {
            database.abort();
            throw e;
        }
    }

    /**
     * Find objects by a predicate in transaction.
     *
     * @param <T>       HQDM entity type.
     * @param predicate the predicate {@link IRI}
     * @return a List of {@link Thing} that were found.
     */
    public <T extends Thing> List<T> findByPredicateIriInTransaction(final IRI predicate) {

        try {
            database.beginRead();
            final List<Thing> result = database.findByPredicateIriOnly(predicate);
            database.commit();
            return (List<T>) result;

        } catch (final Exception e) {
            database.abort();
            throw e;
        }

    }

    /**
     * Find objects by a predicate and value in transaction.
     *
     * @param <T>       HQDM entity type.
     * @param predicate the predicate {@link IRI}
     * @param value     The value of the predicate.
     * @return a List of {@link Thing} that were found.
     */
    public <T extends Thing> List<T> findByPredicateIriAndValueInTransaction(final IRI predicate, final Object value) {

        try {
            database.beginRead();
            final List<Thing> result = database.findByPredicateIriAndValue(predicate, value);
            database.commit();
            return (List<T>) result;

        } catch (final Exception e) {
            database.abort();
            throw e;
        }

    }

    /**
     * Dump the database to TTL format.
     *
     * @param out A {@link PrintStream}.
     */
    public void exportTtl(final PrintStream out) {
        database.dump(out, RDFFormat.TURTLE_PRETTY);
    }

    /**
     * Load TTL data from an {@link InputStream}.
     *
     * @param in An {@link InputStream} of TTL data.
     */
    public void importTtl(final InputStream in) {
        database.load(in, Lang.TTL);
    }

    /**
     * Verify that the model in the database matches how we want to use HQDM.
     *
     * @return A {@link List} of {@link Thing} representing model integrity errors.
     */
    public List<Thing> verifyModel() {
        return DataIntegrityReport.verify(database);
    }

    /**
     * Start a transaction in READ mode.
     */
    public void beginRead() {
        database.beginRead();
    }

    /**
     * Start a transaction in Write mode.
     */
    public void beginWrite() {
        database.beginWrite();
    }

    /**
     * Commit a transaction - Finish the current transaction and make any changes
     * permanent (if a
     * "write" transaction).
     */
    public void commit() {
        database.commit();
    }

    /**
     * Abort a transaction - Finish the transaction and undo any changes (if a
     * "write" transaction).
     */
    public void abort() {
        database.abort();
    }

    /**
     * Execute a SELECT query.
     *
     * @param query a SELECT query {@link String}
     * @return a {@link QueryResultList}
     */
    public QueryResultList executeQuery(final String query) {
        return database.executeQuery(query);
    }

    /**
     * SPARQL queries restricted to having 3 columns for the subject, predicate, and
     * object, with any names but they must be in that order. E.g.
     * SELECT ?s ?p ?o WHERE {...} order by ?s ?p ?o
     * The first column must contain entity IDs.
     * The second column must contain predicates.
     * The third column can contain IRIs or primitive values.
     *
     * @param query a SELECT query {@link String}
     * @return a {@link Map} of {@link IRI} to {@link Thing}
     */
    public Map<IRI, Thing> executeQueryForThings(final String query) {
        final QueryResultList resultsList = database.executeQuery(query);

        final List<Thing> things = database.toTopObjects(resultsList);

        final Map<IRI, Thing> result = new HashMap<>();

        things.forEach(t -> result.put(t.getId(), t));

        return result;
    }

    /**
     * Apply a set of inference rules to a subset of the model and return a
     * MagmaCoreService attached to
     * the resulting inference model for further use by the caller.
     *
     * @param query            a SPARQL query String to extract a subset of the
     *                         model for inferencing.
     * @param rules            a set of inference rules to be applied to the model
     *                         subset.
     * @param includeRdfsRules boolean true if inferencing should include the
     *                         standard RDFS entailments.
     * @return an in-memory MagmaCoreService attached to the inferencing results
     *         which is independent of the source dataset.
     */
    public MagmaCoreService applyInferenceRules(final String query, final String rules,
            final boolean includeRdfsRules) {
        // This functionality is likely to be database-implementation-specific, so
        // delegate.
        final MagmaCoreDatabase db = database.applyInferenceRules(query, rules, includeRdfsRules);
        return new MagmaCoreService(db);
    }

    /**
     * Apply a set of inference rules to a subset of the model and return a List of
     * ValidationReportEntry objects.
     *
     * @param query            a SPARQL query String to extract a subset of the
     *                         model for inferencing.
     * @param rules            a set of inference rules to be applied to the model
     *                         subset.
     * @param includeRdfsRules boolean true if inferencing should include the
     *                         standard RDFS entailments.
     * @return A {@link List} of {@link ValidationReportEntry} objects.
     *         It will be Optional.empty if the underlying database is not an
     *         inference model.
     */
    public List<ValidationReportEntry> validate(final String query, final String rules,
            final boolean includeRdfsRules) {
        return database.validate(query, rules, includeRdfsRules);
    }

    /**
     * Load some TTL from an InputStream.
     *
     * @param stream {@link InputStream}
     */
    public void loadTtl(final InputStream stream) {
        database.load(stream, Lang.TTL);
    }

    /**
     * Constructs a MagmaCoreService for a {@link MagmaCoreDatabase}.
     *
     * @param prefixesToRegister List of IriBase to build the service for.
     */
    public void register(final List<IriBase> prefixesToRegister) {
        prefixesToRegister.forEach(pf -> {
            this.database.register(pf);
        });
    }

}
