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

package com.zutubi.pulse.acceptance.components.pulse.server;

import com.zutubi.pulse.acceptance.SeleniumBrowser;
import com.zutubi.pulse.acceptance.components.table.ContentTable;
import com.zutubi.pulse.acceptance.pages.MessageDialog;
import com.zutubi.pulse.acceptance.pages.YesNoDialog;
import com.zutubi.pulse.core.engine.api.ResultState;
import org.openqa.selenium.By;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Corresponds to the Zutubi.pulse.server.ActiveBuildsTable JS component.
 */
public class ActiveBuildsTable extends ContentTable
{
    public ActiveBuildsTable(SeleniumBrowser browser, String id)
    {
        super(browser, id);
    }

    /**
     * Returns the number of builds displayed by this table.
     *
     * @return the number of builds displayed
     */
    public long getBuildCount()
    {
        return getDataLength();
    }

    /**
     * Returns the build at the given index.  Note this is an index into the
     * builds, not the table rows (rows also include stages).
     * 
     * @param buildIndex zero-based index of the build to retrieve
     * @return the build at the given index
     */
    public ActiveBuild getBuild(int buildIndex)
    {
        // We need to ensure the stage rows are visible to get their text.
        expandAll();
        
        int rowIndex = getRowIndexForBuild(buildIndex);
        String name = getCellContents(rowIndex, 0);
        String[] ownerId = name.split(" :: ");
        ResultState state = ResultState.fromPrettyString(getCellContents(rowIndex, 1));
        Map<String, String> details = parseDetails(getCellContents(rowIndex, 2));
        String actions = getCellContents(rowIndex, 3);

        ActiveBuild build = new ActiveBuild(ownerId[0], Integer.parseInt(ownerId[1].substring(6)), state, details.get("revision"), details.get("reason"), actions.contains("cancel"));
        long stageCount = getStageCount(buildIndex);
        for (int i = 0; i < stageCount; i++)
        {
            build.addStage(getStage(rowIndex + i + 1));
        }
        return build;
    }

    private void expandAll()
    {
        browser.evaluateScript(getComponentJS() + ".expandAll()");
    }

    private ActiveStage getStage(int rowIndex)
    {
        String name = getCellContents(rowIndex, 0);
        ResultState state = ResultState.fromPrettyString(getCellContents(rowIndex, 1));
        Map<String, String> details = parseDetails(getCellContents(rowIndex, 2));
        
        return new ActiveStage(name, state, details.get("recipe"), details.get("agent"));
    }

    private int getRowIndexForBuild(int buildIndex)
    {
        int rowIndex = 1;
        for (int i = 0; i < buildIndex; i++)
        {
            rowIndex += 1 + getStageCount(i);
        }
        
        return rowIndex;
    }

    private long getStageCount(int buildIndex)
    {
        return (Long) browser.evaluateScript("return " + getComponentJS() + ".data[" + buildIndex + "].stages.length;");
    }

    /**
     * Clicks the cancel link for the build at the given index.
     * 
     * @param index zero-based index of the build to cancel
     * @return the dialog popped to confirm deletion
     */
    public MessageDialog clickCancel(int index)
    {
        Long id = (Long) browser.evaluateScript("return " + getComponentJS() + ".data[" + index + "].id;");
        browser.click(By.id("cancel-" + id + "-button"));
        return new YesNoDialog(browser);
    }

    private Map<String, String> parseDetails(String details)
    {
        Map<String, String> result = new HashMap<String, String>();
        String[] pieces = details.split(" // ");
        for (String piece: pieces)
        {
            String[] keyValue = piece.split(": ");
            result.put(keyValue[0], keyValue[1]);
        }
        
        return result;
    }

    /**
     * Holds information about an active build.
     */
    public static class ActiveBuild
    {
        public String owner;
        public int id;
        public ResultState status;
        public String revision;
        public String reason;
        public boolean canCancel;
        public List<ActiveStage> stages = new LinkedList<ActiveStage>();

        /**
         * Creates an active build.
         * 
         * @param owner     owner of the build (project or user)
         * @param id        build number
         * @param status    current build state
         * @param revision  build revision string
         * @param reason    build reason (e.g. manual trigger by 'foo')
         * @param canCancel if true, a cancel link is present for the build
         */
        public ActiveBuild(String owner, int id, ResultState status, String revision, String reason, boolean canCancel)
        {
            this.owner = owner;
            this.id = id;
            this.status = status;
            this.revision = revision;
            this.reason = reason;
            this.canCancel = canCancel;
        }
        
        public void addStage(ActiveStage stage)
        {
            stages.add(stage);
        }
    }

    /**
     * Holds information about an active build stage.
     */
    public static class ActiveStage
    {
        public String name;
        public ResultState status;
        public String recipe;
        public String agent;

        /**
         * Creates a new active stage.
         * 
         * @param name   the stage's name
         * @param status current stage status
         * @param recipe the recipe being built (may be "[default]")
         * @param agent  the agent the stage is running on (may be null for
         *               pending stages)
         */
        public ActiveStage(String name, ResultState status, String recipe, String agent)
        {
            this.name = name;
            this.status = status;
            this.recipe = recipe;
            this.agent = agent;
        }
    }
    
}
