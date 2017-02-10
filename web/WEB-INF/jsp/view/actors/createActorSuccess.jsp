<spring:message code="title.successCreateActor" var="pageTitle" />
<spring:message code="date.pattern" var="datePattern" />
<template:displayPost htmlTitle="${pageTitle}" bodyTitle="${pageTitle}">
  
  <jsp:attribute name="displayContent"><br/>
  
  	<h2><spring:message code="addActor.actorData"/></h2>
   	<table>
    <tr>
        <td><spring:message code="personal.firstName"/></td>
        <td>${actor.firstName}</td>
    </tr>
    <tr>
        <td><spring:message code="personal.lastName"/></td>
        <td>${actor.lastName}</td>
    </tr>
    <tr>
        <td><spring:message code="personal.birthDateDisplay"/></td>
        <td><fmt:formatDate value="${actor.birthDate}" pattern="${datePattern}" type="date" /></td>
    </tr>
    <tr>
        <td><spring:message code="personal.id"/></td>
        <td>${actor.id}</td>
    </tr>
	</table>  
 
  </jsp:attribute>
  
</template:displayPost>