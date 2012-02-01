package com.zutubi.pulse.master.upgrade.tasks;

import com.zutubi.util.CollectionUtils;
import com.zutubi.util.UnaryFunction;

import java.util.Arrays;
import java.util.List;

/**
 * Adds a separate permission for project cleanup, updating ACLs that have the
 * existing trigger permission.
 */
public class AddProjectCleanPermissionUpgradeTask extends AbstractRecordPropertiesUpgradeTask
{
    public boolean haltOnFailure()
    {
        return true;
    }

    @Override
    protected RecordLocator getRecordLocator()
    {
        return RecordLocators.newPathPattern("projects/*/permissions/*");
    }

    @Override
    protected List<RecordUpgrader> getRecordUpgraders()
    {
        return Arrays.asList(RecordUpgraders.newEditProperty("allowedActions", new UnaryFunction<Object, Object>()
        {
            public Object process(Object o)
            {
                if (o != null && o instanceof String[])
                {
                    String[] allowedActions = (String[]) o;
                    if (CollectionUtils.contains(allowedActions, "trigger"))
                    {
                        String[] editedActions = new String[allowedActions.length + 1];
                        System.arraycopy(allowedActions, 0, editedActions, 0, allowedActions.length);
                        editedActions[editedActions.length - 1] = "clean";
                        o = editedActions;
                    }
                }

                return o;
            }
        }));
    }
}
