/*
 * Copyright (c) 2011 Nat Pryce, Timo Meinen.
 *
 * This file is part of Team Piazza.
 *
 * Team Piazza is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Team Piazza is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.natpryce.piazza;

import jetbrains.buildServer.Build;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.comments.Comment;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsModification;

import java.util.*;

public class BuildTypeMonitorViewState {

	private final SBuildType buildType;

    private final List<String> commitMessages;
	private final SBuild lastFinishedBuild;
	private final SBuild latestBuild;
	private final TestStatisticsViewState tests;
	private final Set<PiazzaUser> committers;
    private final UserGroup users;
    private final ResponsibilityInfo responsibilityInfo;

    public BuildTypeMonitorViewState (SBuildType buildType, UserGroup userPictures) {
		this.buildType = buildType;
        this.users = userPictures;
        this.lastFinishedBuild = buildType.getLastChangesFinished();
		this.latestBuild = buildType.getLastChangesStartedBuild();
		this.commitMessages = commitMessagesForBuild(latestBuild);
        this.responsibilityInfo = buildType.getResponsibilityInfo();

		committers = userPictures.usersInvolvedInCommit(
				committersForBuild(latestBuild),
				commitMessagesForBuild(latestBuild)
		);

		this.tests = testStatistics();
	}

	private Set<String> committersForBuild (Build latestBuild) {
		List<? extends VcsModification> changesSinceLastSuccessfulBuild = changesInBuild(latestBuild);

		HashSet<String> committers = new HashSet<String>();
		for (VcsModification vcsModification : changesSinceLastSuccessfulBuild) {
			String userName = vcsModification.getUserName();
			if (userName != null) {
				committers.add(userName.trim());
			}
		}
		return committers;
	}

	private ArrayList<String> commitMessagesForBuild (Build latestBuild) {
		List<? extends VcsModification> changesSinceLastSuccessfulBuild = changesInBuild(latestBuild);

		ArrayList<String> commitMessages = new ArrayList<String>();
		for (VcsModification vcsModification : changesSinceLastSuccessfulBuild) {
			commitMessages.add(vcsModification.getDescription().trim());
		}

		return commitMessages;
	}

	private TestStatisticsViewState testStatistics () {
        ShortStatistics stats = latestBuild.getShortStatistics();
        return new TestStatisticsViewState(
                stats.getPassedTestCount(), stats.getFailedTestCount(), stats.getIgnoredTestCount());
	}

	@SuppressWarnings("unchecked")
	private List<? extends VcsModification> changesInBuild (Build latestBuild) {
		return latestBuild.getChanges(SelectPrevBuildPolicy.SINCE_LAST_SUCCESSFULLY_FINISHED_BUILD, true);
	}

	public String getFullName () {
		return Text.toTitleCase(buildType.getFullName());
	}

	public String getName () {
		return Text.toTitleCase(buildType.getName());
	}

	public String getBuildNumber () {
		return latestBuild.getBuildNumber();
	}

	public String getCombinedStatusClasses () {
        String status = status().toStringReflectingCurrentlyBuilding(isBuilding());
        if (buildType.isPaused())
            return status + " Paused";
        return status;
	}

	public boolean isBuilding () {
		return !latestBuild.isFinished();
	}
    
	public Build getLatestBuild () {
		return latestBuild;
	}

	public String getActivity () {
		if (isBuilding()) {
			return ((SRunningBuild) latestBuild).getShortStatistics().getCurrentStage();
		} if (buildType.isPaused()) {
            Comment comment = buildType.getPauseComment();
            if (comment != null)
                return "Paused - " + comment.getComment();
        }

		return status().toString();
	}

	public int getCompletedPercent () {
		if (isBuilding()) {
			return ((SRunningBuild) latestBuild).getCompletedPercent();
		} else {
			return 100;
		}
	}

	public TestStatisticsViewState getTests () {
		return tests;
	}

	public long getDurationSeconds () {
		Date start = latestBuild.getStartDate();
		Date finished = latestBuild.getFinishDate();
		Date end = (finished != null) ? finished : now();

		return (end.getTime() - start.getTime()) / 1000L;
	}

	private Date now () {
		return new Date();
	}

	public String getStatus () {
		return status().toString();
	}

	public BuildStatus status () {
		if (latestBuild == null) {
			return BuildStatus.UNKNOWN;
		} else if (latestBuild.getBuildStatus().isFailed()) {
			return BuildStatus.FAILURE;
		}
		if (lastFinishedBuild == null) {
			return BuildStatus.UNKNOWN;
		} else if (lastFinishedBuild.getBuildStatus().isFailed()) {
			return BuildStatus.FAILURE;
		} else {
			return BuildStatus.SUCCESS;
		}
	}

	public String getRunningBuildStatus () {
		return runningBuildStatus().toString();
	}

	public BuildStatus runningBuildStatus () {
        // Don't return paused status here - we're interested in what the current build is doing, for the progress bar
		if (latestBuild == null) {
			return BuildStatus.UNKNOWN;
		} else if (latestBuild.getBuildStatus().isFailed()) {
			return BuildStatus.FAILURE;
		} else {
			return BuildStatus.SUCCESS;
		}
	}

	public List<String> getCommitMessages () {
		return commitMessages;
	}

	public Set<PiazzaUser> getCommitters () {
		return committers;
	}
    
    public String getInvestigationStatusClass() {
        if (status() == BuildStatus.SUCCESS)
            return "NotInvestigated";

        switch (responsibilityInfo.getState())
        {
            case NONE: return "NotInvestigated";
            case FIXED: return "Fixed";
            case GIVEN_UP: return "GivenUp";
            case TAKEN: return "UnderInvestigation";
        }

        return "";
    }

    public boolean isBeingInvestigated () {
        return responsibilityInfo.getState().isActive()
                || responsibilityInfo.getState().isFixed()
                || responsibilityInfo.getState().isGivenUp();
    }
    
    public PiazzaUser getInvestigator() {
        if (isBeingInvestigated())
            return users.getUser(responsibilityInfo.getResponsibleUser().getDescriptiveName().trim());
        return null;
    }
    
    public String getInvestigationComment() {
        if (isBeingInvestigated())
            return responsibilityInfo.getComment();
        return "";
    }

    public boolean isQueued() {
        return buildType.isInQueue();
    }
}
