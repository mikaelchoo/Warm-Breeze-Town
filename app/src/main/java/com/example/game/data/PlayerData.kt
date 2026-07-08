package com.example.game.data

import android.content.Context
import android.util.Log
import com.example.game.models.*
import org.json.JSONArray
import org.json.JSONObject

class PlayerData {
    var playerName: String = "小暖"
    var playerBirthday: String = "07-08"
    var bells: Int = 5000
    var townRating: Float = 1.0f
    
    val inventory = mutableListOf<Item>()
    var currentHairstyle: Int = 0
    var currentHairColor: Int = 0xFF5D4037.toInt() // Dark brown
    var currentClothingId: String = "cloth_red_tee"
    
    var houseExpansionLevel: Int = 0 // 0 to 4
    val indoorFurniture = mutableListOf<PlacedFurniture>()
    val outdoorFurniture = mutableListOf<PlacedFurniture>()
    
    val museumDonatedFishes = mutableSetOf<String>()
    val museumDonatedInsects = mutableSetOf<String>()
    val museumDonatedFossils = mutableSetOf<String>()
    
    val unlockedDIYRecipes = mutableSetOf<String>()
    val placedFlowers = mutableListOf<PlacedFlower>()
    val villagers = mutableListOf<Villager>()
    
    val marketStandItems = mutableListOf<Pair<Item, Int>>() // Item to Price
    var currentCraftingTask: CraftingTask? = null
    
    var petName: String = "阿奇"
    var petClothingId: String = "none"
    
    var fossilsDugCount: Int = 0
    var lastFossilSpawnTime: Long = 0
    val fossilSpawns = mutableListOf<Pair<Float, Float>>() // X, Y coordinates in town
    var lastDailyResetTime: Long = 0

