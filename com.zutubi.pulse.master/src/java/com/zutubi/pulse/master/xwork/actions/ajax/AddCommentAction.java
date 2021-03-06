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

package com.zutubi.pulse.master.xwork.actions.ajax;

import com.zutubi.pulse.master.model.Comment;
import com.zutubi.pulse.master.model.CommentContainer;
import com.zutubi.pulse.master.model.User;

/**
 * Action allowing a user to add a message to a build result.
 */
public class AddCommentAction extends CommentActionBase
{
    private String message;

    public void setMessage(String message)
    {
        this.message = message;
    }

    @Override
    protected void updateContainer(CommentContainer container, User user)
    {
        Comment comment = new Comment(user.getLogin(), System.currentTimeMillis(), message);
        container.addComment(comment);
    }

}
