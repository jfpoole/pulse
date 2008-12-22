package com.zutubi.pulse.model.persistence;

import com.zutubi.pulse.core.model.*;
import com.zutubi.pulse.model.*;

import java.util.Date;
import java.util.List;

public interface BuildResultDao extends EntityDao<BuildResult>
{
    void save(RecipeResultNode node);

    void save(PersistentName name);

    void save(RecipeResult result);

    void save(CommandResult result);

    List<BuildResult> queryBuilds(Project[] projects, ResultState[] states, PersistentName[] specs, long earliestStartTime, long latestStartTime, Boolean hasWorkDir, int first, int max, boolean mostRecentFirst);

    List<BuildResult> queryBuildsWithMessages(Project[] projects, PersistentName[] specs, Feature.Level level, int max);

    List<BuildResult> findLatestByProject(Project project, int max);

    List<BuildResult> findSinceByProject(Project project, PersistentName spec, Date since);

    List<BuildResult> findLatestByProject(Project project, int first, int max);

    List<BuildResult> findLatestByProject(Project project, ResultState[] states, PersistentName spec, int first, int max);

    List<BuildResult> findLatestCompleted(Project project, PersistentName spec, int first, int max);

    BuildResult findPreviousBuildResult(BuildResult result);

    List<BuildResult> findOldestByProject(Project project, ResultState[] states, int max, boolean includePersonal);

    BuildResult findByProjectAndNumber(final Project project, final long number);

    BuildResult findByUserAndNumber(User user, long id);

    CommandResult findCommandResult(long id);

    RecipeResultNode findRecipeResultNode(long id);

    RecipeResult findRecipeResult(long id);

    int getBuildCount(Project project, ResultState[] states, PersistentName spec);

    int getBuildCount(Project project, ResultState[] states, Boolean hasWorkDir);

    int getBuildCount(PersistentName spec, long after, long upTo);

    List<PersistentName> findAllSpecifications(Project project);

    List<PersistentName> findAllSpecificationsForProjects(Project[] projects);

    List<BuildResult> querySpecificationBuilds(Project project, PersistentName spec, ResultState[] states, long lowestNumber, long highestNumber, int first, int max, boolean mostRecentFirst, boolean initialise);

    List<BuildResult> findByUser(User user);

    List<BuildResult> getLatestByUser(User user, ResultState[] states, int max);

    int getCompletedResultCount(User user);

    List<BuildResult> getOldestCompletedBuilds(User user, int limit);

    List<BuildResult> getOldestBuilds(Project project, ResultState[] states, Boolean hasWorkDir, int limit);

    RecipeResultNode findResultNodeByResultId(long id);

    BuildResult findLatest();

    CommandResult findCommandResultByArtifact(long artifactId);

    BuildResult findLatestByBuildSpec(BuildSpecification spec);

    BuildResult findLatestSuccessfulBySpecification(BuildSpecification spec);

    BuildResult findLatestSuccessfulByProject(Project project);

    BuildResult findLatestSuccessful();
}