<spring:message code="title.addActor" var="pageTitle" />
<template:displayPost htmlTitle="${pageTitle}" bodyTitle="${pageTitle}">
  
  <jsp:attribute name="displayContent"><br/>
  
  <h2><spring:message code="addActor.actorData"/></h2>
	<form:form method="POST" action="addActor" modelAttribute="actorForm">
   	<table>
    <tr>
        <td><form:label path="firstName"><spring:message code="personal.firstName"/>:</form:label></td>
        <td><form:input path="firstName" /></td>
        <td><form:errors path="firstName" class="errors" /></td>
    </tr>
    <tr>
        <td><form:label path="lastName"><spring:message code="personal.lastName"/>:</form:label></td>
        <td><form:input path="lastName" /></td>
        <td><form:errors path="lastName" class="errors" /></td>
    </tr>
    <tr>
        <td><form:label path="birthDate"><spring:message code="personal.birthDate"/>:</form:label></td>
        <td><form:input path="birthDate" /></td>
        <td><form:errors path="birthDate" class="errors"/></td>
    </tr>  
    <tr>
        <td colspan="2">
            <input type="submit" value="<spring:message code="form.valid"/>"/>
        </td>
    </tr>
  	</table>  
</form:form>
  
  
  </jsp:attribute>
  
</template:displayPost>