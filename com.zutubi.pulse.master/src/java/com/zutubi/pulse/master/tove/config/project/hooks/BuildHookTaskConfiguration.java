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

package com.zutubi.pulse.master.tove.config.project.hooks;

import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.RecipeResultNode;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.config.api.Configuration;

/**
 * A build hook task is the action performed when a build hook is triggered.
 */
@SymbolicName("zutubi.buildHookTask")
public interface BuildHookTaskConfiguration extends Configuration
{
    void execute(ExecutionContext context, BuildResult buildResult, RecipeResultNode resultNode, boolean onAgent) throws Exception;
}
