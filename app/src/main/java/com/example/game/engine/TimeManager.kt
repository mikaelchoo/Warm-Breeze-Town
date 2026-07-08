package com.example.game.engine

import java.util.Calendar
import java.util.Random

enum class Season(val id: Int, val nameCn: String) {
    SPRING(1, "温暖春季"),
    SUMMER(2, "繁茂夏季"),
    AUTUMN(3, "金秋收获"),
    WINTER(4, "静谧冬季")
}

enum class DayPhase(val nameCn: String) {
    DAY("白天"),
    DUSK("黄昏"),
    NIGHT("夜晚")
}

enum class Weather(val nameCn: String) {
    SUNNY("晴空万里"),
    CLOUDY("微风多云"),
    RAINY("绵绵细雨"),
    SNOWY("纷纷瑞雪")
}

object TimeManager {

    fun getCurrentSeason(): Season {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) // 0-indexed (0: Jan, 11: Dec)
        return when (month) {
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Season.SPRING
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Season.SUMMER
            Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> Season.AUTUMN
            else -> Season.WINTER
        }
    }

    fun getCurrentDayPhase(): DayPhase {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY) // 0..23
        return when {
            hour in 6..17 -> DayPhase.DAY
            hour in 18..19 -> DayPhase.DUSK
            else -> DayPhase.NIGHT
        }
    }

    /**
     * Generates a stable weather for the day based on the year + dayOfYear seed.
     */
    fun getWeatherForToday(): Weather {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        val seed = (year * 1000L + dayOfYear).hashCode().toLong()
        val rand = Random(seed)
        
        val season = getCurrentSeason()
        val roll = rand.nextInt(100)
        
        return when (season) {
            Season.SPRING -> {
                if (roll < 60) Weather.SUNNY
                else if (roll < 85) Weather.CLOUDY
                else Weather.RAINY
            }
            Season.SUMMER -> {
                if (roll < 50) Weather.SUNNY
                else if (roll < 75) Weather.CLOUDY
                else Weather.RAINY
            }
            Season.AUTUMN -> {
                if (roll < 70) Weather.SUNNY
                else if (roll < 90) Weather.CLOUDY
                else Weather.RAINY
            }
            Season.WINTER -> {
                if (roll < 40) Weather.SUNNY
                else if (roll < 70) Weather.CLOUDY
                else Weather.SNOWY
            }
        }
    }

    /**
     * Check if a custom date is a holiday.
     */
    fun getHolidayToday(): String? {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1 // 1..12
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val mmDd = String.format("%02d-%02d", month, day)
        
        return when (mmDd) {
            "01-01" -> "元旦节 (New Year's Day) 🎆"
            "02-17" -> "春节 (Lunar New Year) 🧧" // Simulating Chinese New year
            "02-14" -> "情人节 (Valentine's Day) 💖"
            "04-05" -> "复活节 (Easter Egg Hunt) 🥚"
            "10-31" -> "万圣节 (Halloween Pumpkin Festival) 🎃"
            "12-25" -> "圣诞节 (Christmas Gift Exchange) 🎄"
            else -> null
        }
    }

    fun getFormattedTime(): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        return String.format("%02d:%02d:%02d", hour, minute, second)
    }

    fun getFormattedDate(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return "$year 年 $month 月 $day 日"
    }

    /**
     * Given the planting timestamp and watered status, calculate current growth stage:
     * 0: Seed, 1: Sprout, 2: Bud, 3: Bloom.
     * Ordinarily takes 24 hours per stage (real-time).
     * To support interactive immediate progression, we also provide simulated offsets.
     */
    fun getFlowerGrowthStage(plantedTime: Long, currentOffsetSec: Long = 0): Int {
        val elapsedMs = System.currentTimeMillis() - plantedTime + (currentOffsetSec * 1000L)
        val elapsedHours = elapsedMs / (1000L * 60 * 60)
        
        // Fast test values: say 10 seconds is 1 hour, or standard 24 hours.
        // Let's make it 1 minute = 1 hour for fun mobile simulation, OR standard real-time if offset is applied.
        // Let's use real days but with a fast fallback: if no offset, 1 stage per 10 minutes (for excellent playability!),
        // or standard real-time. Let's do 1 stage per 5 minutes of real playtime so the user can see it grow in front of them,
        // which makes the game feel responsive!
        val stage = (elapsedMs / (1000L * 60 * 5)).toInt() // 5 minutes per stage
        return stage.coerceAtMost(3)
    }
}
