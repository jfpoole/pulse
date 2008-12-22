package com.zutubi.pulse.web.wizard;

import com.zutubi.pulse.util.logging.Logger;
import com.zutubi.pulse.xwork.TextProviderSupport;
import com.opensymphony.xwork.*;
import com.opensymphony.xwork.validator.DefaultActionValidatorManager;
import com.opensymphony.xwork.validator.ValidationException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * <class-comment/>
 */
public class BaseWizard implements Wizard
{
    /**
     * The wizard state history. States on this stack have been previously traversed by
     * the user.
     */
    private Stack<WizardState> history = new Stack<WizardState>();

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(BaseWizard.class);

    /**
     * The initial state of the wizard.
     */
    protected WizardState initialState;

    /**
     * The current state of the wizard.
     */
    private WizardState currentState;

    /**
     * The final state of the wizard.
     */
    protected WizardState finalState = new WizardCompleteState(this, "success");

    /**
     * A mapping of all the wizards states and their names.
     */
    private Map<String, WizardState> states = new HashMap<String, WizardState>();

    /**
     * An instance of the validation manager used by this wizard instance to handle wizard state
     * validation.
     */
    private DefaultActionValidatorManager validationManager = new DefaultActionValidatorManager();

    private final ValidationAware validationAware = new ValidationAwareSupport();

    private TextProvider textProvider;
    private LocaleProvider localeProvider;

    /**
     * Required no-arg constructor. A no-arg constructor should be implemented by subclasses
     * to initialise the wizard states.
     */
    public BaseWizard()
    {
        states.put("success", finalState);
    }

    /**
     * Retrieve the wizards current state.
     *
     */
    public WizardState getCurrentState()
    {
        return currentState;
    }

    /**
     * Retrieve the named state.
     *
     * @param name
     */
    public WizardState getState(String name)
    {
        return states.get(name);
    }

    public void addState(WizardState state)
    {
        states.put(state.getStateName(), state);
    }

    public void addInitialState(WizardState state)
    {
        initialState = state;
        states.put(state.getStateName(), state);
    }

    /**
     * The wizard is complete if we have reached the final state.
     *
     * @return true if the wizard is complete and requires no more interaction, false
     * otherwise.
     */
    public boolean isComplete()
    {
        return currentState == finalState;
    }

    /**
     * Override this to handle any final processing required when the wizard is completed.
     */
    public void process()
    {

    }

    /**
     * Override this to handle any processing required when this wizard is cancelled.
     */
    public void cancel()
    {

    }

    /**
     * Handle initialisation of this wizard.
     */
    public void initialise()
    {
        currentState = initialState;
        currentState.initialise();
    }

    /**
     * Go back to the requested state. If this state is located in the wizards history,
     * the it is set as the current state. If it is not located, no change is made.
     *
     * @param requestedState
     *
     * @return true if the wizard has been rewound to the requested state, false if no change
     * has occured..
     */
    public boolean traverseBackwardTo(String requestedState)
    {
        if (getState(requestedState) == null)
        {
            return false;
        }
        if (currentState.getStateName().equals(requestedState))
        {
            return true;
        }

        WizardState state;
        while (history.size() > 0)
        {
            state = history.pop();

            if (state.getStateName().equals(requestedState))
            {
                currentState = state;
                return true;
            }
        }
        return false;
    }

    /**
     * Take a step forward.
     *
     * @return the state name for the state to be displayed next.
     */
    public String traverseForward()
    {
        // we can not progress to the next state until validation is successful.
        if (hasErrors())
        {
            return currentState.getView();
        }

        // execute current state
        currentState.execute();
        history.push(currentState);

        // get next state.
        String nextState = currentState.getNextStateName();
        if (nextState == null)
        {
            addActionError("Unknown next state: " + nextState);
            return currentState.getView();
        }

        // move on to the next state.
        currentState = getState(nextState);

        clearErrors();
        currentState.initialise();

        return currentState.getView();
    }

    /**
     * Take a step backwards to the previous wizard state.
     *
     * @return the state name for the next state to be displayed.
     */
    public String traverseBackward()
    {
        if (history.size() > 0)
        {
            currentState = history.pop();
        }
        return currentState.getView();
    }

    /**
     * Restart this wizard.
     *
     * @return the wizards initial state name.
     */
    public String restart()
    {
        currentState = initialState;
        return currentState.getView();
    }

    /**
     * Validate the current wizard state, using Xworks validation framework.
     *
     */
    public void validate()
    {
        // Since this state persists over multiple requests, we need to ensure that
        // we reset its error state before starting the validation.
        clearErrors();
        assert !hasErrors();

        try
        {
            validationManager.validate(currentState, currentState.getClass().getName());
            if (Validateable.class.isAssignableFrom(currentState.getClass()))
            {
                ((Validateable) currentState).validate();
            }
        }
        catch (ValidationException e)
        {
            addActionError(e.getMessage());
        }
    }

    /**
     * Specify the wizards locale provider.
     *
     * @param localeProvider
     */
    public void setLocaleProvider(LocaleProvider localeProvider)
    {
        this.localeProvider = localeProvider;
    }

    protected TextProvider getTextProvider()
    {
        if (textProvider == null)
        {
            textProvider = new TextProviderSupport(getClass(), localeProvider);
        }
        return textProvider;
    }

    // ---( Implement the ValidationAware interface. )---

    public void addActionError(String anErrorMessage)
    {
        validationAware.addActionError(anErrorMessage);
    }

    public void addActionMessage(String aMessage)
    {
        validationAware.addActionMessage(aMessage);
    }

    public void addFieldError(String fieldName, String errorMessage)
    {
        validationAware.addFieldError(fieldName, errorMessage);
    }

    public Collection getActionMessages()
    {
        return validationAware.getActionMessages();
    }

    public boolean hasActionErrors()
    {
        return validationAware.hasActionErrors();
    }

    public boolean hasActionMessages()
    {
        return validationAware.hasActionMessages();
    }

    public boolean hasFieldErrors()
    {
        return validationAware.hasFieldErrors();
    }

    public void setActionErrors(Collection errorMessages)
    {
        validationAware.setActionErrors(errorMessages);
    }

    public void setActionMessages(Collection messages)
    {
        validationAware.setActionMessages(messages);
    }

    public void setFieldErrors(Map errorMap)
    {
        validationAware.setFieldErrors(errorMap);
    }

    public Collection getActionErrors()
    {
        return validationAware.getActionErrors();
    }

    public Map getFieldErrors()
    {
        return validationAware.getFieldErrors();
    }

    public boolean hasErrors()
    {
        return validationAware.hasErrors();
    }

    public void clearErrors()
    {
        setActionErrors(null);
        setFieldErrors(null);
        setActionMessages(null);
    }
}