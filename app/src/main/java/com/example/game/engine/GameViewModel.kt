package com.example.game.engine

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.data.PlayerData
import com.example.game.models.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Random

enum class GameScene {
    TOWN,
    HOME,
    MUSEUM,
    SHOP,
    FOREST,
    BATTLE,
    DIY,
    INVENTORY,
    PROFILE,
    CAFE_WORK,
    MARKET_STAND
}

enum class FishingState {
    IDLE,
    CASTING,
    WAITING_SHADOW,
    SHADOW_APPROACHING,
    BITE,
    REELING_SUCCESS,
    REELING_FAIL
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    
    // Core state
    val playerData = PlayerData.load(context)
    
    private val _scene = MutableStateFlow(GameScene.TOWN)
    val scene: StateFlow<GameScene> = _scene

    // Reactive wrapper values for easy Compose observation
    val bells = MutableStateFlow(playerData.bells)
    val townRating = MutableStateFlow(playerData.townRating)
    val currentClothing = MutableStateFlow(playerData.currentClothingId)
    val currentHairstyle = MutableStateFlow(playerData.currentHairstyle)
    
    val inventory = mutableStateListOf<Item>().apply { addAll(playerData.inventory) }
    val indoorFurniture = mutableStateListOf<PlacedFurniture>().apply { addAll(playerData.indoorFurniture) }
    val outdoorFurniture = mutableStateListOf<PlacedFurniture>().apply { addAll(playerData.outdoorFurniture) }
    val placedFlowers = mutableStateListOf<PlacedFlower>().apply { addAll(playerData.placedFlowers) }
    val villagers = mutableStateListOf<Villager>().apply { addAll(playerData.villagers) }
    val marketStand = mutableStateListOf<Pair<Item, Int>>().apply { addAll(playerData.marketStandItems) }
    
    // Dialogue system
    val activeDialogueSpeaker = MutableStateFlow<String?>(null)
    val activeDialogueText = MutableStateFlow<String?>(null)
    val activeDialogueChoices = mutableStateListOf<String>()
    var onDialogueChoiceSelected: ((Int) -> Unit)? = null
    var activeVillagerId: String? = null

    // Fishing State
    val fishingState = MutableStateFlow(FishingState.IDLE)
    var activeFishShadowX = 0f
    var activeFishShadowY = 0f
    var activeFishBobberX = 0.5f
    var activeFishBobberY = 0.7f
    var activeCaughtFish: FishSpec? = null
    private var fishingJob: Job? = null
    private var biteStartTime = 0L
    private var biteWindowMs = 700L

    // Bug Spawning & Catching
    data class ActiveBug(val id: String, val spec: InsectSpec, var x: Float, var y: Float, val speed: Float, var angle: Float)
    val activeBugs = mutableStateListOf<ActiveBug>()
    private var bugJob: Job? = null

    // Shop Daily Inventory (Randomizes each launch/reset)
    val shopItems = mutableStateListOf<Item>()
    
    // DIY Crafting State
    val activeCraftingTask = MutableStateFlow<CraftingTask?>(playerData.currentCraftingTask)
    val recipeList = mutableStateListOf<DIYRecipe>().apply { addAll(PlayerData.RECIPES) }

    // Cafe mini-game
    val cafeCustomerName = MutableStateFlow("")
    val cafeCustomerDialogue = MutableStateFlow("")
    val cafeTargetCoffee = MutableStateFlow("") // e.g. "特浓-全奶-无糖"
    val cafeSuccessBells = MutableStateFlow(0)
    var cafeSelectedBeans = "" // 柔和, 意式, 特浓
    var cafeSelectedMilk = "" // 无奶, 低脂, 全奶
    var cafeSelectedSugar = "" // 无糖, 微糖, 多糖

    // Combat State (System 5)
    val combatPetHp = MutableStateFlow(100)
    val combatPetMaxHp = MutableStateFlow(100)
    val combatPetShield = MutableStateFlow(0)
    val combatMonster = MutableStateFlow<CombatMonster?>(null)
    val combatDeck = mutableStateListOf<CombatCard>()
    val combatHand = mutableStateListOf<CombatCard>()
    val combatEnergy = MutableStateFlow(3)
    val combatTurnCount = MutableStateFlow(1)
    val combatLog = mutableStateListOf<String>()
    var combatOnFinish: ((Boolean) -> Unit)? = null

    // Visual indicators / notifications
    val gameNotification = MutableStateFlow<String?>(null)

    // Player coordinates inside town/scene (Canvas hooks into these)
    var playerX = 0.5f
    var playerY = 0.6f
    var targetPlayerX = 0.5f
    var targetPlayerY = 0.6f

    init {
        generateDailyShopItems()
        spawnFossilsAndBugs()
        startGlobalTickers()
        recalculateTownRating()
    }

    fun changeScene(newScene: GameScene) {
        _scene.value = newScene
        // Stop active mini-games when leaving
        if (newScene != GameScene.TOWN && newScene != GameScene.FOREST) {
            stopBugSpawning()
        } else {
            startBugSpawning()
        }
        if (newScene != GameScene.TOWN) {
            cancelFishing()
        }
        saveGame()
    }

