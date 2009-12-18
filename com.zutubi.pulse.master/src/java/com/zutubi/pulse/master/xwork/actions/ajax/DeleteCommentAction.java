package com.zutubi.pulse.master.xwork.actions.ajax;

import com.zutubi.i18n.Messages;
import com.zutubi.pulse.core.model.EntityWithIdPredicate;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.Comment;
import com.zutubi.pulse.master.model.User;
import com.zutubi.tove.security.AccessManager;
import com.zutubi.util.CollectionUtils;

/**
 * Action allowing a user to delete a comment on a build result.
 */
public class DeleteCommentAction extends CommentActionBase
{
    private static final Messages I18N = Messages.getInstance(DeleteCommentAction.class);

    private long commentId;

    public void setCommentId(long commentId)
    {
        this.commentId = commentId;
    }

    @Override
    protected void updateBuild(BuildResult build, User user)
    {
        Comment comment = CollectionUtils.find(build.getComments(), new EntityWithIdPredicate<Comment>(commentId));
        if (comment == null)
        {
            throw new IllegalArgumentException(I18N.format("unknown.comment", commentId));
        }

        accessManager.ensurePermission(AccessManager.ACTION_DELETE, comment);
        build.removeComment(comment);
    }
}