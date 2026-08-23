package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PersonalTrendTest {
    @Test
    void comparesEqualRecentAndPreviousWindowsNewestFirst() {
        PersonalTrend trend = PersonalTrend.summarize(List.of(
                new PersonalTrend.Sample(18, 18_000L),
                new PersonalTrend.Sample(20, 20_000L),
                new PersonalTrend.Sample(22, 22_000L),
                new PersonalTrend.Sample(30, 30_000L),
                new PersonalTrend.Sample(32, 32_000L),
                new PersonalTrend.Sample(34, 34_000L)));

        assertEquals(3, trend.getRecentCount());
        assertEquals(3, trend.getPreviousCount());
        assertEquals(20L, trend.getRecentAverageMoves());
        assertEquals(32L, trend.getPreviousAverageMoves());
        assertEquals(-38, trend.getMoveChangePercent());
        assertEquals(PersonalTrend.Direction.IMPROVING, trend.getMoveDirection());
        assertEquals(PersonalTrend.Direction.IMPROVING, trend.getTimeDirection());
    }

    @Test
    void reportsRecentAverageWithoutClaimingTrendBeforeSixSamples() {
        PersonalTrend trend = PersonalTrend.summarize(List.of(
                new PersonalTrend.Sample(10, 9_000L),
                new PersonalTrend.Sample(14, 11_000L)));

        assertEquals(2, trend.getRecentCount());
        assertEquals(12L, trend.getRecentAverageMoves());
        assertEquals(10_000L, trend.getRecentAverageTimeMs());
        assertEquals(0, trend.getPreviousCount());
        assertEquals(PersonalTrend.Direction.NOT_ENOUGH_DATA, trend.getMoveDirection());
        assertEquals(PersonalTrend.Direction.NOT_ENOUGH_DATA, trend.getTimeDirection());
    }
}