    fun saveGame() {
        playerData.bells = bells.value
        playerData.townRating = townRating.value
        playerData.currentHairstyle = currentHairstyle.value
        playerData.currentHairColor = playerData.currentHairColor
        playerData.currentClothingId = currentClothing.value
        
        playerData.inventory.clear()
        playerData.inventory.addAll(inventory)
        
        playerData.indoorFurniture.clear()
        playerData.indoorFurniture.addAll(indoorFurniture)
        
        playerData.outdoorFurniture.clear()
        playerData.outdoorFurniture.addAll(outdoorFurniture)
        
        playerData.placedFlowers.clear()
        playerData.placedFlowers.addAll(placedFlowers)
        
        playerData.villagers.clear()
        playerData.villagers.addAll(villagers)
        
        playerData.marketStandItems.clear()
        playerData.marketStandItems.addAll(marketStand)
        
        playerData.currentCraftingTask = activeCraftingTask.value
        
        playerData.save(context)
    }

    private fun startGlobalTickers() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                // 1. Tick DIY Crafting
                val task = activeCraftingTask.value
                if (task != null && !task.isCompleted) {
                    val elapsed = System.currentTimeMillis() - task.startTime
                    if (elapsed >= task.durationMs) {
                        activeCraftingTask.value = task.copy(isCompleted = true)
                        playerData.currentCraftingTask = activeCraftingTask.value
                        showNotification("🔨 DIY 制作完成了！")
                    }
                }
                
