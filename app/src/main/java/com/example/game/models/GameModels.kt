package com.example.game.models

import androidx.compose.ui.graphics.Color

enum class ItemType {
    TOOL,
    FURNITURE,
    WALLPAPER,
    FLOORING,
    SEED,
    FISH,
    INSECT,
    FOSSIL,
    MATERIAL,
    CLOTHING,
    FLOWER,
    ORE,
    MISC
}

data class Item(
    val id: String,
    val name: String,
    val type: ItemType,
    val description: String,
    val buyPrice: Int,
    val sellPrice: Int,
    val iconId: String, // Identifies visual style for custom Canvas rendering
    val extra: String = ""
)

data class PlacedFurniture(
    val id: String,
    val itemId: String,
    val x: Float, // Relative 0..1 coordinate inside room/yard
    val y: Float,
    val rotation: Float = 0f, // 0, 90, 180, 270
    val isOutdoors: Boolean = false
)

data class FishSpec(
    val id: String,
    val name: String,
    val location: String, // River, Pond, Sea
    val seasons: List<Int>, // 1: Spring, 2: Summer, 3: Autumn, 4: Winter
    val hours: List<Int>, // Hours active 0..23
    val rarity: String, // Common, Rare, Legendary
    val minSize: Float,
    val maxSize: Float,
    val sellPrice: Int
)

data class InsectSpec(
    val id: String,
    val name: String,
    val location: String, // Tree, Flower, Grass, Sky
    val seasons: List<Int>,
    val hours: List<Int>,
    val rarity: String,
    val sellPrice: Int
)

data class FossilSpec(
    val id: String,
    val name: String,
    val skeletonGroup: String, // e.g. "T-Rex", "Triceratops", "Pterodactyl"
    val partName: String, // e.g. "Head", "Torso", "Tail"
    val sellPrice: Int
)

data class PlacedFlower(
    val id: String,
    val flowerType: String, // Rose, Tulip, Mum
    val color: String, // Red, White, Yellow, Pink, Orange, Purple, Blue, Black, Gold
    val x: Float, // Yard 0..1 coordinate
    val y: Float,
    val growthStage: Int, // 0: Seed, 1: Sprout, 2: Bud, 3: Bloom
    val plantedTime: Long, // timestamp
    val watered: Boolean = false
)

enum class VillagerState {
    WALKING,
    FISHING,
    IN_HOUSE,
    SLEEPING
}

data class Villager(
    val id: String,
    val name: String,
    val animalType: String, // Cat, Dog, Rabbit, Bear, Deer, Penguin
    val personality: String, // 元气, 悠闲, 成熟, 暴躁
    val friendship: Int = 0, // 0..100
    val nickname: String = "",
    val hasPhoto: Boolean = false,
    val currentActivity: VillagerState = VillagerState.WALKING,
    val activityX: Float = 0.5f,
    val activityY: Float = 0.5f,
    val houseColor: Int = 0xFF8D6E63.toInt() // ARGB
)

data class DIYRecipe(
    val id: String,
    val name: String,
    val materials: List<Pair<String, Int>>, // ItemId to Count
    val outputItemId: String,
    val craftDurationSec: Int
)

data class CraftingTask(
    val recipeId: String,
    val startTime: Long,
    val durationMs: Long,
    val isCompleted: Boolean = false
)

enum class CombatCardType {
    ATTACK,
    DEFEND,
    HEAL,
    SPECIAL
}

data class CombatCard(
    val id: String,
    val name: String,
    val type: CombatCardType,
    val cost: Int,
    val damage: Int = 0,
    val shield: Int = 0,
    val heal: Int = 0,
    val description: String,
    val color: Int = 0xFF4CAF50.toInt()
)

data class CombatMonster(
    val id: String,
    val name: String,
    val maxHp: Int,
    var hp: Int,
    val attack: Int,
    val color: Int,
    val scale: Float = 1.0f
)

data class HolidayEvent(
    val name: String,
    val date: String, // MM-dd
    val description: String,
    val themeColor: Int // ARGB color
)
