<spring:message code="title.getActorQueryResult" var="pageTitle" />
<spring:message code="date.pattern" var="datePattern" />

<template:displayPost htmlTitle="${pageTitle}" bodyTitle="${pageTitle}">
  
  <jsp:attribute name="displayContent"><br/>
  
  	<h2><spring:message code="actor.actorData"/></h2>
         
    <spring:message code="personal.id"/>: ${actor.id}<br />
    <spring:message code="personal.firstName"/>: ${actor.firstName}<br />
    <spring:message code="personal.lastName"/>: ${actor.lastName}<br />
    <spring:message code="personal.birthDateDisplay"/>: <fmt:formatDate value="${actor.birthDate}" pattern="${datePattern}" type="date" /><br /><br />              
   
  </jsp:attribute>
  
</template:displayPost>