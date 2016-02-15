/*
 * Copyright (C) 2007-2014, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.md;

import com.gooddata.AbstractPollHandler;
import com.gooddata.AbstractService;
import com.gooddata.FutureResult;
import com.gooddata.GoodDataException;
import com.gooddata.GoodDataRestException;
import com.gooddata.PollResult;
import com.gooddata.gdc.TaskStatus;
import com.gooddata.gdc.UriResponse;
import com.gooddata.md.maintenance.MaintenanceException;
import com.gooddata.md.maintenance.PartialMdArtifact;
import com.gooddata.md.maintenance.PartialMdExport;
import com.gooddata.md.maintenance.PartialMdImport;
import com.gooddata.md.report.ReportDefinition;
import com.gooddata.project.Project;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gooddata.util.Validate.noNullElements;
import static com.gooddata.util.Validate.notEmpty;
import static com.gooddata.util.Validate.notNull;
import static java.util.Arrays.asList;

/**
 * Query, create and update project metadata - attributes, facts, metrics, reports,...
 */
public class MetadataService extends AbstractService {

    public MetadataService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    /**
     * Create metadata object in given project
     *
     * @param project project
     * @param obj     metadata object to be created
     * @param <T>     type of the object to be created
     * @return new metadata object
     * @throws com.gooddata.md.ObjCreateException   if creation failed
     * @throws com.gooddata.md.ObjNotFoundException if new metadata object not found after creation
     * @throws com.gooddata.GoodDataRestException   if GoodData REST API returns unexpected status code when getting
     *                                              the new object
     * @throws com.gooddata.GoodDataException       if no response from API or client-side HTTP error when getting the new object
     */
    @SuppressWarnings("unchecked")
    public <T extends Obj> T createObj(Project project, T obj) {
        notNull(project, "project");
        notNull(obj, "obj");

        final UriResponse response;
        try {
            response = restTemplate.postForObject(Obj.URI, obj, UriResponse.class, project.getId());
        } catch (GoodDataRestException | RestClientException e) {
            throw new ObjCreateException(obj, e);
        }

        if (response == null) {
            throw new ObjCreateException("empty response from API call", obj);
        }
        return getObjByUri(response.getUri(), (Class<T>) obj.getClass());
    }

    /**
     * Get metadata object by URI (format is <code>/gdc/md/{PROJECT_ID}/obj/{OBJECT_ID}</code>)
     *
     * @param uri URI in format <code>/gdc/md/{PROJECT_ID}/obj/{OBJECT_ID}</code>
     * @param cls class of the resulting object
     * @param <T> type of the object to be returned
     * @return the metadata object
     * @throws com.gooddata.md.ObjNotFoundException if metadata object not found
     * @throws com.gooddata.GoodDataRestException   if GoodData REST API returns unexpected status code
     * @throws com.gooddata.GoodDataException       if no response from API or client-side HTTP error
     */
    public <T extends Obj> T getObjByUri(String uri, Class<T> cls) {
        notNull(uri, "uri");
        notNull(cls, "cls");
        try {
            final T result = restTemplate.getForObject(uri, cls);

            if (result != null) {
                return result;
            } else {
                throw new GoodDataException("empty response from API call");
            }
        } catch (GoodDataRestException e) {
            if (HttpStatus.NOT_FOUND.value() == e.getStatusCode()) {
                throw new ObjNotFoundException(uri, cls, e);
            } else {
                throw e;
            }
        } catch (RestClientException e) {
            throw new GoodDataException("Unable to get " + cls.getSimpleName().toLowerCase() + " " + uri, e);
        }
    }

    /**
     * Update given metadata object.
     *
     * @param obj object to update
     * @param <T> type of the updated object
     * @return updated metadata object
     * @throws com.gooddata.md.ObjUpdateException in case of error
     */
    @SuppressWarnings("unchecked")
    public <T extends Updatable> T updateObj(T obj) {
        notNull(obj, "obj");
        try {
            restTemplate.put(obj.getUri(), obj);
            return getObjByUri(obj.getUri(), (Class<T>) obj.getClass());
        } catch (GoodDataException | RestClientException e) {
            throw new ObjUpdateException(obj, e);
        }
    }

