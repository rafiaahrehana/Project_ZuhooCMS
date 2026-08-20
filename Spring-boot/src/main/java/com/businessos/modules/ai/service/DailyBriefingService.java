package com.businessos.modules.ai.service;

public interface DailyBriefingService {
    /** Returns today's briefing for the current user, building and caching it on first call of the day. */
    String getOrBuildToday();
}
