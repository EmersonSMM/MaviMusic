package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopSmartPlaylistsTest {

    @Test
    fun ruleSetSerializationRoundTrip() {
        val ruleSet = SmartPlaylistRuleSet(
            matchMode = SmartRuleMatchMode.ALL,
            conditions = listOf(
                SmartRuleCondition(SmartRuleField.IS_LIKED, SmartRuleOperator.IS, "true"),
                SmartRuleCondition(SmartRuleField.PLAY_COUNT, SmartRuleOperator.GREATER_THAN, "5"),
                SmartRuleCondition(SmartRuleField.ARTIST, SmartRuleOperator.CONTAINS, "Daft Punk")
            ),
            sortBy = SmartSortField.PLAY_COUNT,
            sortDescending = true,
            limit = 25,
            autoUpdate = true
        )

        val encoded = SmartRuleSerializer.encode(ruleSet)
        assertTrue(encoded.isNotBlank())

        val decoded = SmartRuleSerializer.decode(encoded)
        assertNotNull(decoded)
        assertEquals(ruleSet.matchMode, decoded!!.matchMode)
        assertEquals(ruleSet.sortBy, decoded.sortBy)
        assertEquals(ruleSet.sortDescending, decoded.sortDescending)
        assertEquals(ruleSet.limit, decoded.limit)
        assertEquals(ruleSet.conditions.size, decoded.conditions.size)

        assertEquals(SmartRuleField.IS_LIKED, decoded.conditions[0].field)
        assertEquals(SmartRuleOperator.IS, decoded.conditions[0].operator)
        assertEquals("true", decoded.conditions[0].value)

        assertEquals(SmartRuleField.PLAY_COUNT, decoded.conditions[1].field)
        assertEquals(SmartRuleOperator.GREATER_THAN, decoded.conditions[1].operator)
        assertEquals("5", decoded.conditions[1].value)

        assertEquals(SmartRuleField.ARTIST, decoded.conditions[2].field)
        assertEquals(SmartRuleOperator.CONTAINS, decoded.conditions[2].operator)
        assertEquals("Daft Punk", decoded.conditions[2].value)
    }

    @Test
    fun evaluationFiltersByLikesAndPlays() {
        val song1 = Song("s1", "One More Time", "Daft Punk", null, "5:20")
        val song2 = Song("s2", "Harder Better Faster", "Daft Punk", null, "3:44")
        val song3 = Song("s3", "Get Lucky", "Daft Punk", null, "4:08")
        val song4 = Song("s4", "Around the World", "Daft Punk", null, "7:09")

        val universe = listOf(song1, song2, song3, song4)

        val context = SmartEvaluationContext(
            likedIds = setOf("s1", "s3"),
            dislikedIds = emptySet(),
            playCountsTotal = mapOf("s1" to 15, "s2" to 30, "s3" to 8, "s4" to 2),
            playCountsRecent = mapOf("s1" to 5, "s2" to 10),
            lastPlayedTimestamps = emptyMap(),
            bpmCache = emptyMap(),
            librarySongIds = setOf("s1", "s2", "s3", "s4")
        )

        // Rule: IS_LIKED == true AND PLAY_COUNT >= 10
        val ruleSet = SmartPlaylistRuleSet(
            matchMode = SmartRuleMatchMode.ALL,
            conditions = listOf(
                SmartRuleCondition(SmartRuleField.IS_LIKED, SmartRuleOperator.IS, "true"),
                SmartRuleCondition(SmartRuleField.PLAY_COUNT, SmartRuleOperator.GREATER_THAN, "9")
            ),
            sortBy = SmartSortField.PLAY_COUNT,
            sortDescending = true,
            limit = 50
        )

        val result = DesktopSmartPlaylistEngine.evaluate(ruleSet, universe, context)
        // song1 is liked and has 15 plays (> 9) -> matches
        // song2 has 30 plays but NOT liked -> fails
        // song3 is liked but has 8 plays (< 10) -> fails
        assertEquals(1, result.size)
        assertEquals("s1", result[0].videoId)
    }

    @Test
    fun evaluationBpmMissingDoesNotInventValues() {
        val songWithBpm = Song("s1", "Fast Beat", "Artist A", null, "3:00")
        val songWithoutBpm = Song("s2", "Mystery Beat", "Artist B", null, "3:00")

        val universe = listOf(songWithBpm, songWithoutBpm)

        val context = SmartEvaluationContext(
            likedIds = emptySet(),
            dislikedIds = emptySet(),
            playCountsTotal = emptyMap(),
            playCountsRecent = emptyMap(),
            lastPlayedTimestamps = emptyMap(),
            bpmCache = mapOf("s1" to 128f), // s2 has NO BPM
            librarySongIds = setOf("s1", "s2")
        )

        // Rule: BPM > 120
        val ruleSet = SmartPlaylistRuleSet(
            matchMode = SmartRuleMatchMode.ALL,
            conditions = listOf(
                SmartRuleCondition(SmartRuleField.BPM, SmartRuleOperator.GREATER_THAN, "120")
            ),
            sortBy = SmartSortField.BPM,
            sortDescending = true,
            limit = 50
        )

        val result = DesktopSmartPlaylistEngine.evaluate(ruleSet, universe, context)
        assertEquals(1, result.size)
        assertEquals("s1", result[0].videoId)

        // Rule: BPM IS_EMPTY
        val emptyRuleSet = SmartPlaylistRuleSet(
            matchMode = SmartRuleMatchMode.ALL,
            conditions = listOf(
                SmartRuleCondition(SmartRuleField.BPM, SmartRuleOperator.IS_EMPTY, "")
            ),
            limit = 50
        )
        val emptyResult = DesktopSmartPlaylistEngine.evaluate(emptyRuleSet, universe, context)
        assertEquals(1, emptyResult.size)
        assertEquals("s2", emptyResult[0].videoId)
    }

    @Test
    fun evaluationDeduplicatesVersionVariants() {
        val original = Song("v1", "Blinding Lights", "The Weeknd", null, "3:20")
        val liveVersion = Song("v2", "Blinding Lights (Live)", "The Weeknd", null, "3:25")
        val remixVersion = Song("v3", "Blinding Lights (Remix)", "The Weeknd", null, "3:22")
        val differentSong = Song("v4", "Save Your Tears", "The Weeknd", null, "3:35")

        val universe = listOf(original, liveVersion, remixVersion, differentSong)

        val context = SmartEvaluationContext(
            likedIds = emptySet(),
            dislikedIds = emptySet(),
            playCountsTotal = emptyMap(),
            playCountsRecent = emptyMap(),
            lastPlayedTimestamps = emptyMap(),
            bpmCache = emptyMap(),
            librarySongIds = emptySet()
        )

        val ruleSet = SmartPlaylistRuleSet(
            matchMode = SmartRuleMatchMode.ALL,
            conditions = emptyList(), // match all
            sortBy = SmartSortField.TITLE,
            sortDescending = false,
            limit = 50
        )

        val result = DesktopSmartPlaylistEngine.evaluate(ruleSet, universe, context)
        // Should keep original and differentSong, filtering duplicate versions of Blinding Lights
        assertEquals(2, result.size)
        assertTrue(result.any { it.videoId == "v1" })
        assertTrue(result.any { it.videoId == "v4" })
        assertFalse(result.any { it.videoId == "v2" })
    }

    @Test
    fun builtInTemplatesAreValid() {
        assertEquals(4, SmartPlaylistTemplates.TEMPLATES.size)
        for (template in SmartPlaylistTemplates.TEMPLATES) {
            assertTrue(template.name.isNotBlank())
            assertTrue(template.description.isNotBlank())
            assertTrue(template.ruleSet.conditions.isNotEmpty())
            val encoded = SmartRuleSerializer.encode(template.ruleSet)
            val decoded = SmartRuleSerializer.decode(encoded)
            assertNotNull("Template ${template.name} should decode properly", decoded)
        }
    }
}