    /**
     * Remove metadata object URI
     *
     * @param obj metadata object to remove
     * @throws com.gooddata.md.ObjNotFoundException if metadata object not found
     * @throws com.gooddata.GoodDataRestException   if GoodData REST API returns unexpected status code
     * @throws com.gooddata.GoodDataException       if no response from API or client-side HTTP error
     */
    public void removeObj(Obj obj) {
        notNull(obj, "obj");
        try {
            restTemplate.delete(obj.getUri());
        } catch (GoodDataRestException e) {
            if (HttpStatus.NOT_FOUND.value() == e.getStatusCode()) {
                throw new ObjNotFoundException(obj);
            } else {
                throw e;
            }
        } catch (RestClientException e) {
            throw new GoodDataException("Unable to remove " + obj.getClass().getSimpleName().toLowerCase() + " " + obj.getUri(), e);
        }
    }

    /**
     * Remove metadata object by URI (format is <code>/gdc/md/{PROJECT_ID}/obj/{OBJECT_ID}</code>)
     *
     * @param uri URI in format <code>/gdc/md/{PROJECT_ID}/obj/{OBJECT_ID}</code>
     * @throws com.gooddata.md.ObjNotFoundException if metadata object not found
     * @throws com.gooddata.GoodDataRestException   if GoodData REST API returns unexpected status code
     * @throws com.gooddata.GoodDataException       if no response from API or client-side HTTP error
     */
    public void removeObjByUri(String uri) {
        notNull(uri, "uri");
        try {
            restTemplate.delete(uri);
        } catch (GoodDataRestException e) {
            if (HttpStatus.NOT_FOUND.value() == e.getStatusCode()) {
                throw new ObjNotFoundException(uri);
            } else {
                throw e;
            }
        } catch (RestClientException e) {
            throw new GoodDataException("Unable to remove " + uri, e);
        }
    }

    /**
     * Get metadata object by id.
     *
     * @param project project where to search for the object
     * @param id      id of the object
     * @param cls     class of the resulting object
     * @param <T>     type of the object to be returned
     * @return the metadata object
     * @throws com.gooddata.md.ObjNotFoundException if metadata object not found
     * @throws com.gooddata.GoodDataRestException   if GoodData REST API returns unexpected status code
     * @throws com.gooddata.GoodDataException       if no response from API or client-side HTTP error
     */
    public <T extends Obj> T getObjById(Project project, String id, Class<T> cls) {
        notNull(project, "project");
        notNull(id, "id");
        notNull(cls, "cls");
        return getObjByUri(Obj.OBJ_TEMPLATE.expand(project.getId(), id).toString(), cls);
    }

    /**
     * Get metadata object URI by restrictions like identifier, title or summary.
     *
     * @param project      project where to search for the object
     * @param cls          class of the resulting object
     * @param restrictions query restrictions
     * @param <T>          type of the object to be returned
     * @return the URI of metadata object
     * @throws com.gooddata.md.ObjNotFoundException  if metadata object not found
     * @throws com.gooddata.md.NonUniqueObjException if more than one object corresponds to search restrictions
     */
    public <T extends Queryable> String getObjUri(Project project, Class<T> cls, Restriction... restrictions) {
        final Collection<String> results = findUris(project, cls, restrictions);
        if (results == null || results.isEmpty()) {
            throw new ObjNotFoundException(cls);
        } else if (results.size() != 1) {
            throw new NonUniqueObjException(cls, results);
        }
        return results.iterator().next();
    }

    /**
     * Get metadata object by restrictions like identifier, title or summary.
     *
     * @param project      project where to search for the object
     * @param cls          class of the resulting object
     * @param restrictions query restrictions
     * @param <T>          type of the object to be returned
     * @return metadata object
     * @throws com.gooddata.md.ObjNotFoundException  if metadata object not found
     * @throws com.gooddata.md.NonUniqueObjException if more than one object corresponds to search restrictions
     */
    public <T extends Queryable> T getObj(Project project, Class<T> cls, Restriction... restrictions) {
        final Collection<String> results = findUris(project, cls, restrictions);
        if (results == null || results.isEmpty()) {
            throw new ObjNotFoundException(cls);
        } else if (results.size() != 1) {
            throw new NonUniqueObjException(cls, results);
        }
        return getObjByUri(results.iterator().next(), cls);
    }