                // 2. Randomly check if villagers wander around or check player's market stand
                tickVillagerAI()
            }
        }
    }

    private fun tickVillagerAI() {
        val rand = Random()
        // Randomly move villagers
        for (i in villagers.indices) {
            val v = villagers[i]
            if (rand.nextInt(30) == 0) {
                val newAct = when (rand.nextInt(4)) {
                    0 -> VillagerState.WALKING
                    1 -> VillagerState.FISHING
                    2 -> VillagerState.IN_HOUSE
                    else -> VillagerState.SLEEPING
                }
                villagers[i] = v.copy(
                    currentActivity = newAct,
                    activityX = 0.2f + rand.nextFloat() * 0.6f,
                    activityY = 0.2f + rand.nextFloat() * 0.6f
                )
            }
        }

        // Market stand customer purchase
        if (marketStand.isNotEmpty() && rand.nextInt(40) == 0) {
            val vil = villagers.randomOrNull()
            if (vil != null) {
                val standIndex = rand.nextInt(marketStand.size)
                val pair = marketStand[standIndex]
                val item = pair.first
                val price = pair.second
                
                // Villager buy check
                if (bells.value + price < 999999) {
                    marketStand.removeAt(standIndex)
                    bells.value += price
                    showNotification("🎁 ${vil.name} 以 ${price} 铃钱购买了你的 ${item.name}！")
                    saveGame()
                }
            }
        }
    }

    fun showNotification(msg: String) {
        viewModelScope.launch {
            gameNotification.value = msg
            delay(3500)
            if (gameNotification.value == msg) {
                gameNotification.value = null
            }
        }
    }

    // --- SYSTEM 1: TOWN & HOME ---
    fun expandHouse() {
        val cost = when (playerData.houseExpansionLevel) {
            0 -> 10000
            1 -> 30000
            2 -> 60000
            3 -> 120000
            else -> 0
        }
        if (cost > 0 && bells.value >= cost) {
            bells.value -= cost
            playerData.houseExpansionLevel++
            saveGame()
            showNotification("🏠 房屋成功扩建！空间变得更宽敞了！")
        } else {
            showNotification("❌ 铃钱不足或房屋已是最大等级。")
        }
    }

    fun customizeAppearance(hairIdx: Int, clothesId: String) {
        currentHairstyle.value = hairIdx
        currentClothing.value = clothesId
        saveGame()
        showNotification("✨ 外观装扮已更新！")
    }

    fun placeFurniture(item: Item, isOutdoors: Boolean) {
        val furnList = if (isOutdoors) outdoorFurniture else indoorFurniture
        if (inventory.remove(item)) {
            val rand = Random()
            val newFurn = PlacedFurniture(
                id = "furn_${System.currentTimeMillis()}",
                itemId = item.id,
                x = 0.3f + rand.nextFloat() * 0.4f,
                y = 0.3f + rand.nextFloat() * 0.4f,
                rotation = 0f,
                isOutdoors = isOutdoors
            )
            furnList.add(newFurn)
            recalculateTownRating()
            saveGame()
            showNotification("🛋️ 已放置 ${item.name}")
        }
    }

    fun pickupFurniture(furn: PlacedFurniture, isOutdoors: Boolean) {
        val furnList = if (isOutdoors) outdoorFurniture else indoorFurniture
        if (furnList.remove(furn)) {
            val item = PlayerData.ITEMS[furn.itemId]
            if (item != null) {
                inventory.add(item)
            } else if (furn.itemId.startsWith("fish_")) {
                val spec = PlayerData.FISH_SPECIES.firstOrNull { it.id == furn.itemId }
                if (spec != null) {
                    inventory.add(Item(spec.id, spec.name, ItemType.FISH, "一条新鲜的${spec.name}", spec.sellPrice * 2, spec.sellPrice, "ic_fish"))
                }
            }
            recalculateTownRating()
            saveGame()
            showNotification("🎒 已将 ${item?.name ?: "家具"} 收回背包")
        }
    }

    fun rotateFurniture(furn: PlacedFurniture, isOutdoors: Boolean) {
        val furnList = if (isOutdoors) outdoorFurniture else indoorFurniture
        val idx = furnList.indexOf(furn)
        if (idx != -1) {
            val currentRot = furnList[idx].rotation
            furnList[idx] = furnList[idx].copy(rotation = (currentRot + 90f) % 360f)
            saveGame()
        }
    }

    // --- SYSTEM 2: COLLECTING ---

    // Fishing
    fun startFishing() {
        if (fishingState.value != FishingState.IDLE) return
        
        val rod = inventory.firstOrNull { it.type == ItemType.TOOL && it.id == "tool_rod" }
        if (rod == null) {
            showNotification("❌ 你的背包里没有钓竿！")
            return
        }

        fishingState.value = FishingState.CASTING
        activeFishBobberX = playerX
        activeFishBobberY = (playerY + 0.18f).coerceAtMost(0.95f)

        fishingJob = viewModelScope.launch {
            delay(1500)
            fishingState.value = FishingState.WAITING_SHADOW
            
            // Random delay for shadow
            val waitSec = 2L + Random().nextInt(5)
            delay(waitSec * 1000)
            
            fishingState.value = FishingState.SHADOW_APPROACHING
            activeFishShadowX = activeFishBobberX + (if (Random().nextBoolean()) 0.15f else -0.15f)
            activeFishShadowY = activeFishBobberY
            
            // Nibbles simulation
            val nibbles = 1 + Random().nextInt(3)
            for (i in 0 until nibbles) {
                delay(1200)
                // Move shadow closer to bobber
                activeFishShadowX = activeFishBobberX + (activeFishShadowX - activeFishBobberX) * 0.4f
                activeFishShadowY = activeFishBobberY + (activeFishShadowY - activeFishBobberY) * 0.4f
            }
            
            // BITE!
            delay(1000)
            fishingState.value = FishingState.BITE
            activeFishShadowX = activeFishBobberX
            activeFishShadowY = activeFishBobberY
            biteStartTime = System.currentTimeMillis()
            
            // Determine active fish based on season and weather
            val season = TimeManager.getCurrentSeason().id
            val isRainy = TimeManager.getWeatherForToday() == Weather.RAINY
            
            val validFish = PlayerData.FISH_SPECIES.filter { spec ->
                spec.seasons.contains(season)
            }
            
            // Fallback default
            var chosenFish = PlayerData.FISH_SPECIES.first()
            if (validFish.isNotEmpty()) {
                val roll = Random().nextInt(100)
                chosenFish = if (roll > 85) {
                    validFish.filter { it.rarity == "Legendary" }.randomOrNull()
                        ?: validFish.filter { it.rarity == "Rare" }.randomOrNull() ?: validFish.random()
                } else if (roll > 60) {
                    validFish.filter { it.rarity == "Rare" }.randomOrNull() ?: validFish.random()
                } else {
                    validFish.filter { it.rarity == "Common" }.randomOrNull() ?: validFish.random()
                }
            }
            
            activeCaughtFish = chosenFish
            // Rarity scales reaction window
            biteWindowMs = when (chosenFish.rarity) {
                "Legendary" -> 350L
                "Rare" -> 500L
                else -> 750L
            }
            
            // Wait for bite timeout
            delay(biteWindowMs)
            if (fishingState.value == FishingState.BITE) {
                fishingState.value = FishingState.REELING_FAIL
                showNotification("🐟 鱼儿吃完饵溜走了...")
                delay(2000)
                fishingState.value = FishingState.IDLE
            }
        }
    }

    fun reelIn() {
        val state = fishingState.value
        if (state != FishingState.BITE) {
            cancelFishing()
            return
        }
        
        val elapsed = System.currentTimeMillis() - biteStartTime
        fishingJob?.cancel()
        
        if (elapsed <= biteWindowMs && activeCaughtFish != null) {
            fishingState.value = FishingState.REELING_SUCCESS
            val fish = activeCaughtFish!!
            
            val fishItem = Item(
                id = fish.id,
                name = fish.name,
                type = ItemType.FISH,
                description = "在小镇${fish.location}钓到的${fish.name}，体长 ${String.format("%.1f", fish.minSize + Random().nextFloat() * (fish.maxSize - fish.minSize))}m。罕见度：${fish.rarity}。",
                buyPrice = fish.sellPrice * 2,
                sellPrice = fish.sellPrice,
                iconId = "ic_fish"
            )
            
            inventory.add(fishItem)
            saveGame()
            
            viewModelScope.launch {
                triggerDialogue(
                    speaker = "系统消息",
                    text = "成功拉竿！\n你钓到了一条 【${fish.name}】！\n可以把它捐赠给博物馆，或者在商店卖掉换取铃钱！",
                    choices = listOf("收入背包"),
                    onChoice = {
                        fishingState.value = FishingState.IDLE
                    }
                )
            }
        } else {
            fishingState.value = FishingState.REELING_FAIL
            showNotification("❌ 拉竿太迟了！鱼线空空如也。")
            viewModelScope.launch {
                delay(1500)
                fishingState.value = FishingState.IDLE
            }
        }
    }

    private fun cancelFishing() {
        fishingJob?.cancel()
        fishingState.value = FishingState.IDLE
    }

    // Bug Spawning & Catching
    private fun startBugSpawning() {
        bugJob?.cancel()
        bugJob = viewModelScope.launch {
            while (true) {
                delay(4000)
                if (activeBugs.size < 4) {
                    val season = TimeManager.getCurrentSeason().id
                    val validBugs = PlayerData.INSECT_SPECIES.filter { it.seasons.contains(season) }
                    if (validBugs.isNotEmpty()) {
                        val spec = validBugs.random()
                        val rand = Random()
                        val bug = ActiveBug(
                            id = "bug_${System.currentTimeMillis()}",
                            spec = spec,
                            x = 0.1f + rand.nextFloat() * 0.8f,
                            y = 0.2f + rand.nextFloat() * 0.6f,
                            speed = 0.02f + rand.nextFloat() * 0.03f,
                            angle = rand.nextFloat() * 360f
                        )
                        activeBugs.add(bug)
                    }
                }
            }
        }
    }

    private fun stopBugSpawning() {
        bugJob?.cancel()
        activeBugs.clear()
    }

    fun catchBug(bug: ActiveBug) {
        val net = inventory.firstOrNull { it.type == ItemType.TOOL && it.id == "tool_net" }
        if (net == null) {
            showNotification("❌ 你的背包里没有捕虫网！")
            return
        }

        activeBugs.remove(bug)
        val bugItem = Item(
            id = bug.spec.id,
            name = bug.spec.name,
            type = ItemType.INSECT,
            description = "用捕虫网捕捉到的${bug.spec.name}。可以拿去卖钱或者捐给博物馆收藏展示。",
            buyPrice = bug.spec.sellPrice * 2,
            sellPrice = bug.spec.sellPrice,
            iconId = "ic_bug"
        )
        inventory.add(bugItem)
        saveGame()

        viewModelScope.launch {
            triggerDialogue(
                speaker = "捕虫成功",
                text = "哇！你成功挥网捕捉到了一只 【${bug.spec.name}】！\n小小的虫子在罐子里扑腾，可爱极了！",
                choices = listOf("好耶"),
                onChoice = {}
            )
        }
    }

    // Fossil Digging
    private fun spawnFossilsAndBugs() {
        val rand = Random()
        // Ensure some random star cracks are placed on the ground of the Town
        val now = System.currentTimeMillis()
        if (now - playerData.lastFossilSpawnTime > 3 * 3600 * 1000L || playerData.fossilSpawns.isEmpty()) {
            playerData.fossilSpawns.clear()
            for (i in 0 until 3) {
                playerData.fossilSpawns.add(Pair(0.15f + rand.nextFloat() * 0.7f, 0.3f + rand.nextFloat() * 0.5f))
            }
            playerData.lastFossilSpawnTime = now
            saveGame()
        }
    }

    fun digFossilAt(x: Float, y: Float) {
        val shovel = inventory.firstOrNull { it.type == ItemType.TOOL && it.id == "tool_shovel" }
        if (shovel == null) {
            showNotification("❌ 你的背包里没有铲子！")
            return
        }

        val crack = playerData.fossilSpawns.firstOrNull { Math.hypot((it.first - x).toDouble(), (it.second - y).toDouble()) < 0.08 }
        if (crack != null) {
            playerData.fossilSpawns.remove(crack)
            val roll = Random().nextInt(100)
            
            val dugItem = if (roll < 15) {
                // Ore drop
                val ore = listOf("mat_clay", "mat_iron", "mat_gold").random()
                PlayerData.ITEMS[ore]!!
            } else {
                // Fossil drop
                val spec = PlayerData.FOSSIL_SPECIES.random()
                Item(
                    id = spec.id,
                    name = "未鉴定的化石",
                    type = ItemType.FOSSIL,
                    description = "一块裹满泥土的古老岩石化石骨骼。需要交给博物馆鉴定其真实种属。",
                    buyPrice = spec.sellPrice,
                    sellPrice = spec.sellPrice / 2,
                    iconId = "ic_fossil",
                    extra = spec.id // stash correct specimen in extra
                )
            }

            inventory.add(dugItem)
            playerData.fossilsDugCount++
            recalculateTownRating()
            saveGame()

            viewModelScope.launch {
                triggerDialogue(
                    speaker = "挖掘成就",
                    text = "你用铲子奋力挖掘地面，挖出了一个 【${dugItem.name}】！\n也许馆长傅达能鉴定它到底是哪种古代巨兽！",
                    choices = listOf("收起来"),
                    onChoice = {}
                )
            }
        }
    }

    // DIY Crafting
    fun startCrafting(recipe: DIYRecipe) {
        if (activeCraftingTask.value != null) {
            showNotification("❌ 你已经在制作家具了，请等待或立即完成。")
            return
        }

        // Verify materials
        for (mat in recipe.materials) {
            val count = inventory.count { it.id == mat.first }
            if (count < mat.second) {
                showNotification("❌ 制作材料不足！需要 ${mat.second} 个 ${PlayerData.ITEMS[mat.first]?.name ?: "材料"}")
                return
            }
        }

        // Deduct materials
        for (mat in recipe.materials) {
            var deducted = 0
            val iterator = inventory.iterator()
            while (iterator.hasNext() && deducted < mat.second) {
                if (iterator.next().id == mat.first) {
                    iterator.remove()
                    deducted++
                }
            }
        }

        // Start Crafting
        val durationMs = recipe.craftDurationSec * 1000L
        val task = CraftingTask(recipe.id, System.currentTimeMillis(), durationMs)
        activeCraftingTask.value = task
        playerData.currentCraftingTask = task
        saveGame()
        showNotification("🔨 开始制作 ${recipe.name}...")
    }

    fun speedUpCrafting() {
        val task = activeCraftingTask.value ?: return
        // Instant completion for playability / review ease
        activeCraftingTask.value = task.copy(startTime = System.currentTimeMillis() - task.durationMs, isCompleted = true)
        playerData.currentCraftingTask = activeCraftingTask.value
        saveGame()
    }

    fun collectCraftedItem() {
        val task = activeCraftingTask.value ?: return
        if (!task.isCompleted) return

        val recipe = PlayerData.RECIPES.firstOrNull { it.id == task.recipeId }
        if (recipe != null) {
            val output = PlayerData.ITEMS[recipe.outputItemId]
            if (output != null) {
                inventory.add(output)
                showNotification("🎒 获得了制作好的 ${output.name}！")
            }
        }
        activeCraftingTask.value = null
        playerData.currentCraftingTask = null
        saveGame()
    }

    // --- SYSTEM 3: SOCIAL & VILLAGERS ---
    fun talkToVillager(v: Villager) {
        activeVillagerId = v.id
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val phaseStr = TimeManager.getCurrentDayPhase().nameCn
        
        // Base lines depending on personality
        val greeting = when (v.personality) {
            "元气" -> listOf(
                "嘿！伙伴！看到你真让人兴奋，今天天气真棒对吧！",
                "喵哈罗！今天我制定了超级闪亮的偶像排练计划哦！要一起来吗？",
                "啊！你穿这身衣服简直太搭啦！好时尚啊！"
            ).random()
            "悠闲" -> listOf(
                "啊，好巧啊...我刚刚在吃苹果派，嘴巴黏糊糊的...",
                "哈~呼...今天的微风吹着真舒服，我好想躺在草地上打个盹...",
                "你有没有看见远处的池塘？刚才那里的鱼影很大哦，快去钓鱼吧..."
            ).random()
            "成熟" -> listOf(
                "贵安，小家伙。在这个温暖的季节里漫步，心境也会变得优雅呢。",
                "小镇的生活节奏总是这么温柔，偶尔品一杯浓咖啡也是极好的。",
                "交友之道就像是培植花朵，需要极大的耐心和每日的真诚，你说是吧？"
            ).random()
            else -> listOf( // 暴躁
                "哼！是你啊，没事少来打扰本大爷，我正忙着在海边思考重要问题呢！",
                "呼，小家伙！走路打起精神来，看到那些化石裂缝了吗？快去挖出来！",
                "什么？好感度？哼，我才不在乎那玩意呢...（脸红）给你起个小绰号叫'暖暖'如何？"
            ).random()
        }

        // Daily friendship gain (cap 1-2 conversations)
        val index = villagers.indexOfFirst { it.id == v.id }
        if (index != -1) {
            val updated = villagers[index].copy(friendship = (v.friendship + 5).coerceAtMost(100))
            villagers[index] = updated
            if (updated.friendship >= 50 && !updated.hasPhoto) {
                // Give photo gift
                inventory.add(Item(
                    id = "photo_${v.id}",
                    name = "${v.name} 的珍贵照片",
                    type = ItemType.FURNITURE,
                    description = "村民${v.name}赠送给你的照片，是至高无上的深厚友谊证明！可以摆在房间桌子上。",
                    buyPrice = 9999,
                    sellPrice = 1,
                    iconId = "ic_photo"
                ))
                villagers[index] = updated.copy(hasPhoto = true)
                showNotification("📸 关系极好！${v.name} 赠送了你它的珍贵签名照！")
            }
            saveGame()
        }

        viewModelScope.launch {
            triggerDialogue(
                speaker = v.name,
                text = "【时间：$phaseStr】\n$greeting",
                choices = listOf("送礼物", "闲聊", "再见"),
                onChoice = { choiceIdx ->
                    when (choiceIdx) {
                        0 -> triggerGiftMenu(v)
                        1 -> {
                            val specialLine = "听说你捐赠了 ${playerData.museumDonatedFishes.size} 种鱼和 ${playerData.museumDonatedInsects.size} 种虫给博物馆，真的太了不起了！"
                            viewModelScope.launch {
                                triggerDialogue(v.name, specialLine, listOf("谢谢夸奖"), {})
                            }
                        }
                    }
                }
            )
        }
    }

    private fun triggerGiftMenu(v: Villager) {
        val giftable = inventory.filter { it.type != ItemType.TOOL }
        if (giftable.isEmpty()) {
            viewModelScope.launch {
                triggerDialogue(v.name, "诶？你口袋里空荡荡的，似乎没有合适的礼物给我呢。不过有这份心意我已经很高兴啦！", listOf("尴尬摸头"), {})
            }
            return
        }

        // Choose first items for preview
        val giftChoices = giftable.take(3).map { "赠送 ${it.name}" } + "算了吧"
        viewModelScope.launch {
            triggerDialogue(
                speaker = v.name,
                text = "礼物？哇，你要送我什么呀？我已经迫不及待了！",
                choices = giftChoices,
                onChoice = { idx ->
                    if (idx < giftable.size && idx < 3) {
                        val chosenGift = giftable[idx]
                        inventory.remove(chosenGift)
                        val index = villagers.indexOfFirst { it.id == v.id }
                        if (index != -1) {
                            villagers[index] = villagers[index].copy(friendship = (villagers[index].friendship + 15).coerceAtMost(100))
                        }
                        
                        // Reciprocate with a gift!
                        val rewardItem = PlayerData.ITEMS.values.filter { it.type != ItemType.TOOL }.random()
                        inventory.add(rewardItem)
                        saveGame()
                        
                        viewModelScope.launch {
                            triggerDialogue(
                                speaker = v.name,
                                text = "天呐！这是 【${chosenGift.name}】 吗？！我太喜欢了，真的非常感谢你！\n作为回礼，请你收下这件 【${rewardItem.name}】！",
                                choices = listOf("谢谢！"),
                                onChoice = {}
                            )
                        }
                    }
                }
            )
        }
    }

    // --- SYSTEM 4: TOWN DEVELOPMENT & SHOP ---
    private fun generateDailyShopItems() {
        val rand = Random()
        shopItems.clear()
        
        // Add random items
        val allItems = PlayerData.ITEMS.values.toList()
        val randomItems = allItems.filter { it.type != ItemType.TOOL }.shuffled(rand).take(4)
        shopItems.addAll(randomItems)
        
        // Ensure standard tools are always available for purchase
        shopItems.add(PlayerData.ITEMS["tool_shovel"]!!)
        shopItems.add(PlayerData.ITEMS["tool_rod"]!!)
        shopItems.add(PlayerData.ITEMS["tool_net"]!!)
        shopItems.add(PlayerData.ITEMS["tool_watering_can"]!!)
    }

    fun buyShopItem(item: Item) {
        if (bells.value >= item.buyPrice) {
            bells.value -= item.buyPrice
            inventory.add(item)
            recalculateTownRating()
            saveGame()
            showNotification("🛍️ 成功购买了 ${item.name}！")
        } else {
            showNotification("❌ 铃钱不足！")
        }
    }

    fun sellInventoryItem(item: Item) {
        if (inventory.remove(item)) {
            bells.value += item.sellPrice
            recalculateTownRating()
            saveGame()
            showNotification("🪙 成功出售 ${item.name}，获得 ${item.sellPrice} 铃钱！")
        }
    }

    fun registerMarketStandItem(item: Item, price: Int) {
        if (inventory.remove(item)) {
            marketStand.add(Pair(item, price))
            saveGame()
            showNotification("🏪 ${item.name} 已以 ${price} 铃钱上架至小镇摆摊位！")
        }
    }

    fun retrieveMarketStandItem(index: Int) {
        if (index in marketStand.indices) {
            val pair = marketStand.removeAt(index)
            inventory.add(pair.first)
            saveGame()
            showNotification("🎒 已收回上架的 ${pair.first.name}")
        }
    }

    // Part-time Cafe Work
    fun startCafePartTime() {
        cafeSelectedBeans = ""
        cafeSelectedMilk = ""
        cafeSelectedSugar = ""
        
        val randomCustomer = villagers.random()
        cafeCustomerName.value = randomCustomer.name
        
        // Custom preferences based on personality
        val (prefBeans, prefMilk, prefSugar) = when (randomCustomer.personality) {
            "元气" -> Triple("柔和", "全奶", "多糖")
            "悠闲" -> Triple("意式", "全奶", "多糖")
            "成熟" -> Triple("特浓", "低脂", "微糖")
            else -> Triple("特浓", "无奶", "无糖") // 暴躁
        }
        
        cafeTargetCoffee.value = "$prefBeans-$prefMilk-$prefSugar"
        cafeCustomerDialogue.value = "“老板！今天请给我冲一杯香香的咖啡，我喜欢【$prefBeans】烘焙的咖啡豆，配上【$prefMilk】，甜度要【$prefSugar】才行喵！”"
        cafeSuccessBells.value = 1000 + Random().nextInt(1000)
        
        changeScene(GameScene.CAFE_WORK)
    }

    fun submitCoffee() {
        val recipe = "$cafeSelectedBeans-$cafeSelectedMilk-$cafeSelectedSugar"
        if (recipe == cafeTargetCoffee.value) {
            bells.value += cafeSuccessBells.value
            saveGame()
            viewModelScope.launch {
                triggerDialogue(
                    speaker = cafeCustomerName.value,
                    text = "“呜哇！太完美了！这味道、奶香和甜度，简直就是我想象中的神级咖啡！太谢谢你了，这是给你的高额小费！”",
                    choices = listOf("收下 ${cafeSuccessBells.value} 铃钱"),
                    onChoice = {
                        changeScene(GameScene.TOWN)
                    }
                )
            }
        } else {
            val consolation = 300
            bells.value += consolation
            saveGame()
            viewModelScope.launch {
                triggerDialogue(
                    speaker = cafeCustomerName.value,
                    text = "“呃...这味道稍微有点不合我平时的口味呢。不过，还是很谢谢你帮我冲泡咖啡，这是辛苦费！”",
                    choices = listOf("收下 $consolation 铃钱"),
                    onChoice = {
                        changeScene(GameScene.TOWN)
                    }
                )
            }
        }
    }

    // --- SYSTEM 5: FOREST EXPLORATION & CARD BATTLE ---
    fun startForestExploration() {
        // Option to trigger combat or just harvest resources
        val roll = Random().nextInt(100)
        if (roll < 40) {
            // Initiate Card Combat
            setupCardCombat()
        } else {
            // Harvest rewards directly
            val rareDrops = listOf("mat_iron", "mat_clay", "mat_gold", "mat_star", "mat_horn")
            val drop = PlayerData.ITEMS[rareDrops.random()]!!
            inventory.add(drop)
            saveGame()
            viewModelScope.launch {
                triggerDialogue(
                    speaker = "神秘森林",
                    text = "你在清幽的神秘森林小径中散步，路过一棵发光的古树，在树根下的苔藓里捡到了罕见材料 【${drop.name}】！",
                    choices = listOf("太棒了！"),
                    onChoice = {
                        changeScene(GameScene.TOWN)
                    }
                )
            }
        }
    }

    private fun setupCardCombat() {
        combatPetHp.value = 100
        combatPetShield.value = 0
        combatTurnCount.value = 1
        combatLog.clear()
        
        val monsters = listOf(
            CombatMonster("m_slime", "糯米史莱姆", 50, 50, 8, 0xFF4CAF50.toInt()),
            CombatMonster("m_beetle", "大角金龟精", 80, 80, 15, 0xFF795548.toInt()),
            CombatMonster("m_treant", "愤怒森林树精", 120, 120, 12, 0xFF8D6E63.toInt())
        )
        combatMonster.value = monsters.random()
        
        // Assemble deck: 2 strikes, 1 bite, 1 bark, 1 shield
        combatDeck.clear()
        combatDeck.addAll(PlayerData.CARDS)
        combatDeck.shuffle()
        
        drawStartingHand()
        changeScene(GameScene.BATTLE)
        combatLog.add("⚔️ 一只 ${combatMonster.value!!.name} 挡住了你和阿奇的去路！战斗开始！")
    }

    private fun drawStartingHand() {
        combatHand.clear()
        combatEnergy.value = 3
        
        // Draw 3 cards from shuffled deck
        val list = combatDeck.shuffled()
        combatHand.addAll(list.take(3))
    }

    fun playCombatCard(card: CombatCard) {
        if (combatEnergy.value < card.cost) {
            showNotification("❌ 能量点不足！")
            return
        }
        
        combatHand.remove(card)
        combatEnergy.value -= card.cost
        
        // Apply card effects
        val mon = combatMonster.value ?: return
        if (card.damage > 0) {
            mon.hp = (mon.hp - card.damage).coerceAtLeast(0)
            combatLog.add("🐕 阿奇使用了【${card.name}】，对 ${mon.name} 造成了 ${card.damage} 点伤害！")
        }
        if (card.shield > 0) {
            combatPetShield.value += card.shield
            combatLog.add("🛡️ 叠加了 ${card.shield} 点泥巴护盾保护自身。")
        }
        if (card.heal > 0) {
            combatPetHp.value = (combatPetHp.value + card.heal).coerceAtMost(combatPetMaxHp.value)
            combatLog.add("💖 恢复了 ${card.heal} 点宠物生命值。")
        }

        // Check monster defeat
        if (mon.hp <= 0) {
            handleCombatVictory()
            return
        }

        // Draw card replacement
        if (combatHand.isEmpty()) {
            endPlayerTurn()
        }
    }

    fun endPlayerTurn() {
        viewModelScope.launch {
            // Monster Turn AI
            val mon = combatMonster.value ?: return@launch
            combatLog.add("👾 ${mon.name} 的回合！")
            delay(1000)
            
            // Random action: attack or shield
            if (Random().nextBoolean()) {
                val dmg = mon.attack
                val shield = combatPetShield.value
                val remainingDmg = (dmg - shield).coerceAtLeast(0)
                combatPetShield.value = (shield - dmg).coerceAtLeast(0)
                combatPetHp.value = (combatPetHp.value - remainingDmg).coerceAtLeast(0)
                
                combatLog.add("🔥 ${mon.name} 发动撞击，造成 $dmg 点攻击（护盾抵消后玩家受损 $remainingDmg 点）！")
            } else {
                combatLog.add("🔮 ${mon.name} 发出低沉咆哮，体表绿光环绕，浑身充满力量。")
            }

            // Check player defeat
            if (combatPetHp.value <= 0) {
                handleCombatDefeat()
                return@launch
            }

            // Next Turn
            combatTurnCount.value++
            drawStartingHand()
            combatLog.add("⏳ 回合 ${combatTurnCount.value}！能量点重置。")
        }
    }

    private fun handleCombatVictory() {
        val rareDrops = listOf("mat_iron", "mat_clay", "mat_gold", "mat_star", "mat_horn")
        val drop1 = PlayerData.ITEMS[rareDrops.random()]!!
        val drop2 = PlayerData.ITEMS["mat_iron"]!!
        
        inventory.add(drop1)
        inventory.add(drop2)
        saveGame()

        viewModelScope.launch {
            triggerDialogue(
                speaker = "战斗胜利",
                text = "阿奇和配合默契的你一同击败了强大的对手！\n你们在怪物倒下的地方，搜刮获得了稀有材料 【${drop1.name}】 和 【${drop2.name}】！",
                choices = listOf("收下奖励归来"),
                onChoice = {
                    changeScene(GameScene.TOWN)
                }
            )
        }
    }

    private fun handleCombatDefeat() {
        viewModelScope.launch {
            triggerDialogue(
                speaker = "战败归来",
                text = "哎呀！对手太强了，阿奇体力不支累倒了。\n你赶紧抱着阿奇回到了小镇家中。别气馁，下次准备更强的卡牌再战吧！",
                choices = listOf("好，先回家休息"),
                onChoice = {
                    changeScene(GameScene.HOME)
                }
            )
        }
    }

    // --- MUSEUM DONATIONS ---
    fun donateItem(item: Item) {
        if (item.type == ItemType.FOSSIL && item.name == "未鉴定的化石") {
            // Need to identify first!
            val specId = item.extra
            val spec = PlayerData.FOSSIL_SPECIES.firstOrNull { it.id == specId }
            if (spec != null) {
                // Perform identification
                inventory.remove(item)
                val identified = Item(spec.id, spec.name, ItemType.FOSSIL, "鉴定出的${spec.name}骨骼骨架。拼凑完整将呈现史前巨兽全貌！", spec.sellPrice * 2, spec.sellPrice, "ic_fossil")
                inventory.add(identified)
                saveGame()
                
                viewModelScope.launch {
                    triggerDialogue(
                        speaker = "傅达馆长",
                        text = "“呜呼！经过本人的精细清理和科学比对，这块化石正是史前巨兽 【${spec.name}】！太震撼了！请问您愿意将它捐赠给我们博物馆展出吗？”",
                        choices = listOf("愿意捐赠", "先自己留着"),
                        onChoice = { choice ->
                            if (choice == 0) {
                                donateIdentifiedItem(identified)
                            }
                        }
                    )
                }
            }
            return
        }

        donateIdentifiedItem(item)
    }

    private fun donateIdentifiedItem(item: Item) {
        val donated = when (item.type) {
            ItemType.FISH -> playerData.museumDonatedFishes.add(item.id)
            ItemType.INSECT -> playerData.museumDonatedInsects.add(item.id)
            ItemType.FOSSIL -> playerData.museumDonatedFossils.add(item.id)
            else -> false
        }

        if (donated) {
            inventory.remove(item)
            recalculateTownRating()
            saveGame()
            showNotification("🏛️ 成功捐赠 ${item.name}！它现在已在展厅陈列。")
        } else {
            showNotification("❌ 捐赠失败。该标本可能已捐赠过，或者该物品不适合捐赠。")
        }
    }

    // Recalculates town rating based on:
    // Donated items, purchased furniture, flowers planted, villagers friendship
    private fun recalculateTownRating() {
        val score = (playerData.museumDonatedFishes.size * 0.15f) +
                    (playerData.museumDonatedInsects.size * 0.15f) +
                    (playerData.museumDonatedFossils.size * 0.2f) +
                    (placedFlowers.size * 0.1f) +
                    (outdoorFurniture.size * 0.3f) +
                    (villagers.sumOf { it.friendship } * 0.01f)
        
        // Clamped between 1.0 and 5.0
        val finalRating = (1.0f + score).coerceAtMost(5.0f)
        townRating.value = String.format("%.1f", finalRating).toFloat()
        playerData.townRating = townRating.value
    }

    // --- DIALOGUE TRIGGER UTILITY ---
    fun triggerDialogue(speaker: String, text: String, choices: List<String>, onChoice: (Int) -> Unit) {
        activeDialogueSpeaker.value = speaker
        activeDialogueText.value = text
        activeDialogueChoices.clear()
        activeDialogueChoices.addAll(choices)
        onDialogueChoiceSelected = { idx ->
            activeDialogueSpeaker.value = null
            activeDialogueText.value = null
            activeDialogueChoices.clear()
            onChoice(idx)
        }
    }

    fun dismissDialogue() {
        onDialogueChoiceSelected?.invoke(0)
    }

    // Flower interaction: watering
    fun waterFlower(flower: PlacedFlower) {
        val can = inventory.firstOrNull { it.type == ItemType.TOOL && it.id == "tool_watering_can" }
        if (can == null) {
            showNotification("❌ 你的背包里没有洒水壶！")
            return
        }

        val idx = placedFlowers.indexOf(flower)
        if (idx != -1) {
            val f = placedFlowers[idx]
            placedFlowers[idx] = f.copy(watered = true)
            saveGame()
            showNotification("💦 花儿得到了滋润，表面散发出亮晶晶的露珠！")
        }
    }

    // Fast-Forward cheat (LEGENDARY PLAYABILITY FEATURE)
    fun cheatTimeSkip() {
        // Fast forward 1 day: grows flowers, resets shop, spawns fossils
        for (i in placedFlowers.indices) {
            val f = placedFlowers[i]
            val newStage = (f.growthStage + 1).coerceAtMost(3)
            placedFlowers[i] = f.copy(growthStage = newStage, watered = false)
        }
        
        generateDailyShopItems()
        spawnFossilsAndBugs()
        recalculateTownRating()
        saveGame()
        showNotification("⏳ 时光穿梭！小镇推进了24小时的真实生命轨迹。")
    }
}
