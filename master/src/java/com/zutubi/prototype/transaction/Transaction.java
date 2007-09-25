package com.zutubi.prototype.transaction;

import java.util.List;
import java.util.LinkedList;

/**
 * The transaction objects allows for operations to be performed against a specific transaction.
 *  
 * A Transaction object is created corresponding to each global transaction creation. The Transaction object
 * can be used for resource enlistment, transaction completion and status query operations.
 *
 * 
 */
public class Transaction
{
    /**
     * System transaction manager that manages this transaction. 
     */
    private TransactionManager transactionManager;

    /**
     * The list of transaction resources that are participating in this transaction.
     */
    private List<TransactionResource> resources;

    private List<Synchronization> synchronizations;

    /**
     * The transactions current status.
     */
    private TransactionStatus status = TransactionStatus.INACTIVE;

    Transaction(TransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

    /**
     * Complete the transaction represented by this Transaction object
     */
    public void commit()
    {
        this.transactionManager.commit();
    }

    /**
     * Rollback the transaction represented by this Transaction object.
     */
    public void rollback()
    {
        this.transactionManager.rollback();
    }

    /**
     * Modify the transaction associated with the current thread such that the only possible outcome of
     * the transaction is to roll back the transaction.
     */
    public void setRollbackOnly()
    {
        this.transactionManager.setRollbackOnly();
    }

    /**
     * Obtain the status of the transaction associated with the current thread.
     *
     * @return status
     *
     * @see com.zutubi.prototype.transaction.TransactionStatus
     */
    public TransactionStatus getStatus()
    {
        return status;
    }

    void setStatus(TransactionStatus status)
    {
        this.status = status;
    }

    /**
     * Register a transaction resource with this transaction.  Any registered resource will participate
     * commit / rollback processing of this transaction.  
     *
     * @param resource to be registered.
     */
    public void enlistResource(TransactionResource resource)
    {
        if (!getResources().contains(resource))
        {
            getResources().add(resource);
        }
    }

    public void delistResource(TransactionResource resource)
    {
        getResources().remove(resource);
    }

    public void registerSynchronization(Synchronization sync)
    {
        if (!getSynchronizations().contains(sync))
        {
            getSynchronizations().add(sync);
        }
    }

    List<Synchronization> getSynchronizations()
    {
        if (synchronizations == null)
        {
            synchronizations = new LinkedList<Synchronization>();
        }
        return synchronizations;
    }

    List<TransactionResource> getResources()
    {
        if (resources == null)
        {
            resources = new LinkedList<TransactionResource>();
        }
        return resources;
    }
}
