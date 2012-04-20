<!--
Copyright (C) 2007-2009 Nat Pryce.

This file is part of Team Piazza.

Team Piazza is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

Team Piazza is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
-->
<%@ include file="/include.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" session="true" errorPage="/runtimeError.jsp" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<jsp:useBean id="project"
             type="com.natpryce.piazza.ProjectMonitorViewState"
             scope="request"/>

<jsp:useBean id="resourceRoot"
             type="java.lang.String"
             scope="request"/>

<jsp:useBean id="version"
             type="java.lang.String"
             scope="request"/>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Piazza - ${project.projectName}</title>
    <meta http-equiv="refresh" content="${project.building ? 5 : 10}">
    <link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>${resourceRoot}piazza.css"/>
</head>
<body class="${project.combinedStatusClasses}">
<h1>${project.projectName}</h1>

<h2>${project.status}.</h2>


<div class="Content">

    <c:if test="${! empty project.committers}">
        <div class="Portraits">
            <c:forEach var="committer" items="${project.committers}">
                <div class="Portrait">
                <c:choose>
                <c:when test="${empty committer.portraitURL}"><img src="<%= request.getContextPath() %>${resourceRoot}silhouette.jpg" title="${fn:escapeXml(committer.name)}"></c:when>
                <c:otherwise><img src="${fn:escapeXml(committer.portraitURL)}" title="${fn:escapeXml(committer.name)}"></c:otherwise>
                </c:choose>
                <p class="Name">${committer.name}</p>
                </div>
            </c:forEach>
        </div>
    </c:if>

    <div class="Builds">
        <c:if test="${empty project.builds}">
            <p class="Tip">
            No monitored builds. Enable the status widget for builds that you want to appear in the Project monitor.
            </p>
        </c:if>
        
        <c:forEach var="build" items="${project.builds}">
            <div class="Build ${build.combinedStatusClasses} ${build.investigationStatusClass}">
                <div>
                    <div class="Details">
                        <h3>${build.name}</h3>
                        <span class="BuildNumber">#${build.buildNumber}</span>
                        <c:if test="${build.queued}"><span class="Queued">Queued</span></c:if>

                        <c:choose>
                            <c:when test="${build.building}">
                                <div class="ProgressBar ${build.runningBuildStatus}">
                                    <div class="Thumb" style="width: ${build.completedPercent}%">
                                        <div class="ThumbInner"></div>
                                    </div>
                                    <div class="Activity ${build.runningBuildStatus}">
                                        ${build.activity}
                                        <c:if test="${build.tests.anyHaveRun}">
                                            (Tests passed: ${build.tests.passed},
                                            failed: ${build.tests.failed},
                                            ignored: ${build.tests.ignored})
                                        </c:if>
                                    </div>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="Information ${build.runningBuildStatus}">
                                    <div class="Activity">${build.activity}</div>
                                    <div class="Timings">
                                        Build completed in
                                        ${fn:substringBefore((build.durationSeconds - (build.durationSeconds mod 60)) div 60, '.')}m
                                        ${build.durationSeconds mod 60}s
                                    </div>
                                    <c:if test="${build.tests.anyHaveRun}">
                                    <div class="Results">
                                        <span class="Passed">Passed: <span class="Result">${build.tests.passed}</span></span>
                                        <span class="Failed">Failed: <span class="Result">${build.tests.failed}</span></span>
                                        <span class="Ignored">Ignored: <span class="Result">${build.tests.ignored}</span></span>
                                    </div>
                                    </c:if>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="Investigation ${build.investigationStatusClass}">
                        <c:if test="${build.beingInvestigated}">
                            <div class="Portrait">
                                <c:choose>
                                <c:when test="${empty build.investigator.portraitURL}"><img src="<%= request.getContextPath() %>${resourceRoot}silhouette.jpg" title="${fn:escapeXml(build.investigator.name)}"></c:when>
                                <c:otherwise><img src="${fn:escapeXml(build.investigator.portraitURL)}" title="${fn:escapeXml(build.investigator.name)}"></c:otherwise>
                                </c:choose>
                                <p class="Name">${build.investigator.name}</p>
                            </div>
                            <c:if test="${! empty build.investigationComment}"><span class="Comment">${build.investigationComment}</span></c:if>
                        </c:if>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>

    <div class="Version">
        Team Piazza version ${version}
    </div>
</div>
</body>
</html>
