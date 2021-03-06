(function(form, field)
{
    form.bind('action', function(e)
    {
        var fsw, basePath, dirField;

        if (e.field !== field || e.action !== 'browse') return;

        basePath = Zutubi.config.templateOwner(form.options.parentPath + "/" + form.options.baseName);
<#if field.parameters.baseDirField?exists>
        dirField = form.getFieldNamed('${field.parameters.baseDirField}');
        basePath = Zutubi.config.normalisedPath(basePath + "/" + dirField.getValue());
</#if>

        fsw = new Zutubi.config.FileSystemWindow({
            title: 'select file',
            fs: 'scm',
            basePath: basePath,
            targetField: field
        });

        fsw.show();
    });
});
