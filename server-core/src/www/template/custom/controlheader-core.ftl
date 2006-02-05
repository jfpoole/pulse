<#--
	Only show message if errors are available.
	This will be done if ActionSupport is used.
-->
<#assign hasFieldErrors = fieldErrors?exists && fieldErrors[parameters.name]?exists/>
<#if hasFieldErrors>
<#list fieldErrors[parameters.name] as error>
<tr errorFor="${parameters.id}">
    <th class="error-message" colspan="2"><#rt/>
        ${error?html}<#t/>
    </th><#lt/>
</tr>
</#list>
</#if>
<tr>
    <th <#if hasFieldErrors>
        class="error-label"<#t/>
<#else>
        class="label"<#t/>
</#if>
    ><#t/>
<#if parameters.label?exists>
    <label <#t/>
<#if parameters.id?exists>
        for="${parameters.id?html}" <#t/>
</#if>
<#if hasFieldErrors>
        class="error-label"<#t/>
<#else>
        class="label"<#t/>
</#if>
    ><#t/>
        ${parameters.label?html} <#lt/>
<#if parameters.required?default(false)>
        <span class="required">*</span><#t/>
</#if>
    </label>:<#t/>
</#if>
    </th><#lt/>