    /**
     * Find metadata by restrictions like identifier, title or summary.
     *
     * @param project      project where to search for the metadata
     * @param cls          class of searched metadata
     * @param restrictions query restrictions
     * @param <T>          type of the metadata referenced in returned entries
     * @return the collection of metadata entries
     * @throws com.gooddata.GoodDataException if unable to query metadata
     */
    public <T extends Queryable> Collection<Entry> find(Project project, Class<T> cls, Restriction... restrictions) {
        notNull(project, "project");
        notNull(cls, "cls");

        final String type = cls.getSimpleName().toLowerCase() +
                (cls.isAssignableFrom(ReportDefinition.class) ? "" : "s");
        try {
            final Query queryResult = restTemplate.getForObject(Query.URI, Query.class, project.getId(), type);

            if (queryResult != null && queryResult.getEntries() != null) {
                return filterEntries(queryResult.getEntries(), restrictions);
            } else {
                throw new GoodDataException("empty response from API call");
            }
        } catch (RestClientException e) {
            throw new GoodDataException("Unable to query metadata: " + type, e);
        }
    }

    private Collection<Entry> filterEntries(Collection<Entry> entries, Restriction... restrictions) {
        if (restrictions == null || restrictions.length == 0) {
            return entries;
        }
        final Collection<Entry> result = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            for (Restriction restriction : restrictions) {
                switch (restriction.getType()) {
                    case IDENTIFIER:
                        if (restriction.getValue().equals(entry.getIdentifier())) result.add(entry);
                        break;
                    case TITLE:
                        if (restriction.getValue().equals(entry.getTitle())) result.add(entry);
                        break;
                    case SUMMARY:
                        if (restriction.getValue().equals(entry.getSummary())) result.add(entry);
                        break;
                }
            }
        }
        return result;
    }

    /**
     * Find metadata URIs by restrictions like identifier, title or summary.
     *
     * @param project      project where to search for the metadata
     * @param cls          class of searched metadata
     * @param restrictions query restrictions
     * @param <T>          type of the metadata referenced by returned URIs
     * @return the collection of metadata URIs
     * @throws com.gooddata.GoodDataException if unable to query metadata
     */
    public <T extends Queryable> Collection<String> findUris(Project project,
                                                             Class<T> cls,
                                                             Restriction... restrictions) {
        final Collection<Entry> entries = find(project, cls, restrictions);
        final Collection<String> result = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            result.add(entry.getLink());
        }
        return result;
    }

    /**
     * Find all objects which use the given object.
     * @param project project
     * @param obj     object to find using objects for
     * @param nearest find nearest objects only
     * @param types   what types (categories) to search for (for example 'reportDefinition', 'report', 'tableDataLoad',
     *                'table'...)
     * @return objects using given objects.
     */
    public Collection<Entry> usedBy(Project project, Obj obj, boolean nearest, Class<? extends Obj>... types) {
        notNull(obj, "obj");
        return usedBy(project, obj.getUri(), nearest, types);
    }

    /**
     * Find all objects which use the given object.
     * @param project project
     * @param uri     URI of object to find using objects for
     * @param nearest find nearest objects only
     * @param types   what types (categories) to search for (for example 'reportDefinition', 'report', 'tableDataLoad',
     *                'table'...)
     * @return objects using given objects.
     * @see #usedBy(Project, Collection, boolean, Class[])
     */
    public Collection<Entry> usedBy(Project project, String uri, boolean nearest, Class<? extends Obj>... types) {
        notNull(uri, "uri");
        notNull(project, "project");

        final Collection<Usage> usages = usedBy(project, asList(uri), nearest, types);
        return usages.size() > 0 ? usages.iterator().next().getUsedBy() : Collections.<Entry>emptyList();
    }

    /**
     * Find all objects which use the given objects. Batch alternative to {@link #usedBy(Project, String, boolean, Class[])}
     * @param project project
     * @param uris    URIs of object to find using objects for
     * @param nearest find nearest objects only
     * @param types   what types (categories) to search for (for example 'reportDefinition', 'report', 'tableDataLoad',
     *                'table'...), returns all objects if no type is provided
     * @return objects usages
     * @see #usedBy(Project, String, boolean, Class[])
     */
    public Collection<Usage> usedBy(Project project, Collection<String> uris, boolean nearest, Class<? extends Obj>... types) {
        notNull(uris, "uris");
        notNull(project, "project");

        final UseMany response;
        try {
            response = restTemplate.postForObject(InUseMany.USEDBY_URI, new InUseMany(uris, nearest, types), UseMany.class, project.getId());
        } catch (GoodDataRestException | RestClientException e) {
            throw new GoodDataException("Unable to find objects.", e);
        }
        final List<Usage> usages = new ArrayList<>(uris.size());
        final Collection<UseManyEntries> useManyEntries = response.getUseMany();
        for (UseManyEntries useMany : useManyEntries) {
            usages.add(new Usage(useMany.getUri(), useMany.getEntries()));
        }
        return usages;
    }

    /**
     * Find metadata URIs by restrictions. Identifier is the only supported restriction.
     *
     * @param project      project where to search for the metadata
     * @param restrictions query restrictions
     * @return the collection of metadata URIs
     * @throws com.gooddata.GoodDataException if unable to query metadata
     */
    public Collection<String> findUris(Project project, Restriction... restrictions) {
        notNull(project, "project" );
        noNullElements(restrictions, "restrictions");

        final List<String> ids = new ArrayList<>(restrictions.length);
        for (Restriction restriction: restrictions) {
            if (!Restriction.Type.IDENTIFIER.equals(restriction.getType())) {
                throw new IllegalArgumentException("All restrictions have to be of type " + Restriction.Type.IDENTIFIER);
            }
            ids.add(restriction.getValue());
        }

        final IdentifiersAndUris response = getUrisForIdentifiers(project, ids);

        final List<String> uris = new ArrayList<>();
        for (IdentifierAndUri idAndUri : response.getIdentifiers()) {
            uris.add(idAndUri.getUri());
        }

        return uris;
    }

    /**
     * Find metadata URIs for given identifiers.
     *
     * @param project      project where to search for the metadata
     * @param identifiers query restrictions
     * @return the map of identifiers as keys and metadata URIs as values
     * @throws com.gooddata.GoodDataException if unable to query metadata
     * @see #findUris(Project, Restriction...)
     */
    public Map<String, String> identifiersToUris(Project project, Collection<String> identifiers) {
        notNull(project, "project" );
        noNullElements(identifiers, "identifiers");

        final IdentifiersAndUris response = getUrisForIdentifiers(project, identifiers);

        final Map<String, String> identifiersToUris = new HashMap<>();
        for (IdentifierAndUri idAndUri : response.getIdentifiers()) {
            identifiersToUris.put(idAndUri.getIdentifier(), idAndUri.getUri());
        }

        return Collections.unmodifiableMap(identifiersToUris);
    }

    /**
     * Fetches attribute elements for given attribute using default display form.
     *
     * @param attribute attribute to fetch elements for
     * @return attribute elements or empty set if there is no link for elements in default display form
     */
    public List<AttributeElement> getAttributeElements(Attribute attribute) {
        notNull(attribute, "attribute");

        return getAttributeElements(attribute.getDefaultDisplayForm());
    }

    /**
     * Fetches attribute elements by given display form.
     *
     * @param displayForm display form to fetch attributes for
     * @return attribute elements or empty set if there is no link for elements
     */
    public List<AttributeElement> getAttributeElements(DisplayForm displayForm) {
        notNull(displayForm, "displayForm");

        final String elementsLink = displayForm.getElementsLink();
        if (StringUtils.isEmpty(elementsLink)) {
            return Collections.emptyList();
        }

        try {
            final AttributeElements attributeElements = restTemplate.getForObject(elementsLink, AttributeElements.class);
            return attributeElements.getElements();
        } catch (GoodDataRestException | RestClientException e) {
            throw new GoodDataException("Unable to get attribute elements from " + elementsLink + ".", e);
        }
    }

    /**
     * Exports partial metadata from project and returns token identifying this export
     *
     * @param project project from which metadata should be exported
     * @param mdObjectsUris list of uris to metadata objects which should be exported
     * @param crossDataCenterExport whether export should be usable in any Data Center
     * @param exportAttributeProperties whether to add necessary data to be able to clone attribute properties
     * @return {@link FutureResult} of the task containing token identifying partial export after the task is completed
     */
    public FutureResult<String> partialExport(Project project, List<String> mdObjectsUris, boolean crossDataCenterExport, boolean exportAttributeProperties) {
        notEmpty(mdObjectsUris, "mdObjectUris");

        final PartialMdArtifact partialMdArtifact;
        try {
            final PartialMdExport request = new PartialMdExport(mdObjectsUris, crossDataCenterExport, exportAttributeProperties);
            partialMdArtifact = restTemplate.postForObject(PartialMdExport.URI, request, PartialMdArtifact.class, project.getId());
        } catch (GoodDataRestException | RestClientException e) {
            throw new GoodDataException("Unable to export metadata from objects " + mdObjectsUris + ".", e);
        }

        return new PollResult<>(this, new AbstractPollHandler<TaskStatus, String>(partialMdArtifact.getStatus().getUri(), TaskStatus.class, String.class) {
            @Override
            public void handlePollResult(TaskStatus pollResult) {
                if (!pollResult.isSuccess()) {
                    throw new MaintenanceException("Partial metadata export failed with errors: " + pollResult.getMessages());
                }
                setResult(partialMdArtifact.getToken());
            }

            @Override
            public void handlePollException(GoodDataRestException e) {
                throw new MaintenanceException("Unable to to export partial metadata.", e);
            }
        });
    }

    /**
     * Imports partial metadata to project based on given token
     *
     * @param project project to which metadata should be imported
     * @param token token identifying metadata partially exported from another project
     * @param overwriteNewer overwrite UDM/ADM objects without checking modification time
     * @param updateLDMObjects overwrite related LDM objects name, description and tags
     * @param importAttributeProperties clone following attribute properties:
     *                                  <ul>
     *                                      <li>for attribute - import drillDownStepAttributeDF setting</li>
     *                                      <li>for attributeDisplayForm - import type setting</li>
     *                                  </ul>
     *                                  it also implies 'updateLDMObjects = true' for all mentioned types;
     *                                  it will not reliably work (can fail) for exports without 'exportAttributeProperties = true'
     * @return {@link FutureResult} of the task
     */
    public FutureResult<Void> partialImport(Project project, String token, boolean overwriteNewer,
            boolean updateLDMObjects, boolean importAttributeProperties) {
        notEmpty(token, "token");

        final UriResponse importResponse;
        try {
            final PartialMdImport request = new PartialMdImport(token, overwriteNewer, updateLDMObjects, importAttributeProperties);
            importResponse = restTemplate.postForObject(PartialMdImport.URI, request, UriResponse.class, project.getId());
        } catch (GoodDataRestException | RestClientException e) {
            throw new GoodDataException("Unable to import partial metadata to project '" + project.getUri() + "' with token '" + token + "'.", e);
        }

        return new PollResult<>(this, new AbstractPollHandler<TaskStatus, Void>(importResponse.getUri(), TaskStatus.class, Void.class) {
            @Override
            public void handlePollResult(TaskStatus pollResult) {
                if (!pollResult.isSuccess()) {
                    throw new MaintenanceException("Partial metadata import failed with errors: " + pollResult.getMessages());
                }
                setResult(null);
            }

            @Override
            public void handlePollException(GoodDataRestException e) {
                throw new MaintenanceException("Unable to import partial metadata.", e);
            }
        });
    }

    private IdentifiersAndUris getUrisForIdentifiers(final Project project, final Collection<String> identifiers) {
        final IdentifiersAndUris response;
        try {
            response = restTemplate.postForObject(IdentifiersAndUris.URI, new IdentifierToUri(identifiers), IdentifiersAndUris.class, project.getId());
        } catch (GoodDataRestException | RestClientException e) {
            throw new GoodDataException("Unable to get URIs from identifiers.", e);
        }
        return response;
    }
}
