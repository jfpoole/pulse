/* Copyright 2017 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.pulse.master.upgrade.tasks;

import com.zutubi.pulse.master.util.monitor.TaskException;
import com.zutubi.tove.transaction.TransactionManager;
import com.zutubi.tove.transaction.UserTransaction;
import com.zutubi.tove.type.record.*;

import java.util.Map;

/**
 * Introduces stubs for the new dependency configurations.
 */
public class DependenciesUpgradeTask extends AbstractUpgradeTask
{
    private static final String PATH_PATTERN_ALL_PROJECTS = "projects/*";

    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_PROPERTIES = "properties";
    private static final String PROPERTY_DEPENDENCIES = "dependencies";
    private static final String PROPERTY_PUBLICATIONS = "publications";
    private static final String PROPERTY_PUBLICATION_PATTERN = "publicationPattern";
    private static final String PROPERTY_RETRIEVAL_PATTERN = "retrievalPattern";

    private static final String DEPENDENCY_TRIGGER_NAME = "dependency trigger";

    private static final String TYPE_DEPENDENCIES_CONFIGURATION = "zutubi.dependenciesConfiguration";
    private static final String TYPE_DEPENDENCY_TRIGGER = "zutubi.dependentBuildTriggerConfig";

    private RecordManager recordManager;

    private TransactionManager transactionManager;

    public boolean haltOnFailure()
    {
        return true;
    }

    public void execute() throws TaskException
    {
        // Ensure that either all of the dependencies configuration changes are committed, or none.
        UserTransaction txn = new UserTransaction(transactionManager);
        txn.begin();

        try
        {
            MutableRecord dependenciesSkeleton = new MutableRecordImpl();
            dependenciesSkeleton.setSymbolicName(TYPE_DEPENDENCIES_CONFIGURATION);
            dependenciesSkeleton.put(PROPERTY_DEPENDENCIES, new MutableRecordImpl());
            dependenciesSkeleton.put(PROPERTY_PUBLICATIONS, new MutableRecordImpl());

            MutableRecord stagePublicationsSkeleton = new MutableRecordImpl();

            MutableRecord triggerSkeleton = new MutableRecordImpl();
            triggerSkeleton.setSymbolicName(TYPE_DEPENDENCY_TRIGGER);
            triggerSkeleton.put(PROPERTY_NAME, DEPENDENCY_TRIGGER_NAME);
            triggerSkeleton.put(PROPERTY_PROPERTIES, new MutableRecordImpl());

            // for all projects, add the skeletons.
            RecordLocator allProjectsLocator = RecordLocators.newPathPattern(PATH_PATTERN_ALL_PROJECTS);
            Map<String, Record> projects = allProjectsLocator.locate(recordManager);

            for (Map.Entry<String, Record> entry : projects.entrySet())
            {
                String projectPath = entry.getKey();
                MutableRecord projectEntry = entry.getValue().copy(true, true);
                recordManager.insert(projectPath + "/dependencies", dependenciesSkeleton.copy(true, true));

                MutableRecord stagesEntry = (MutableRecord) projectEntry.get("stages");
                for (String stageName : stagesEntry.keySet())
                {
                    recordManager.insert(projectPath + "/stages/" + stageName + "/publications", stagePublicationsSkeleton.copy(true, true));
                }

                // Triggers are only associated with concrete projects.
                if (!projectEntry.containsMetaKey(TemplateRecord.TEMPLATE_KEY))
                {
                    recordManager.insert(projectPath + "/triggers/" + triggerSkeleton.get(PROPERTY_NAME), triggerSkeleton.copy(true, true));
                }
            }

            // for global project, add specific values.
            String globalDependenciesPath = "projects/" + getGlobalProjectName() + "/dependencies";
            Record globalDependencies = recordManager.select(globalDependenciesPath);
            MutableRecord globalDependenciesEntry = globalDependencies.copy(true, true);
            globalDependenciesEntry.put(PROPERTY_PUBLICATION_PATTERN, "build/[artifact].[ext]");
            globalDependenciesEntry.put(PROPERTY_RETRIEVAL_PATTERN, "lib/[artifact].[ext]");
            recordManager.update(globalDependenciesPath, globalDependenciesEntry);

            txn.commit();
            
        }
        catch (RuntimeException e)
        {
            txn.rollback();
            throw e;
        }
        catch (Throwable t)
        {
            txn.rollback();
            throw new RuntimeException(t);
        }
    }

    private String getGlobalProjectName()
    {
        TemplatedScopeDetails details = new TemplatedScopeDetails("projects", recordManager);
        return details.getHierarchy().getRoot().getId();
    }

    public void setRecordManager(RecordManager recordManager)
    {
        this.recordManager = recordManager;
    }

    public void setPulseTransactionManager(TransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }
}
