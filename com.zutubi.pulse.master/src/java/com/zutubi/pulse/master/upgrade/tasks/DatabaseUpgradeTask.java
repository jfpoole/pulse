package com.zutubi.pulse.master.upgrade.tasks;

import com.zutubi.pulse.core.util.JDBCUtils;
import com.zutubi.pulse.master.upgrade.DataSourceAware;
import com.zutubi.pulse.master.upgrade.UpgradeException;
import com.zutubi.util.io.IOUtils;
import com.zutubi.util.logging.Logger;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 */
public abstract class DatabaseUpgradeTask extends AbstractUpgradeTask implements DataSourceAware
{
    private static final Logger LOG = Logger.getLogger(DatabaseUpgradeTask.class);

    protected DataSource dataSource;

    public void execute() throws UpgradeException
    {
        Connection connection = null;
        try
        {
            connection = dataSource.getConnection();
            execute(connection);
        }
        catch (IOException e)
        {
            LOG.error(e);
            addError("IOException: " + e.getMessage() + ". Please see the log files for details.");
        }
        catch (SQLException e)
        {
            LOG.error(e);
            addError("SQLException: " + e.getMessage() + ". Please see the log files for details.");
        }
        finally
        {
            JDBCUtils.close(connection);
        }
    }

    public abstract void execute(Connection con) throws IOException, SQLException;

    protected byte[] upgradeBlob(ResultSet rs, String columnName, ObjectUpgrader objectUpgrader) throws SQLException, IOException
    {
        byte[] data = rs.getBytes(columnName);

        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;

        try
        {
            ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object object = ois.readObject();
            objectUpgrader.upgrade(object);

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(byteStream);
            oos.writeObject(object);
            data = byteStream.toByteArray();
        }
        catch (ClassNotFoundException e)
        {
            throw new IOException(e.getMessage());
        }
        finally
        {
            IOUtils.close(ois);
            IOUtils.close(oos);
        }
        return data;
    }

    protected void addIndex(Connection con, String table, String indexName, String... columns) throws SQLException
    {
        String sql = "CREATE INDEX " + indexName + " ON " + table + " (";
        boolean first = true;
        for (String column : columns)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sql += ",";
            }

            sql += column;
        }

        sql += ")";

        runUpdate(con, sql);
    }

    protected void dropIndex(Connection con, String table, String indexName) throws SQLException
    {
        String sql;
        String databaseProductName = con.getMetaData().getDatabaseProductName().toLowerCase();
        if (databaseProductName.contains("postgres"))
        {
            sql = "DROP INDEX IF EXISTS " + indexName;
        }
        else if (databaseProductName.contains("mysql"))
        {
            sql = "DROP INDEX " + indexName + " ON " + table;
        }
        else
        {
            sql = "DROP INDEX " + indexName + " IF EXISTS";
        }

        runUpdate(con, sql);
    }

    protected void runUpdate(Connection con, String sql) throws SQLException
    {
        PreparedStatement stmt = null;
        try
        {
            stmt = con.prepareStatement(sql);
            stmt.executeUpdate();
        }
        finally
        {
            JDBCUtils.close(stmt);
        }
    }

    public void setDataSource(DataSource source)
    {
        this.dataSource = source;
    }
}