    companion object {
        private const val PREFS_NAME = "WarmBreezeTownPrefs"
        private const val SAVE_KEY = "SaveGameData"
        private const val TAG = "PlayerData"

        // CATALOGS
        val ITEMS = mapOf(
            // Tools
            "tool_shovel" to Item("tool_shovel", "简易铲子", ItemType.TOOL, "用来挖掘地面星形裂缝，寻找化石和矿石。", 500, 250, "ic_shovel"),
            "tool_rod" to Item("tool_rod", "简易钓竿", ItemType.TOOL, "在河流、池塘和海边钓鱼。", 500, 250, "ic_rod"),
            "tool_net" to Item("tool_net", "简易捕虫网", ItemType.TOOL, "用来捕捉在花草树木间飞舞的昆虫。", 500, 250, "ic_net"),
            "tool_watering_can" to Item("tool_watering_can", "洒水壶", ItemType.TOOL, "给花朵浇水。杂交出稀有花色的必备工具。", 400, 200, "ic_watering_can"),
            "tool_slingshot" to Item("tool_slingshot", "弹弓", ItemType.TOOL, "可以击落天空中飘过的气球礼盒。", 500, 250, "ic_slingshot"),

            // Materials
            "mat_wood" to Item("mat_wood", "普通木材", ItemType.MATERIAL, "砍树获得的结实木头，DIY的常用基础材料。", 100, 50, "ic_wood"),
            "mat_softwood" to Item("mat_softwood", "软木材", ItemType.MATERIAL, "质地轻盈的淡色木材，适合做小家具。", 100, 50, "ic_softwood"),
            "mat_hardwood" to Item("mat_hardwood", "硬木材", ItemType.MATERIAL, "坚硬沉重的深色木材，用于高级家具制作。", 100, 50, "ic_hardwood"),
            "mat_stone" to Item("mat_stone", "石头", ItemType.MATERIAL, "路边或敲矿石掉落的普通石头。", 80, 40, "ic_stone"),
            "mat_clay" to Item("mat_clay", "黏土", ItemType.MATERIAL, "挖裂缝或敲矿石获得的黏土，用于制作陶器。", 150, 75, "ic_clay"),
            "mat_iron" to Item("mat_iron", "铁矿石", ItemType.MATERIAL, "坚固耐用的优质矿石，升级工具和高级建筑需要它。", 300, 150, "ic_iron"),
            "mat_gold" to Item("mat_gold", "金矿石", ItemType.MATERIAL, "极为罕见的珍贵金灿灿矿石。", 3000, 1500, "ic_gold"),
            "mat_star" to Item("mat_star", "星星碎片", ItemType.MATERIAL, "流星雨过后的海滩上偶尔能拾到的梦幻碎片。", 2000, 1000, "ic_star"),
            "mat_horn" to Item("mat_horn", "怪兽之角", ItemType.MATERIAL, "探索神秘森林击败树精或甲虫获得的神秘触角，可做高级装饰。", 500, 250, "ic_horn"),

            // Flowers Seeds
            "seed_rose" to Item("seed_rose", "红玫瑰种子", ItemType.SEED, "种在院子里，能长出美丽的红玫瑰。", 240, 120, "ic_seed_rose", "Rose:Red"),
            "seed_rose_white" to Item("seed_rose_white", "白玫瑰种子", ItemType.SEED, "种在院子里，能长出皎洁的白玫瑰。", 240, 120, "ic_seed_rose", "Rose:White"),
            "seed_rose_yellow" to Item("seed_rose_yellow", "黄玫瑰种子", ItemType.SEED, "种在院子里，能长出明艳的黄玫瑰。", 240, 120, "ic_seed_rose", "Rose:Yellow"),
            "seed_tulip" to Item("seed_tulip", "红郁金香种子", ItemType.SEED, "能种出饱满的红色郁金香花骨朵。", 240, 120, "ic_seed_tulip", "Tulip:Red"),
            "seed_tulip_white" to Item("seed_tulip_white", "白郁金香种子", ItemType.SEED, "能种出纯洁高雅的白色郁金香。", 240, 120, "ic_seed_tulip", "Tulip:White"),
            "seed_mum" to Item("seed_mum", "黄菊花种子", ItemType.SEED, "乡村风情的金黄野菊花种子。", 240, 120, "ic_seed_mum", "Mum:Yellow"),

            // Clothing
            "cloth_straw_hat" to Item("cloth_straw_hat", "编织草帽", ItemType.CLOTHING, "充满田园气息的手工编织草帽，夏天必备。", 800, 400, "ic_cloth_straw_hat"),
            "cloth_red_tee" to Item("cloth_red_tee", "红白条纹短袖", ItemType.CLOTHING, "舒适休闲的条纹棉质T恤。", 600, 300, "ic_cloth_red_tee"),
            "cloth_sweater" to Item("cloth_sweater", "落叶暖色毛衣", ItemType.CLOTHING, "温暖厚实的针织毛衣，适合凉爽的秋天。", 1200, 600, "ic_cloth_sweater"),
            "cloth_starry_robe" to Item("cloth_starry_robe", "群星学者法袍", ItemType.CLOTHING, "绣着流光星轨的华丽长袍，感觉有神秘力量。", 4000, 2000, "ic_cloth_starry_robe"),
            "cloth_crown" to Item("cloth_crown", "皇家小王冠", ItemType.CLOTHING, "闪耀着纯金光芒、镶嵌蓝宝石的尊贵王冠。", 10000, 5000, "ic_cloth_crown"),

            // Furniture Items
            // Pastoral series
            "furn_pastoral_chair" to Item("furn_pastoral_chair", "田园风竹椅", ItemType.FURNITURE, "田园系列：用轻柔竹条编制的圆背小扶手椅。", 1200, 600, "ic_furn_pastoral_chair", "pastoral"),
            "furn_pastoral_table" to Item("furn_pastoral_table", "田园实木圆桌", ItemType.FURNITURE, "田园系列：桌角刻着雏菊浮雕的实木圆矮桌。", 2000, 1000, "ic_furn_pastoral_table", "pastoral"),
            "furn_pastoral_bed" to Item("furn_pastoral_bed", "花边碎叶单人床", ItemType.FURNITURE, "田园系列：铺着淡绿碎花棉被的舒适小床。", 3500, 1750, "ic_furn_pastoral_bed", "pastoral"),
            "furn_pastoral_wallpaper" to Item("furn_pastoral_wallpaper", "碎花壁纸", ItemType.WALLPAPER, "印有可爱碎花和浅绿藤蔓的温暖壁纸。", 1500, 750, "ic_furn_pastoral_wallpaper", "pastoral"),
            "furn_pastoral_flooring" to Item("furn_pastoral_flooring", "青草编织地板", ItemType.FLOORING, "散发着淡淡青草香气的日式榻榻米地板。", 1500, 750, "ic_furn_pastoral_flooring", "pastoral"),

            // Wooden series
            "furn_wood_stool" to Item("furn_wood_stool", "木制小凳子", ItemType.FURNITURE, "木头系列：用简单木块打磨成的粗犷圆凳。", 800, 400, "ic_furn_wood_stool", "wood"),
            "furn_wood_desk" to Item("furn_wood_desk", "学者木书桌", ItemType.FURNITURE, "木头系列：带两个小抽屉，散发松木香味的原木书桌。", 1800, 900, "ic_furn_wood_desk", "wood"),
            "furn_wood_wardrobe" to Item("furn_wood_wardrobe", "原木高衣柜", ItemType.FURNITURE, "木头系列：高大实用的两门原木衣柜，可以用来收纳服装。", 3000, 1500, "ic_furn_wood_wardrobe", "wood"),
            "furn_wood_wall" to Item("furn_wood_wall", "小木屋墙壁", ItemType.WALLPAPER, "铺满温馨横向松木条的度假小木屋质感墙面。", 1200, 600, "ic_furn_wood_wall", "wood"),
            "furn_wood_floor" to Item("furn_wood_floor", "红松木地板", ItemType.FLOORING, "温润的人字斜拼红木地板，踩上去很暖和。", 1200, 600, "ic_furn_wood_floor", "wood"),

            // Fruit series
            "furn_fruit_tv" to Item("furn_fruit_tv", "大苹果电视机", ItemType.FURNITURE, "水果系列：红彤彤的巨型苹果造型电视机，叶片是天线。", 2500, 1250, "ic_furn_fruit_tv", "fruit"),
            "furn_fruit_wardrobe" to Item("furn_fruit_wardrobe", "雪梨收纳衣帽架", ItemType.FURNITURE, "水果系列：一个胖乎乎的香梨切开露出的衣架子。", 2200, 1100, "ic_furn_fruit_wardrobe", "fruit"),
            "furn_fruit_chair" to Item("furn_fruit_chair", "香橙软绵小转椅", ItemType.FURNITURE, "水果系列：像一块多汁的橙子剖面，极为弹软舒适。", 1200, 600, "ic_furn_fruit_chair", "fruit"),
            "furn_fruit_wall" to Item("furn_fruit_wall", "热带水果壁纸", ItemType.WALLPAPER, "绘满柠檬、香蕉、橙子的色彩斑斓活跃壁纸。", 1600, 800, "ic_furn_fruit_wall", "fruit"),
            "furn_fruit_floor" to Item("furn_fruit_floor", "落叶森林地毯", ItemType.FLOORING, "织有绿油油树叶脉络的圆型防滑大地毯。", 1600, 800, "ic_furn_fruit_floor", "fruit"),

            // Star series
            "furn_star_lamp" to Item("furn_star_lamp", "流光星空灯", ItemType.FURNITURE, "星星系列：会发出柔和暖光，缓缓自转的五角星夜灯。", 4000, 2000, "ic_furn_star_lamp", "star"),
            "furn_star_chair" to Item("furn_star_chair", "月亮秋千椅", ItemType.FURNITURE, "星星系列：挂在金色弯月上的梦幻秋千椅，浪漫之至。", 6000, 3000, "ic_furn_star_chair", "star"),
            "furn_star_table" to Item("furn_star_table", "星团流砂桌", ItemType.FURNITURE, "星星系列：双层钢化玻璃桌面下封存着淡紫色发光流砂。", 5000, 2500, "ic_furn_star_table", "star"),
            "furn_star_wall" to Item("furn_star_wall", "深邃星空壁纸", ItemType.WALLPAPER, "自带银河流动微光，能看到繁星星座的暗夜壁纸。", 3000, 1500, "ic_furn_star_wall", "star"),
            "furn_star_floor" to Item("furn_star_floor", "宇宙科幻地板", ItemType.FLOORING, "踩在上面会扩散出星云涟漪的神奇微光地板。", 3000, 1500, "ic_furn_star_floor", "star"),

            // Ocean series
            "furn_ocean_bed" to Item("furn_ocean_bed", "深海贝壳珍珠床", ItemType.FURNITURE, "海洋系列：一个硕大的开合粉色贝壳内镶嵌雪白珍珠大床。", 5000, 2500, "ic_furn_ocean_bed", "ocean"),
            "furn_ocean_table" to Item("furn_ocean_table", "波纹珊瑚圆桌", ItemType.FURNITURE, "海洋系列：由数根火红珊瑚托起的水波纹玻璃矮桌。", 3200, 1600, "ic_furn_ocean_table", "ocean"),
            "furn_ocean_chair" to Item("furn_ocean_chair", "海葵懒人沙发", ItemType.FURNITURE, "海洋系列：像深海海葵一样，坐上去会被温柔包覆的沙发。", 1800, 900, "ic_furn_ocean_chair", "ocean"),
            "furn_ocean_wall" to Item("furn_ocean_wall", "深海波浪壁纸", ItemType.WALLPAPER, "能看到阳光折射进蔚蓝海底波纹的动态美感墙壁。", 2000, 1000, "ic_furn_ocean_wall", "ocean"),
            "furn_ocean_floor" to Item("furn_ocean_floor", "金黄贝壳沙滩地板", ItemType.FLOORING, "铺满细腻沙子，点缀着小小海螺贝壳的阳光沙滩地板。", 2000, 1000, "ic_furn_ocean_floor", "ocean")
        )

        val FISH_SPECIES = listOf(
            // River & Pond
            FishSpec("fish_goldfish", "金鱼", "Pond", listOf(1, 2, 3, 4), (0..23).toList(), "Common", 0.1f, 0.15f, 150),
            FishSpec("fish_carp", "鲤鱼", "River", listOf(1, 2, 3, 4), (0..23).toList(), "Common", 0.4f, 0.6f, 300),
            FishSpec("fish_salmon", "大马哈鱼", "River", listOf(3), (0..23).toList(), "Common", 0.6f, 0.9f, 700),
            FishSpec("fish_koi", "锦鲤", "Pond", listOf(1, 2, 3, 4), listOf(16, 17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Rare", 0.5f, 0.8f, 2000),
            FishSpec("fish_bass", "黑鲈鱼", "River", listOf(1, 2, 3, 4), (0..23).toList(), "Common", 0.3f, 0.5f, 240),
            FishSpec("fish_catfish", "鲶鱼", "River", listOf(2, 3), listOf(16, 17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Common", 0.5f, 0.7f, 400),
            FishSpec("fish_guppy", "孔雀鱼", "River", listOf(1, 2, 3), listOf(9, 10, 11, 12, 13, 14, 15, 16), "Common", 0.05f, 0.08f, 500),
            FishSpec("fish_eel", "河鳗", "River", listOf(2), listOf(16, 17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Common", 0.6f, 0.8f, 2000),
            
            // Sea
            FishSpec("fish_tuna", "金枪鱼", "Sea", listOf(4, 1), (0..23).toList(), "Legendary", 1.8f, 2.5f, 7000),
            FishSpec("fish_mackerel", "竹荚鱼", "Sea", listOf(1, 2, 3, 4), (0..23).toList(), "Common", 0.2f, 0.3f, 150),
            FishSpec("fish_red_snapper", "真鲷", "Sea", listOf(1, 2, 3, 4), (0..23).toList(), "Rare", 0.5f, 0.8f, 3000),
            FishSpec("fish_clownfish", "小丑鱼", "Sea", listOf(2), (0..23).toList(), "Common", 0.1f, 0.15f, 650),
            FishSpec("fish_squid", "鱿鱼", "Sea", listOf(1, 4), (0..23).toList(), "Common", 0.25f, 0.35f, 500),
            FishSpec("fish_octopus", "章鱼", "Sea", listOf(1, 2, 3, 4), (0..23).toList(), "Common", 0.4f, 0.6f, 1200),
            FishSpec("fish_butterfly", "海蝴蝶", "Sea", listOf(4), listOf(16, 17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Common", 0.03f, 0.05f, 1000),
            FishSpec("fish_blowfish", "红鳍东方鲀", "Sea", listOf(4), listOf(21, 22, 23, 0, 1, 2, 3, 4), "Rare", 0.3f, 0.45f, 5000),
            FishSpec("fish_shark", "大白鲨", "Sea", listOf(2), listOf(16, 17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), "Legendary", 2.2f, 3.5f, 15000),
            FishSpec("fish_ray", "鳐鱼", "Sea", listOf(2, 3), listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21), "Rare", 1.0f, 1.5f, 3000),
            FishSpec("fish_sunfish", "翻车鱼", "Sea", listOf(2), listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21), "Rare", 1.5f, 2.2f, 4000),
            FishSpec("fish_coelacanth", "矛尾鱼", "Sea", listOf(1, 2, 3, 4), (0..23).toList(), "Legendary", 1.5f, 2.0f, 15000) // (Only catches in rainy weather technically, but handles rain modifier in viewmodel)
        )

        val INSECT_SPECIES = listOf(
            InsectSpec("bug_monarch", "大桦斑蝶", "Flower", listOf(1, 2, 3), listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19), "Common", 140),
            InsectSpec("bug_swallowtail", "凤蝶", "Flower", listOf(1, 2, 3), listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19), "Common", 240),
            InsectSpec("bug_peacock", "乌鸦凤蝶", "Flower", listOf(1, 2), listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19), "Rare", 2500),
            InsectSpec("bug_moth", "飞蛾", "Light", listOf(1, 2, 3, 4), listOf(19, 20, 21, 22, 23, 0, 1, 2, 3, 4), "Common", 130),
            InsectSpec("bug_firefly", "萤火虫", "Flower", listOf(2), listOf(19, 20, 21, 22, 23, 0, 1, 2, 3, 4), "Common", 300),
            InsectSpec("bug_cicada", "油蝉", "Tree", listOf(2), listOf(8, 9, 10, 11, 12, 13, 14, 15, 16, 17), "Common", 250),
            InsectSpec("bug_honeybee", "蜜蜂", "Flower", listOf(1, 2), listOf(8, 9, 10, 11, 12, 13, 14, 15, 16, 17), "Common", 200),
            InsectSpec("bug_ladybug", "七星瓢虫", "Flower", listOf(1, 3), listOf(8, 9, 10, 11, 12, 13, 14, 15, 16, 17), "Common", 200),
            InsectSpec("bug_mantis", "螳螂", "Flower", listOf(1, 2, 3), listOf(8, 9, 10, 11, 12, 13, 14, 15, 16, 17), "Common", 430),
            InsectSpec("bug_cricket", "蟋蟀", "Grass", listOf(3), listOf(17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8), "Common", 130),
            InsectSpec("bug_beetle", "金龟子", "Tree", listOf(2), (0..23).toList(), "Common", 100),
            InsectSpec("bug_hercules", "赫拉克勒斯长戟大兜虫", "Tree", listOf(2), listOf(17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8), "Legendary", 12000),
            InsectSpec("bug_stag", "黄金鬼锹甲", "Tree", listOf(2), listOf(17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8), "Legendary", 12000),
            InsectSpec("bug_dragonfly", "大鬼蜻蜓", "Sky", listOf(2, 3), listOf(8, 9, 10, 11, 12, 13, 14, 15, 16, 17), "Rare", 4500),
            InsectSpec("bug_ant", "蚂蚁", "Grass", listOf(1, 2, 3, 4), (0..23).toList(), "Common", 80),
            InsectSpec("bug_flea", "跳蚤", "Grass", listOf(1, 2, 3, 4), (0..23).toList(), "Common", 70),
            InsectSpec("bug_snail", "蜗牛", "Tree", listOf(1, 2, 3, 4), (0..23).toList(), "Common", 250), // Appears on trees in rain
            InsectSpec("bug_pillbug", "鼠妇", "Grass", listOf(1, 2, 3, 4), listOf(23, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16), "Common", 250),
            InsectSpec("bug_walkingstick", "竹节虫", "Tree", listOf(2, 3), listOf(4, 5, 6, 7, 8, 17, 18, 19, 20, 21), "Common", 600),
            InsectSpec("bug_tarantula", "狼蛛", "Grass", listOf(3, 4), listOf(19, 20, 21, 22, 23, 0, 1, 2, 3, 4), "Legendary", 8000)
        )

        val FOSSIL_SPECIES = listOf(
            FossilSpec("fossil_trex_head", "霸王龙头部", "T-Rex", "Head", 6000),
            FossilSpec("fossil_trex_torso", "霸王龙躯干", "T-Rex", "Torso", 5500),
            FossilSpec("fossil_trex_tail", "霸王龙尾部", "T-Rex", "Tail", 5000),
            FossilSpec("fossil_tricera_head", "三角龙头部", "Triceratops", "Head", 5500),
            FossilSpec("fossil_tricera_torso", "三角龙躯干", "Triceratops", "Torso", 5000),
            FossilSpec("fossil_tricera_tail", "三角龙尾部", "Triceratops", "Tail", 4500),
            FossilSpec("fossil_ptera_head", "无齿翼龙头部", "Pterodactyl", "Head", 4500),
            FossilSpec("fossil_ptera_torso", "无齿翼龙胸部", "Pterodactyl", "Torso", 4000),
            FossilSpec("fossil_ptera_tail", "无齿翼龙翅膀", "Pterodactyl", "Tail", 4000),
            FossilSpec("fossil_stego_head", "剑龙头部", "Stegosaurus", "Head", 5000),
            FossilSpec("fossil_stego_torso", "剑龙脊椎", "Stegosaurus", "Torso", 4500),
            FossilSpec("fossil_stego_tail", "剑龙骨刺尾部", "Stegosaurus", "Tail", 4500),
            FossilSpec("fossil_mammoth_head", "猛犸象头骨", "Mammoth", "Head", 5500),
            FossilSpec("fossil_mammoth_torso", "猛犸象象牙躯干", "Mammoth", "Torso", 5000),
            FossilSpec("fossil_mammoth_tail", "猛犸象骨盆部", "Mammoth", "Tail", 4000)
        )

        val RECIPES = listOf(
            DIYRecipe("recipe_chair", "田园风竹椅", listOf("mat_wood" to 5, "mat_softwood" to 2), "furn_pastoral_chair", 10),
            DIYRecipe("recipe_table", "田园实木圆桌", listOf("mat_wood" to 10, "mat_hardwood" to 4), "furn_pastoral_table", 20),
            DIYRecipe("recipe_bed", "花边碎叶单人床", listOf("mat_wood" to 12, "mat_softwood" to 6, "mat_star" to 1), "furn_pastoral_bed", 30),
            DIYRecipe("recipe_stool", "木制小凳子", listOf("mat_softwood" to 4), "furn_wood_stool", 5),
            DIYRecipe("recipe_desk", "学者木书桌", listOf("mat_wood" to 8, "mat_hardwood" to 6, "mat_iron" to 2), "furn_wood_desk", 15),
            DIYRecipe("recipe_wardrobe", "原木高衣柜", listOf("mat_hardwood" to 12, "mat_iron" to 4), "furn_wood_wardrobe", 25),
            DIYRecipe("recipe_apple_tv", "大苹果电视机", listOf("mat_wood" to 8, "mat_clay" to 3, "mat_iron" to 2), "furn_fruit_tv", 30),
            DIYRecipe("recipe_shell_bed", "深海贝壳珍珠床", listOf("mat_stone" to 15, "mat_star" to 2, "mat_iron" to 5), "furn_ocean_bed", 60),
            DIYRecipe("recipe_star_lamp", "流光星空灯", listOf("mat_star" to 5, "mat_gold" to 1), "furn_star_lamp", 45),
            DIYRecipe("recipe_moon_chair", "月亮秋千椅", listOf("mat_star" to 10, "mat_gold" to 2, "mat_iron" to 5), "furn_star_chair", 90)
        )

        val CARDS = listOf(
            CombatCard("card_strike", "萌犬爪击", CombatCardType.ATTACK, 1, damage = 15, description = "阿奇挥舞利爪对敌怪造成 15 点伤害"),
            CombatCard("card_bite", "野性撕咬", CombatCardType.ATTACK, 2, damage = 35, description = "蓄力猛咬，造成 35 点高额伤害"),
            CombatCard("card_bark", "治愈咆哮", CombatCardType.HEAL, 1, heal = 20, description = "温柔叫声治愈心灵，恢复玩家 20 点体力"),
            CombatCard("card_shield", "顽皮格挡", CombatCardType.DEFEND, 1, shield = 20, description = "召唤泥巴护盾获得 20 点护甲"),
            CombatCard("card_star_nova", "群星坠落", CombatCardType.SPECIAL, 3, damage = 60, shield = 20, description = "引导星空碎片之力，流星锤重砸敌怪造成60点伤害并附加20护盾")
        )

        fun createDefault(): PlayerData {
            val pd = PlayerData()
            pd.playerName = "玩家小暖"
            pd.bells = 5000
            
            // Starter tools and materials
            pd.inventory.add(ITEMS["tool_rod"]!!)
            pd.inventory.add(ITEMS["tool_net"]!!)
            pd.inventory.add(ITEMS["tool_shovel"]!!)
            pd.inventory.add(ITEMS["tool_watering_can"]!!)
            
            pd.inventory.add(ITEMS["mat_wood"]!!)
            pd.inventory.add(ITEMS["mat_wood"]!!)
            pd.inventory.add(ITEMS["mat_stone"]!!)
            
            // Starter unlocked recipes
            pd.unlockedDIYRecipes.add("recipe_chair")
            pd.unlockedDIYRecipes.add("recipe_stool")
            pd.unlockedDIYRecipes.add("recipe_table")
            
            // Pre-spawn some default villagers
            pd.villagers.add(Villager("vil_kitty", "小猫妙妙", "Cat", "元气", 20, "喵酱", false, VillagerState.WALKING, 0.25f, 0.45f, 0xFFF06292.toInt()))
            pd.villagers.add(Villager("vil_sparky", "小狗皮皮", "Dog", "悠闲", 15, "皮皮", false, VillagerState.FISHING, 0.7f, 0.8f, 0xFF8D6E63.toInt()))
            pd.villagers.add(Villager("vil_bunbun", "兔子兔美", "Rabbit", "元气", 30, "小美", false, VillagerState.WALKING, 0.5f, 0.6f, 0xFFFFB74D.toInt()))
            pd.villagers.add(Villager("vil_teddy", "熊叔阿泰", "Bear", "暴躁", 10, "老泰", false, VillagerState.IN_HOUSE, 0.1f, 0.2f, 0xFF4CAF50.toInt()))
            pd.villagers.add(Villager("vil_fauna", "小鹿诗诗", "Deer", "成熟", 25, "诗仙子", false, VillagerState.WALKING, 0.6f, 0.35f, 0xFF81C784.toInt()))
            pd.villagers.add(Villager("vil_pip", "企鹅胖达", "Penguin", "悠闲", 5, "大肚皮", false, VillagerState.SLEEPING, 0.4f, 0.4f, 0xFF4FC3F7.toInt()))

            // Pre-seed some flowers in the yard
            pd.placedFlowers.add(PlacedFlower("flower_1", "Rose", "Red", 0.3f, 0.3f, 3, System.currentTimeMillis()))
            pd.placedFlowers.add(PlacedFlower("flower_2", "Rose", "White", 0.4f, 0.35f, 3, System.currentTimeMillis()))
            pd.placedFlowers.add(PlacedFlower("flower_3", "Tulip", "Yellow", 0.6f, 0.4f, 2, System.currentTimeMillis()))
            pd.placedFlowers.add(PlacedFlower("flower_4", "Mum", "White", 0.5f, 0.6f, 1, System.currentTimeMillis()))

            return pd
        }

        fun load(context: Context): PlayerData {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(SAVE_KEY, null)
            if (jsonString == null) {
                Log.d(TAG, "No save found, creating default data.")
                val data = createDefault()
                data.save(context)
                return data
            }
            return try {
                Log.d(TAG, "Loading save data from SharedPreferences.")
                val data = PlayerData()
                val json = JSONObject(jsonString)
                
                data.playerName = json.optString("playerName", "小暖")
                data.playerBirthday = json.optString("playerBirthday", "07-08")
                data.bells = json.optInt("bells", 5000)
                data.townRating = json.optDouble("townRating", 1.0).toFloat()
                data.currentHairstyle = json.optInt("currentHairstyle", 0)
                data.currentHairColor = json.optInt("currentHairColor", 0xFF5D4037.toInt())
                data.currentClothingId = json.optString("currentClothingId", "cloth_red_tee")
                data.houseExpansionLevel = json.optInt("houseExpansionLevel", 0)
                data.petName = json.optString("petName", "阿奇")
                data.petClothingId = json.optString("petClothingId", "none")
                data.fossilsDugCount = json.optInt("fossilsDugCount", 0)
                data.lastFossilSpawnTime = json.optLong("lastFossilSpawnTime", 0)
                data.lastDailyResetTime = json.optLong("lastDailyResetTime", 0)

                // Inventory
                val invArray = json.optJSONArray("inventory")
                if (invArray != null) {
                    for (i in 0 until invArray.length()) {
                        val itemId = invArray.getString(i)
                        val predefined = ITEMS[itemId]
                        if (predefined != null) {
                            data.inventory.add(predefined)
                        } else if (itemId.startsWith("fish_")) {
                            val fSpec = FISH_SPECIES.firstOrNull { it.id == itemId }
                            if (fSpec != null) {
                                data.inventory.add(Item(fSpec.id, fSpec.name, ItemType.FISH, "一条新鲜的${fSpec.name}，可出售或捐赠给博物馆。", fSpec.sellPrice * 2, fSpec.sellPrice, "ic_fish"))
                            }
                        } else if (itemId.startsWith("bug_")) {
                            val iSpec = INSECT_SPECIES.firstOrNull { it.id == itemId }
                            if (iSpec != null) {
                                data.inventory.add(Item(iSpec.id, iSpec.name, ItemType.INSECT, "一只活蹦乱跳的${iSpec.name}，可售卖或捐赠给博物馆。", iSpec.sellPrice * 2, iSpec.sellPrice, "ic_bug"))
                            }
                        } else if (itemId.startsWith("fossil_")) {
                            val fos = FOSSIL_SPECIES.firstOrNull { it.id == itemId }
                            if (fos != null) {
                                data.inventory.add(Item(fos.id, fos.name, ItemType.FOSSIL, "一件未鉴定的化石，拿去博物馆让馆长馆员看看吧！", fos.sellPrice * 2, fos.sellPrice, "ic_fossil"))
                            }
                        }
                    }
                }

                // Indoor furniture
                val indoorArray = json.optJSONArray("indoorFurniture")
                if (indoorArray != null) {
                    for (i in 0 until indoorArray.length()) {
                        val obj = indoorArray.getJSONObject(i)
                        data.indoorFurniture.add(PlacedFurniture(
                            obj.getString("id"),
                            obj.getString("itemId"),
                            obj.getDouble("x").toFloat(),
                            obj.getDouble("y").toFloat(),
                            obj.getDouble("rotation").toFloat(),
                            false
                        ))
                    }
                }

                // Outdoor furniture
                val outdoorArray = json.optJSONArray("outdoorFurniture")
                if (outdoorArray != null) {
                    for (i in 0 until outdoorArray.length()) {
                        val obj = outdoorArray.getJSONObject(i)
                        data.outdoorFurniture.add(PlacedFurniture(
                            obj.getString("id"),
                            obj.getString("itemId"),
                            obj.getDouble("x").toFloat(),
                            obj.getDouble("y").toFloat(),
                            obj.getDouble("rotation").toFloat(),
                            true
                        ))
                    }
                }

                // Donations
                val donFish = json.optJSONArray("museumDonatedFishes")
                if (donFish != null) {
                    for (i in 0 until donFish.length()) data.museumDonatedFishes.add(donFish.getString(i))
                }
                val donIns = json.optJSONArray("museumDonatedInsects")
                if (donIns != null) {
                    for (i in 0 until donIns.length()) data.museumDonatedInsects.add(donIns.getString(i))
                }
                val donFos = json.optJSONArray("museumDonatedFossils")
                if (donFos != null) {
                    for (i in 0 until donFos.length()) data.museumDonatedFossils.add(donFos.getString(i))
                }

                // Unlocked DIY Recipes
                val recipes = json.optJSONArray("unlockedDIYRecipes")
                if (recipes != null) {
                    for (i in 0 until recipes.length()) data.unlockedDIYRecipes.add(recipes.getString(i))
                }

                // Placed Flowers
                val flowers = json.optJSONArray("placedFlowers")
                if (flowers != null) {
                    for (i in 0 until flowers.length()) {
                        val obj = flowers.getJSONObject(i)
                        data.placedFlowers.add(PlacedFlower(
                            obj.getString("id"),
                            obj.getString("flowerType"),
                            obj.getString("color"),
                            obj.getDouble("x").toFloat(),
                            obj.getDouble("y").toFloat(),
                            obj.getInt("growthStage"),
                            obj.getLong("plantedTime"),
                            obj.optBoolean("watered", false)
                        ))
                    }
                }

                // Villagers
                val vils = json.optJSONArray("villagers")
                if (vils != null) {
                    for (i in 0 until vils.length()) {
                        val obj = vils.getJSONObject(i)
                        data.villagers.add(Villager(
                            obj.getString("id"),
                            obj.getString("name"),
                            obj.getString("animalType"),
                            obj.getString("personality"),
                            obj.getInt("friendship"),
                            obj.optString("nickname", ""),
                            obj.optBoolean("hasPhoto", false),
                            VillagerState.valueOf(obj.optString("currentActivity", "WALKING")),
                            obj.optDouble("activityX", 0.5).toFloat(),
                            obj.optDouble("activityY", 0.5).toFloat(),
                            obj.optInt("houseColor", 0xFF8D6E63.toInt())
                        ))
                    }
                } else {
                    // Re-seed default villagers if none exist
                    val def = createDefault()
                    data.villagers.addAll(def.villagers)
                }

                // Market stand items
                val market = json.optJSONArray("marketStandItems")
                if (market != null) {
                    for (i in 0 until market.length()) {
                        val obj = market.getJSONObject(i)
                        val predefined = ITEMS[obj.getString("itemId")]
                        if (predefined != null) {
                            data.marketStandItems.add(Pair(predefined, obj.getInt("price")))
                        }
                    }
                }

                // Crafting task
                val taskObj = json.optJSONObject("currentCraftingTask")
                if (taskObj != null) {
                    data.currentCraftingTask = CraftingTask(
                        taskObj.getString("recipeId"),
                        taskObj.getLong("startTime"),
                        taskObj.getLong("durationMs"),
                        taskObj.optBoolean("isCompleted", false)
                    )
                }

                // Fossil Spawns
                val fossils = json.optJSONArray("fossilSpawns")
                if (fossils != null) {
                    for (i in 0 until fossils.length()) {
                        val obj = fossils.getJSONObject(i)
                        data.fossilSpawns.add(Pair(obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat()))
                    }
                }

                data
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing save data, using default.", e)
                createDefault()
            }
        }
    }

    fun save(context: Context) {
        try {
            val json = JSONObject()
            json.put("playerName", playerName)
            json.put("playerBirthday", playerBirthday)
            json.put("bells", bells)
            json.put("townRating", townRating.toDouble())
            json.put("currentHairstyle", currentHairstyle)
            json.put("currentHairColor", currentHairColor)
            json.put("currentClothingId", currentClothingId)
            json.put("houseExpansionLevel", houseExpansionLevel)
            json.put("petName", petName)
            json.put("petClothingId", petClothingId)
            json.put("fossilsDugCount", fossilsDugCount)
            json.put("lastFossilSpawnTime", lastFossilSpawnTime)
            json.put("lastDailyResetTime", lastDailyResetTime)

            // Inventory
            val invArray = JSONArray()
            for (item in inventory) {
                invArray.put(item.id)
            }
            json.put("inventory", invArray)

            // Indoor furniture
            val indoorArray = JSONArray()
            for (furn in indoorFurniture) {
                val obj = JSONObject()
                obj.put("id", furn.id)
                obj.put("itemId", furn.itemId)
                obj.put("x", furn.x.toDouble())
                obj.put("y", furn.y.toDouble())
                obj.put("rotation", furn.rotation.toDouble())
                indoorArray.put(obj)
            }
            json.put("indoorFurniture", indoorArray)

            // Outdoor furniture
            val outdoorArray = JSONArray()
            for (furn in outdoorFurniture) {
                val obj = JSONObject()
                obj.put("id", furn.id)
                obj.put("itemId", furn.itemId)
                obj.put("x", furn.x.toDouble())
                obj.put("y", furn.y.toDouble())
                obj.put("rotation", furn.rotation.toDouble())
                outdoorArray.put(obj)
            }
            json.put("outdoorFurniture", outdoorArray)

            // Donations
            val donFish = JSONArray()
            for (id in museumDonatedFishes) donFish.put(id)
            json.put("museumDonatedFishes", donFish)

            val donIns = JSONArray()
            for (id in museumDonatedInsects) donIns.put(id)
            json.put("museumDonatedInsects", donIns)

            val donFos = JSONArray()
            for (id in museumDonatedFossils) donFos.put(id)
            json.put("museumDonatedFossils", donFos)

            // Unlocked DIY Recipes
            val recipes = JSONArray()
            for (id in unlockedDIYRecipes) recipes.put(id)
            json.put("unlockedDIYRecipes", recipes)

            // Placed Flowers
            val flowers = JSONArray()
            for (flower in placedFlowers) {
                val obj = JSONObject()
                obj.put("id", flower.id)
                obj.put("flowerType", flower.flowerType)
                obj.put("color", flower.color)
                obj.put("x", flower.x.toDouble())
                obj.put("y", flower.y.toDouble())
                obj.put("growthStage", flower.growthStage)
                obj.put("plantedTime", flower.plantedTime)
                obj.put("watered", flower.watered)
                flowers.put(obj)
            }
            json.put("placedFlowers", flowers)

            // Villagers
            val vils = JSONArray()
            for (v in villagers) {
                val obj = JSONObject()
                obj.put("id", v.id)
                obj.put("name", v.name)
                obj.put("animalType", v.animalType)
                obj.put("personality", v.personality)
                obj.put("friendship", v.friendship)
                obj.put("nickname", v.nickname)
                obj.put("hasPhoto", v.hasPhoto)
                obj.put("currentActivity", v.currentActivity.name)
                obj.put("activityX", v.activityX.toDouble())
                obj.put("activityY", v.activityY.toDouble())
                obj.put("houseColor", v.houseColor)
                vils.put(obj)
            }
            json.put("villagers", vils)

            // Market stand items
            val market = JSONArray()
            for (pair in marketStandItems) {
                val obj = JSONObject()
                obj.put("itemId", pair.first.id)
                obj.put("price", pair.second)
                market.put(obj)
            }
            json.put("marketStandItems", market)

            // Crafting task
            if (currentCraftingTask != null) {
                val obj = JSONObject()
                obj.put("recipeId", currentCraftingTask!!.recipeId)
                obj.put("startTime", currentCraftingTask!!.startTime)
                obj.put("durationMs", currentCraftingTask!!.durationMs)
                obj.put("isCompleted", currentCraftingTask!!.isCompleted)
                json.put("currentCraftingTask", obj)
            } else {
                json.put("currentCraftingTask", JSONObject.NULL)
            }

            // Fossil Spawns
            val fossils = JSONArray()
            for (pos in fossilSpawns) {
                val obj = JSONObject()
                obj.put("x", pos.first.toDouble())
                obj.put("y", pos.second.toDouble())
                fossils.put(obj)
            }
            json.put("fossilSpawns", fossils)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(SAVE_KEY, json.toString()).apply()
            Log.d(TAG, "Successfully saved PlayerData to SharedPreferences.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving PlayerData", e)
        }
    }
}
