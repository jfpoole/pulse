package com.zutubi.pulse.master.charting.demo;

import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.master.charting.BuildResultsDataSource;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.UnknownBuildReason;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Random;

public class DemoDataSourceFactory
{
    private static final Random RAND = new Random();

    public static BuildResultsDataSource createBuildResultsDataSource()
    {
        return new DemoBuildResultDataSource(createBuildResults());
    }

    private static LinkedList<BuildResult> createBuildResults()
    {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DAY_OF_YEAR, -35);

        LinkedList<BuildResult> buildResults = new LinkedList<BuildResult>();
        int l = 0;
        for (int i = 0; i < 35; i++)
        {
            int j = RAND.nextInt(50);
            for (int k = 0; k < j; k++)
            {
                boolean success = RAND.nextInt(50) < 40;
                long finished = date.getTimeInMillis();
                long started = finished - 10000 - (RAND.nextInt(3000) + 200 * i);
                buildResults.add(createBuildResult(l++, success, started, finished));
            }
            date.add(Calendar.DAY_OF_YEAR, 1);
        }
        return buildResults;
    }

    private static BuildResult createBuildResult(long id, boolean successful, long started, long finished)
    {
        BuildResult result = new BuildResult(new UnknownBuildReason(), null, id, false);
        result.setId(id);
        if (successful)
        {
            result.setState(ResultState.SUCCESS);
        }
        else
        {
            result.setState(ResultState.FAILURE);
        }
        result.getStamps().setStartTime(started);
        result.getStamps().setEndTime(finished);
        return result;
    }


}