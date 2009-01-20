package com.zutubi.pulse.master.xwork.actions.project;

import com.zutubi.i18n.Messages;
import com.zutubi.pulse.core.model.NamedEntityComparator;
import com.zutubi.pulse.master.model.*;
import com.zutubi.pulse.master.tove.config.ConfigurationRegistry;
import com.zutubi.pulse.master.tove.config.user.ProjectsSummaryConfiguration;
import com.zutubi.tove.config.ConfigurationTemplateManager;
import com.zutubi.tove.config.TemplateHierarchy;
import com.zutubi.tove.config.TemplateNode;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Predicate;
import com.zutubi.util.Sort;
import com.zutubi.util.TruePredicate;

import java.util.*;

/**
 * A shared helper class that can take a set of projects and groups and output
 * corresponding {@link ProjectsModel} instances.
 */
public class ProjectsModelsHelper
{
    private ProjectManager projectManager;
    private BuildManager buildManager;
    private ConfigurationTemplateManager configurationTemplateManager;

    public List<ProjectsModel> createProjectsModels(ProjectsSummaryConfiguration configuration)
    {
        return createProjectsModels(configuration, new TruePredicate<Project>(), new TruePredicate<ProjectGroup>());
    }
    
    public List<ProjectsModel> createProjectsModels(ProjectsSummaryConfiguration configuration, Predicate<Project> projectPredicate, Predicate<ProjectGroup> groupPredicate)
    {
        List<Project> projects = CollectionUtils.filter(projectManager.getProjects(false), projectPredicate);
        List<ProjectGroup> groups = CollectionUtils.filter(projectManager.getAllProjectGroups(), groupPredicate);
        TemplateHierarchy hierarchy = configurationTemplateManager.getTemplateHierarchy(ConfigurationRegistry.PROJECTS_SCOPE);
        final Comparator<String> stringComparator = new Sort.StringComparator();

        Collections.sort(groups, new Comparator<ProjectGroup>()
        {
            public int compare(ProjectGroup o1, ProjectGroup o2)
            {
                return stringComparator.compare(o1.getName(), o2.getName());
            }
        });

        List<ProjectsModel> result = new LinkedList<ProjectsModel>();

        for (ProjectGroup group : groups)
        {
            Collection<Project> groupProjects = CollectionUtils.filter(group.getProjects(), projectPredicate);
            if (!groupProjects.isEmpty())
            {
                result.add(createModel(group.getName(), true, groupProjects, hierarchy, configuration));
                projects.removeAll(groupProjects);
            }
        }

        if (projects.size() > 0)
        {
            Collections.sort(projects, new NamedEntityComparator());

            // CIB-1550: Only label as ungrouped if there are some other
            // groups.
            Messages messages = Messages.getInstance(ProjectsModelsHelper.class);
            result.add(createModel(result.size() > 0 ? messages.format("projects.ungrouped") : messages.format("projects.all"), false, projects, hierarchy, configuration));
        }

        return result;
    }

    private ProjectsModel createModel(String name, boolean labelled, Collection<Project> projects, TemplateHierarchy hierarchy, ProjectsSummaryConfiguration configuration)
    {
        ProjectsModel model = new ProjectsModel(name, labelled);
        if (configuration.isHierarchyShown())
        {
            // The group can display all concrete projects plus all of their
            // ancestors (which may overlap).  The ancestors may not define the
            // label, but are included to prevent "holes" in the hierarchy.
            Set<String> includedInGroup = new HashSet<String>();
            for(Project p: projects)
            {
                TemplateNode node = hierarchy.getNodeById(p.getName());
                while(node != null)
                {
                    includedInGroup.add(node.getId());
                    node = node.getParent();
                }
            }

            processLevel(model, model.getRoot(), Arrays.asList(hierarchy.getRoot()), 0, includedInGroup, configuration);
        }
        else
        {
            for(Project p: projects)
            {
                model.getRoot().addChild(new ConcreteProjectModel(model, p, getBuilds(p, configuration), configuration.getBuildsPerProject()));
            }
        }

        return model;
    }

    private void processLevel(ProjectsModel group, TemplateProjectModel parentModel, List<TemplateNode> nodes, int depth, Set<String> includedInGroup, ProjectsSummaryConfiguration configuration)
    {
        for (TemplateNode node : nodes)
        {
            if (node.isConcrete() || depth >= configuration.getHiddenHierarchyLevels())
            {
                if (includedInGroup.contains(node.getId()))
                {
                    ProjectModel model;
                    String name = node.getId();
                    if (node.isConcrete())
                    {
                        Project project = projectManager.getProject(name, true);
                        List<BuildResult> builds = getBuilds(project, configuration);
                        model = new ConcreteProjectModel(group, project, builds, configuration.getBuildsPerProject());
                    }
                    else
                    {
                        TemplateProjectModel template = new TemplateProjectModel(group, name);
                        processLevel(group, template, node.getChildren(), depth + 1, includedInGroup, configuration);
                        model = template;
                    }

                    parentModel.addChild(model);
                }
            }
            else
            {
                processLevel(group, parentModel, node.getChildren(), depth + 1, includedInGroup, configuration);
            }
        }
    }

    private List<BuildResult> getBuilds(Project project, ProjectsSummaryConfiguration configuration)
    {
        // We need to retrieve at least 2 to determine the health when the
        // latest build is in progress (this assumes one in progress build
        // per project).
        return buildManager.getLatestBuildResultsForProject(project, Math.max(2, configuration.getBuildsPerProject()));
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public void setConfigurationTemplateManager(ConfigurationTemplateManager configurationTemplateManager)
    {
        this.configurationTemplateManager = configurationTemplateManager;
    }
}