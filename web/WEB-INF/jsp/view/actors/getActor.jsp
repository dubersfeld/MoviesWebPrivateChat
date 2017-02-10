<spring:message code="title.getActor" var="pageTitle" />
<template:displayPost htmlTitle="${pageTitle}" bodyTitle="${pageTitle}">

  
  <jsp:attribute name="displayContent"><br/>
 
	<form:form method="POST" action="getActor" modelAttribute="getActor">
   	<table>  
    <tr>
        <td><form:label path="id"><spring:message code="personal.id"/>:</form:label></td>
        <td><form:input path="id" /></td>
        <td><form:errors path="id" class="errors"/></td>
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