package com.zutubi.pulse.model;

import com.zutubi.pulse.core.model.Entity;
import com.zutubi.pulse.condition.*;
import com.zutubi.pulse.condition.antlr.NotifyConditionLexer;
import com.zutubi.pulse.condition.antlr.NotifyConditionParser;
import com.zutubi.pulse.condition.antlr.NotifyConditionTreeParser;
import com.zutubi.pulse.util.logging.Logger;

import java.io.StringReader;

import antlr.collections.AST;

/**
 * A subscription is a mapping from a project event to a contact point.  When
 * the event occurs, notification is sent to the contact point.
 *
 * @author jsankey
 */
public class Subscription extends Entity
{
    private static final Logger LOG = Logger.getLogger(Subscription.class);

    /**
     * The contact point to notify.
     */
    private ContactPoint contactPoint;

    /**
     * Condition to be satisfied before notifying.
     */
    private String condition;

    /**
     * The project to which this subscription is associated
     */
    private Project project;

    private NotifyCondition notifyCondition = null;

    /**
     * A reference to the systems notify condition factory, used for instantiating
     * the subscriptions condition.
     */
    private NotifyConditionFactory notifyFactory;

    //=======================================================================
    // Construction
    //=======================================================================

    /**
     * Constructor to be used by hibernate only.
     */
    public Subscription()
    {

    }

    /**
     * Constructs a new subscription connection the given event with the given
     * contact point.
     *
     * @param contactPoint the contact point to notify on the event
     */
    public Subscription(Project project, ContactPoint contactPoint)
    {
        this.project = project;
        this.contactPoint = contactPoint;
        this.condition = NotifyConditionFactory.TRUE;

        this.contactPoint.add(this);
    }

    //=======================================================================
    // Interface
    //=======================================================================

    /**
     * @return the contact point to notify
     */
    public ContactPoint getContactPoint()
    {
        return contactPoint;
    }

    /**
     * @param contactPoint
     */
    public void setContactPoint(ContactPoint contactPoint)
    {
        this.contactPoint = contactPoint;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }

    /**
     * Sets the given condition as that which must be satisfied before the
     * contact point should be notified.
     *
     * @param condition the condition to set.
     */
    public void setCondition(String condition)
    {
        this.condition = condition;
    }

    public String getCondition()
    {
        return this.condition;
    }

    /**
     * Indicates if the conditions for notifying the contact point are
     * satisfied by the given build model.
     *
     * @param result the build model to test the properties of
     * @return true iff the contact point should be notified for this model
     */
    public boolean conditionSatisfied(BuildResult result)
    {
        return getNotifyCondition().satisfied(result, contactPoint.getUser());
    }

    public void notify(BuildResult result)
    {
        getContactPoint().notify(result);
    }

    /**
     * The notify condition factory is a required resource, and used when checking
     * if the subscriptions 'nofitication condition' has been satisfied.
     *
     * @param notifyFactory
     */
    public void setNotifyConditionFactory(NotifyConditionFactory notifyFactory)
    {
        this.notifyFactory = notifyFactory;
    }

    public NotifyCondition getNotifyCondition()
    {
        if(notifyCondition == null)
        {
            // Need to parse our condition.
            try
            {
                NotifyConditionLexer lexer = new NotifyConditionLexer(new StringReader(condition));

                NotifyConditionParser parser = new NotifyConditionParser(lexer);
                parser.orexpression();
                AST t = parser.getAST();
                if(t == null)
                {
                    // Empty expression evals to true
                    notifyCondition = new TrueNotifyCondition();
                }
                else
                {
                    NotifyConditionTreeParser tree = new NotifyConditionTreeParser();
                    tree.setNotifyConditionFactory(notifyFactory);
                    notifyCondition = tree.cond(t);
                }
            }
            catch (Exception e)
            {
                LOG.severe("Unable to parse subscription condition '" + condition + "'");
                notifyCondition = new FalseNotifyCondition();
            }
        }

        return notifyCondition;
    }
}
