<%@ tag body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ attribute name="htmlTitle" type="java.lang.String" rtexprvalue="true"
              required="true" %>
<%@ attribute name="bodyTitle" type="java.lang.String" rtexprvalue="true"
              required="true" %>
<%@ attribute name="navigationContent" fragment="true" required="false" %>
<%@ include file="/WEB-INF/jsp/base.jspf" %>
<template:main htmlTitle="${htmlTitle}" bodyTitle="${bodyTitle}">


    <jsp:attribute name="localeContent">
		Language:<br/>
		<a href="?locale=fr_FR">Fran�ais</a><br/>
		<a href="?locale=en_US">English</a><br/>
	</jsp:attribute>
	
	

    <jsp:body>
    	<br/>
    	<a href="<c:url value="/backHome" />"><spring:message code="index.backHome"/></a><br/>
		<br/>
 		<a href="javascript:void 0;"
           onclick="postInvisibleForm('<c:url value="/logout" />', { });">Logout</a><br />
		<br/>
		
		 <jsp:invoke fragment="navigationContent" />
		 
        <jsp:doBody />
    </jsp:body>

   
        
</template:main>
