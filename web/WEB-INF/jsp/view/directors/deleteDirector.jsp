<spring:message code="title.deleteDirector" var="pageTitle" />
<template:displayPost htmlTitle="${pageTitle}" bodyTitle="${pageTitle}">

  <jsp:attribute name="displayContent"><br/>
  
  	<h2><spring:message code="deleteDirector.directorId"/></h2>
	<form:form method="POST" action="deleteDirector" modelAttribute="getDirector">
    <table>
	<tr>
		<td><form:label path="id"><spring:message code="deleteDirector.directorId"/></form:label></td>
		<td><form:input path="id" /></td>         
    	<td><form:errors path="id" class="errors" /></td>
    </tr>     
	<tr>
	 	<td colspan="2"><input type="submit" value="<spring:message code="form.valid"/>" /></td>
	</tr>
	</table>
	</form:form>
  
  </jsp:attribute>
  
</template:displayPost>