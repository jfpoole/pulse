${form.name}.items.last().on('browse', function(field)
{
    var projectPath = '${field.parameters.parentPath}';
<#if field.parameters.baseName?exists>
    projectPath = 'c' + projectPath;
<#else>
    projectPath = 'wizards/' + projectPath;
</#if>
    openSCMSelectDialog('${base}', false, '${form.name}', field.name, projectPath, '', 'scm');
});