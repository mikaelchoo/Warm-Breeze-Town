package com.example.game.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import com.example.game.data.PlayerData
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.*
import com.example.game.models.*
import java.util.Calendar

// Theme colors
val WarmWood = Color(0xFF8D6E63)
val DarkWood = Color(0xFF4E342E)
val WarmSand = Color(0xFFFFFDD0)
val SoftLeafGreen = Color(0xFF81C784)
val SunsetPeach = Color(0xFFFFAB91)
val StarNebula = Color(0xFF7E57C2)

@Composable
fun GameUIOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val scene by viewModel.scene.collectAsState()
    val bellsVal by viewModel.bells.collectAsState()
    val ratingVal by viewModel.townRating.collectAsState()
    val notification by viewModel.gameNotification.collectAsState()
    
    // Sub-menu overlay states
    var showInventoryMenu by remember { mutableStateOf(false) }
    var showShopMenu by remember { mutableStateOf(false) }
    var showDIYMenu by remember { mutableStateOf(false) }
    var showMuseumCatalog by remember { mutableStateOf(false) }
    var showDressingMenu by remember { mutableStateOf(false) }
    var showMarketStandMenu by remember { mutableStateOf(false) }

    // Tick time dynamically
    var tickTimeStr by remember { mutableStateOf(TimeManager.getFormattedTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            tickTimeStr = TimeManager.getFormattedTime()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        
        // 1. TOP HUD (Always visible unless in intense combat card scene)
        if (scene != GameScene.BATTLE) {
            TopHUD(
                bells = bellsVal,
                rating = ratingVal,
                timeStr = tickTimeStr,
                dateStr = TimeManager.getFormattedDate(),
                seasonStr = TimeManager.getCurrentSeason().nameCn,
                weatherStr = TimeManager.getWeatherForToday().nameCn,
                holiday = TimeManager.getHolidayToday()
            )
        }

        // 2. MAIN BOTTOM CONTROL BAR (For scene switching / basic activities)
        if (scene != GameScene.BATTLE && scene != GameScene.CAFE_WORK) {
            BottomControlBar(
                scene = scene,
                viewModel = viewModel,
                onToggleInventory = { showInventoryMenu = !showInventoryMenu },
                onToggleShop = { showShopMenu = !showShopMenu },
                onToggleDIY = { showDIYMenu = !showDIYMenu },
                onToggleMuseum = { showMuseumCatalog = !showMuseumCatalog },
                onToggleDressing = { showDressingMenu = !showDressingMenu },
                onToggleMarket = { showMarketStandMenu = !showMarketStandMenu },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // 3. SPECIAL MINI-GAME SCENE UNDERLAYS / OVERLAYS
        when (scene) {
            GameScene.BATTLE -> {
                CardBattlePanel(viewModel)
            }
            GameScene.CAFE_WORK -> {
                CafeWorkPanel(viewModel)
            }
            else -> {}
        }

        // 4. FLOATING WINDOW DIALOGS (Compose Popup Windows)
        if (showInventoryMenu) {
            InventoryWindow(viewModel, onClose = { showInventoryMenu = false })
        }
        if (showShopMenu) {
            ShopWindow(viewModel, onClose = { showShopMenu = false })
        }
        if (showDIYMenu) {
            DIYWorkbenchWindow(viewModel, onClose = { showDIYMenu = false })
        }
        if (showMuseumCatalog) {
            MuseumCatalogWindow(viewModel, onClose = { showMuseumCatalog = false })
        }
        if (showDressingMenu) {
            DressingRoomWindow(viewModel, onClose = { showDressingMenu = false })
        }
        if (showMarketStandMenu) {
            MarketStandWindow(viewModel, onClose = { showMarketStandMenu = false })
        }

        // 5. PARCHMENT CONVERSATION DIALOGUE (Highest hierarchy popup)
        DialogueBox(viewModel)

        // 6. GLOBAL FLOATING TOAST NOTIFICATION
        notification?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 110.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE4E342E)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(symmetricPadding(16.dp, 10.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Yellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Utility to create padding easily
private fun symmetricPadding(horizontal: androidx.compose.ui.unit.Dp, vertical: androidx.compose.ui.unit.Dp) =
    PaddingValues(horizontal = horizontal, vertical = vertical)

@Composable
fun TopHUD(
    bells: Int,
    rating: Float,
    timeStr: String,
    dateStr: String,
    seasonStr: String,
    weatherStr: String,
    holiday: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp, start = 16.dp, end = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFDF6)),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, WarmWood)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wallet & Rating
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙 ", fontSize = 18.sp)
                        Text(
                            text = "$bells 铃钱",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkWood
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐小镇评分: ", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "$rating 星",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                    }
                }

                // Date, Time, Weather
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$dateStr ($seasonStr)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkWood
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = timeStr,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WarmWood
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🌤️ $weatherStr",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Holiday event ticker
            holiday?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(Color(0xFFFFF176).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🎉 今日活动中: $it",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkWood,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun BottomControlBar(
    scene: GameScene,
    viewModel: GameViewModel,
    onToggleInventory: () -> Unit,
    onToggleShop: () -> Unit,
    onToggleDIY: () -> Unit,
    onToggleMuseum: () -> Unit,
    onToggleDressing: () -> Unit,
    onToggleMarket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fishingState by viewModel.fishingState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Fishing contextual reel action button (Sits above bar)
            if (fishingState == FishingState.BITE) {
                Button(
                    onClick = { viewModel.reelIn() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = CircleShape,
                    elevation = ButtonDefaults.buttonElevation(8.dp),
                    modifier = Modifier
                        .size(76.dp)
                        .padding(bottom = 8.dp)
                        .testTag("reel_action_btn")
                ) {
                    Text("收竿!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else if (fishingState == FishingState.WAITING_SHADOW || fishingState == FishingState.SHADOW_APPROACHING) {
                Button(
                    onClick = { viewModel.reelIn() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(76.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Text("守候...", fontSize = 13.sp, color = Color.White)
                }
            } else if (scene == GameScene.TOWN || scene == GameScene.FOREST) {
                // Casting button when idle
                Button(
                    onClick = { viewModel.startFishing() },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftLeafGreen),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 8.dp)
                        .testTag("fish_cast_btn")
                ) {
                    Icon(Icons.Default.Waves, contentDescription = "Fish", tint = Color.White)
                }
            }

            // Standard Toolbar Row
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE4E342E)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Inventory
                    ToolbarIconButton(Icons.Default.Backpack, "口袋", "inv_btn", onClick = onToggleInventory)

                    // 2. Travel
                    if (scene == GameScene.TOWN) {
                        ToolbarIconButton(Icons.Default.Explore, "去森林", "forest_btn", onClick = {
                            viewModel.startForestExploration()
                        })
                    } else if (scene == GameScene.FOREST || scene == GameScene.HOME || scene == GameScene.MUSEUM || scene == GameScene.SHOP) {
                        ToolbarIconButton(Icons.Default.Home, "回小镇", "town_btn", onClick = {
                            viewModel.changeScene(GameScene.TOWN)
                        })
                    }

                    // 3. Inside / Outside House door toggle
                    if (scene == GameScene.TOWN) {
                        ToolbarIconButton(Icons.Default.MeetingRoom, "进屋", "enter_home_btn", onClick = {
                            viewModel.changeScene(GameScene.HOME)
                        })
                    } else if (scene == GameScene.HOME) {
                        ToolbarIconButton(Icons.Default.DoorBack, "出屋", "exit_home_btn", onClick = {
                            viewModel.changeScene(GameScene.TOWN)
                        })
                    }

                    // 4. DIY Crafting Table
                    ToolbarIconButton(Icons.Default.Handyman, "工作台", "diy_btn", onClick = onToggleDIY)

                    // 5. Museum catalog display
                    ToolbarIconButton(Icons.Default.AccountBalance, "博物馆", "museum_catalog_btn", onClick = onToggleMuseum)

                    // 6. Shop Trigger
                    if (scene == GameScene.TOWN) {
                        ToolbarIconButton(Icons.Default.Storefront, "商店", "shop_btn", onClick = {
                            viewModel.changeScene(GameScene.SHOP)
                            onToggleShop()
                        })
                    } else {
                        ToolbarIconButton(Icons.Default.Storefront, "商店", "shop_btn", onClick = onToggleShop)
                    }

                    // More Menu drop down features: Customize / Cafe / Stand / Cheat Time
                    var expandedMenu by remember { mutableStateOf(false) }
                    Box {
                        ToolbarIconButton(Icons.Default.MoreVert, "功能", "more_btn", onClick = { expandedMenu = !expandedMenu })
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier.background(Color(0xFFFFFDF6))
                        ) {
                            DropdownMenuItem(
                                text = { Text("💈 更换装扮发型", color = DarkWood) },
                                onClick = { expandedMenu = false; onToggleDressing() }
                            )
                            DropdownMenuItem(
                                text = { Text("☕ 咖啡馆打工", color = DarkWood) },
                                onClick = { expandedMenu = false; viewModel.startCafePartTime() }
                            )
                            DropdownMenuItem(
                                text = { Text("🏪 摆摊设点", color = DarkWood) },
                                onClick = { expandedMenu = false; onToggleMarket() }
                            )
                            DropdownMenuItem(
                                text = { Text("⏳ 时光跳跃(测试)", color = Color.Red, fontWeight = FontWeight.Bold) },
                                onClick = { expandedMenu = false; viewModel.cheatTimeSkip() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(WarmWood, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// --- SUB WINDOW TEMPLATE (Aesthetic scrolls/parchment overlays) ---
@Composable
fun BaseScrollWindow(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.78f)
                .clickable(enabled = false) {}, // prevent click-through
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF6)),
            elevation = CardDefaults.cardElevation(12.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(3.dp, WarmWood)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header scroll-bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmWood)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("close_window_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Window Scroll Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

// --- WINDOW 1: INVENTORY (🎒 PLAYER POCKETS) ---
@Composable
fun InventoryWindow(viewModel: GameViewModel, onClose: () -> Unit) {
    val items = viewModel.inventory
    val scene by viewModel.scene.collectAsState()
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    BaseScrollWindow("🎒 随身口袋", onClose) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("点击物品查看详情，并可以进行摆放、出售、或捐赠哦！", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            
            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(0.6f),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedItem == item) SunsetPeach else Color(0xFFEFEBE9))
                            .border(1.dp, WarmWood, RoundedCornerShape(12.dp))
                            .clickable { selectedItem = item }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val emoji = getEmojiForType(item)
                            Text(emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(item.name, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Details and actions
            selectedItem?.let { item ->
                Card(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkWood)
                            Text("💰售价: ${item.sellPrice} 铃钱", fontSize = 12.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        }
                        Text(item.description, fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Place indoors or outdoors contextually
                            if (item.type == ItemType.FURNITURE) {
                                Button(
                                    onClick = {
                                        viewModel.placeFurniture(item, isOutdoors = (scene == GameScene.TOWN))
                                        selectedItem = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftLeafGreen),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(if (scene == GameScene.TOWN) "放置在庭院" else "摆在家里", fontSize = 11.sp)
                                }
                            }

                            // Donate to Museum
                            if (scene == GameScene.TOWN && (item.type == ItemType.FISH || item.type == ItemType.INSECT || item.type == ItemType.FOSSIL)) {
                                Button(
                                    onClick = {
                                        viewModel.donateItem(item)
                                        selectedItem = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("捐赠给博物馆", fontSize = 11.sp)
                                }
                            }

                            // Sell in shop
                            if (scene == GameScene.SHOP) {
                                Button(
                                    onClick = {
                                        viewModel.sellInventoryItem(item)
                                        selectedItem = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("卖给狸猫商店", fontSize = 11.sp)
                                }
                            }

                            // Stash / Delete / Scrap
                            Button(
                                onClick = {
                                    viewModel.inventory.remove(item)
                                    selectedItem = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("丢弃", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- WINDOW 2: SHOP (🏪 NOOK SHOPPING) ---
@Composable
fun ShopWindow(viewModel: GameViewModel, onClose: () -> Unit) {
    val shopItems = viewModel.shopItems
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    val bells by viewModel.bells.collectAsState()

    BaseScrollWindow("🏪 狸猫百货商店", onClose) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("狸猫大叔每日精选商品，快来买一些精美壁纸和工具吧！", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(0.6f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shopItems) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.95f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedItem == item) SunsetPeach else Color(0xFFEFEBE9))
                            .border(1.dp, WarmWood, RoundedCornerShape(12.dp))
                            .clickable { selectedItem = item }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val emoji = getEmojiForType(item)
                            Text(emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(item.name, fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("💰${item.buyPrice}", fontSize = 10.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            selectedItem?.let { item ->
                Card(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkWood)
                        Text(item.description, fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = {
                                viewModel.buyShopItem(item)
                            },
                            enabled = (bells >= item.buyPrice),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmWood),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("确认购买 (${item.buyPrice} 铃钱)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- WINDOW 3: DIY WORKBENCH (🔨 RECIPES) ---
@Composable
fun DIYWorkbenchWindow(viewModel: GameViewModel, onClose: () -> Unit) {
    val recipes = viewModel.recipeList
    val activeTask by viewModel.activeCraftingTask.collectAsState()
    var selectedRecipe by remember { mutableStateOf<DIYRecipe?>(recipes.firstOrNull()) }
    val inventory = viewModel.inventory

    BaseScrollWindow("🔨 DIY 手作工作台", onClose) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Crafting Queue State
            activeTask?.let { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SunsetPeach.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, SunsetPeach)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val rec = recipes.firstOrNull { it.id == task.recipeId }
                        Column {
                            Text("🔨 正在制作: ${rec?.name ?: "物品"}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(if (task.isCompleted) "已制作完毕！" else "制作中，请等待...", fontSize = 11.sp)
                        }
                        
                        Row {
                            if (!task.isCompleted) {
                                Button(
                                    onClick = { viewModel.speedUpCrafting() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("秒成", fontSize = 10.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.collectCraftedItem() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftLeafGreen),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("收取", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.weight(1f)) {
                // Left recipe list
                LazyColumn(
                    modifier = Modifier
                        .weight(0.45f)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    items(recipes) { rec ->
                        val item = PlayerData.ITEMS[rec.outputItemId]
                        val isSelected = selectedRecipe == rec
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) WarmWood else Color.Transparent)
                                .clickable { selectedRecipe = rec }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = rec.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else DarkWood
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right recipe details
                Column(modifier = Modifier.weight(0.55f)) {
                    selectedRecipe?.let { rec ->
                        val outputItem = PlayerData.ITEMS[rec.outputItemId]
                        if (outputItem != null) {
                            Text(outputItem.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DarkWood)
                            Text("⏳ 耗时: ${rec.craftDurationSec} 秒", fontSize = 11.sp, color = Color.Gray)
                            Text(outputItem.description, fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                            
                            Divider(modifier = Modifier.padding(vertical = 6.dp))
                            Text("所需材料:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkWood)
                            
                            // Materials checklist
                            Column {
                                for (mat in rec.materials) {
                                    val matItem = PlayerData.ITEMS[mat.first]
                                    val owned = inventory.count { it.id == mat.first }
                                    val hasEnough = owned >= mat.second
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• ${matItem?.name ?: "未知材"}",
                                            fontSize = 11.sp,
                                            color = if (hasEnough) Color.Black else Color.Red
                                        )
                                        Text(
                                            text = "$owned / ${mat.second}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasEnough) Color(0xFF2E7D32) else Color.Red
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = { viewModel.startCrafting(rec) },
                                enabled = (activeTask == null),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftLeafGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("立即锻造", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- WINDOW 4: MUSEUM CATALOG (🏛️ SPECIMENS) ---
@Composable
fun MuseumCatalogWindow(viewModel: GameViewModel, onClose: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Fish, 1: Bugs, 2: Fossils
    val donatedFishes = viewModel.playerData.museumDonatedFishes
    val donatedInsects = viewModel.playerData.museumDonatedInsects
    val donatedFossils = viewModel.playerData.museumDonatedFossils

    BaseScrollWindow("🏛️ 暖风小镇博物馆馆藏", onClose) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Tab header
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = WarmWood
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("水族馆 (${donatedFishes.size}/20)", fontSize = 12.sp, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("昆虫馆 (${donatedInsects.size}/20)", fontSize = 12.sp, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("化石展厅 (${donatedFossils.size}/15)", fontSize = 12.sp, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display Spec list
            when (selectedTab) {
                0 -> {
                    // Aquarium
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(PlayerData.FISH_SPECIES) { fish ->
                            val isDonated = donatedFishes.contains(fish.id)
                            SpecimenRow(name = fish.name, detail = "分布: ${fish.location} | 罕见度: ${fish.rarity}", isDonated = isDonated, emoji = "🐟")
                        }
                    }
                }
                1 -> {
                    // Insect room
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(PlayerData.INSECT_SPECIES) { bug ->
                            val isDonated = donatedInsects.contains(bug.id)
                            SpecimenRow(name = bug.name, detail = "分布: ${bug.location} | 罕见度: ${bug.rarity}", isDonated = isDonated, emoji = "🦋")
                        }
                    }
                }
                2 -> {
                    // Fossils
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(PlayerData.FOSSIL_SPECIES) { fos ->
                            val isDonated = donatedFossils.contains(fos.id)
                            SpecimenRow(name = fos.name, detail = "分组: ${fos.skeletonGroup} | 展位: ${fos.partName}", isDonated = isDonated, emoji = "🦴")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecimenRow(name: String, detail: String, isDonated: Boolean, emoji: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDonated) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isDonated) emoji else "❓", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDonated) name else "？？？？ (未收集)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDonated) DarkWood else Color.Gray
                )
                Text(
                    text = if (isDonated) detail else "继续在岛屿上搜索捕捞解锁图鉴陈列展品",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            if (isDonated) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Donated", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// --- WINDOW 5: DRESSING ROOM (💈 CUSTOMIZE) ---
@Composable
fun DressingRoomWindow(viewModel: GameViewModel, onClose: () -> Unit) {
    var activeHair by remember { mutableStateOf(viewModel.playerData.currentHairstyle) }
    var activeClothes by remember { mutableStateOf(viewModel.playerData.currentClothingId) }

    val outfits = listOf(
        Pair("cloth_red_tee", "红白条纹T恤 👕"),
        Pair("cloth_straw_hat", "编织草帽 👒"),
        Pair("cloth_sweater", "落叶暖毛衣 🧥"),
        Pair("cloth_starry_robe", "群星学者法袍 🌌"),
        Pair("cloth_crown", "皇家小王冠 👑")
    )

    BaseScrollWindow("💈 暖风装扮更衣间", onClose) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            
            // Hairstyle grid selection
            Column {
                Text("选择发型款式:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkWood)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0..2) {
                        Button(
                            onClick = { activeHair = i },
                            colors = ButtonDefaults.buttonColors(containerColor = if (activeHair == i) SunsetPeach else WarmWood)
                        ) {
                            Text("发型 ${i + 1}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Clothing selection list
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text("挑选服饰:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkWood)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn {
                    items(outfits) { outfit ->
                        val isSelected = activeClothes == outfit.first
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    2.dp,
                                    if (isSelected) SunsetPeach else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { activeClothes = outfit.first },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Text(
                                text = outfit.second,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkWood
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.customizeAppearance(activeHair, activeClothes)
                    onClose()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SoftLeafGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存装扮外观", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- WINDOW 6: MARKET STAND STALL (🏪 SELLING) ---
@Composable
fun MarketStandWindow(viewModel: GameViewModel, onClose: () -> Unit) {
    val standItems = viewModel.marketStand
    val inventory = viewModel.inventory
    var selectedInvItem by remember { mutableStateOf<Item?>(null) }
    var priceInput by remember { mutableStateOf("100") }

    BaseScrollWindow("🏪 自助摆摊摊位", onClose) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("在此上架背包里的东西，村民逛街时会随机以定好价格买走哦！", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            
            // Placed items
            Text("当前正在上架销售的货物:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.weight(0.4f)) {
                if (standItems.isEmpty()) {
                    item {
                        Text("空空如也，快从下方口袋选择物品上架吧！", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                    }
                } else {
                    items(standItems.size) { idx ->
                        val pair = standItems[idx]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(pair.first.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("定价: ${pair.second} 铃钱", fontSize = 11.sp, color = Color(0xFFE65100))
                                }
                                Button(
                                    onClick = { viewModel.retrieveMarketStandItem(idx) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("收回", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Inventory picker to put on stand
            Text("选择想要摆摊贩售的背包装备:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(0.4f),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(inventory) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedInvItem == item) SunsetPeach else Color(0xFFEFEBE9))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .clickable { selectedInvItem = item }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.name, fontSize = 9.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                }
            }

            selectedInvItem?.let { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it.filter { c -> c.isDigit() } },
                        label = { Text("设定售价") },
                        modifier = Modifier.width(130.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val price = priceInput.toIntOrNull() ?: 100
                            viewModel.registerMarketStandItem(item, price)
                            selectedInvItem = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftLeafGreen)
                    ) {
                        Text("上架设摊")
                    }
                }
            }
        }
    }
}

// --- SPECIAL INTERACTIVE INTERFACES ---

// --- COFFEE MAKING CAFE PART-TIME JOB ---
@Composable
fun CafeWorkPanel(viewModel: GameViewModel) {
    val customer by viewModel.cafeCustomerName.collectAsState()
    val dialogue by viewModel.cafeCustomerDialogue.collectAsState()
    val spec by viewModel.cafeTargetCoffee.collectAsState()

    var beans by remember { mutableStateOf("") }
    var milk by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E1C0C).copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("☕ 暖风小镇深夜咖啡馆", fontSize = 22.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(14.dp))
            
            // Customer order description
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF6)),
                border = BorderStroke(2.dp, WarmWood),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("$customer 说:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkWood)
                    Text(dialogue, fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(vertical = 6.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interactive selections
            Text("1. 选择咖啡豆种类:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (b in listOf("柔和", "意式", "特浓")) {
                    Button(
                        onClick = { beans = b; viewModel.cafeSelectedBeans = b },
                        colors = ButtonDefaults.buttonColors(containerColor = if (beans == b) SunsetPeach else WarmWood)
                    ) {
                        Text(b)
                    }
                }
            }

            Text("2. 添加牛奶量:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (m in listOf("无奶", "低脂", "全奶")) {
                    Button(
                        onClick = { milk = m; viewModel.cafeSelectedMilk = m },
                        colors = ButtonDefaults.buttonColors(containerColor = if (milk == m) SunsetPeach else WarmWood)
                    ) {
                        Text(m)
                    }
                }
            }

            Text("3. 甜度设定:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (s in listOf("无糖", "微糖", "多糖")) {
                    Button(
                        onClick = { sugar = s; viewModel.cafeSelectedSugar = s },
                        colors = ButtonDefaults.buttonColors(containerColor = if (sugar == s) SunsetPeach else WarmWood)
                    ) {
                        Text(s)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { viewModel.submitCoffee() },
                enabled = (beans.isNotEmpty() && milk.isNotEmpty() && sugar.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(containerColor = SoftLeafGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("完成美味冲配，端呈咖啡", fontWeight = FontWeight.ExtraBold)
            }
            
            TextButton(
                onClick = { viewModel.changeScene(GameScene.TOWN) },
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text("返回小镇", color = Color.White)
            }
        }
    }
}

// --- SYSTEM 5: TURN-BASED CARD BATTLE PANEL ---
@Composable
fun CardBattlePanel(viewModel: GameViewModel) {
    val monster by viewModel.combatMonster.collectAsState()
    val hand = viewModel.combatHand
    val energy by viewModel.combatEnergy.collectAsState()
    val turn by viewModel.combatTurnCount.collectAsState()
    val logs = viewModel.combatLog

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 36.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚔️ 神秘森林危机", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("回合: $turn", color = Color.White, fontSize = 13.sp)
                Text("🔮 行动力: $energy / 3", color = Color(0xFFFFD54F), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Upper part: Battle Log console
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x99000000)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(10.dp),
                    reverseLayout = true
                ) {
                    items(logs.asReversed()) { log ->
                        Text(log, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Cards display Row (Bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                for (card in hand) {
                    Card(
                        modifier = Modifier
                            .width(96.dp)
                            .height(140.dp)
                            .border(1.dp, Color(card.color), RoundedCornerShape(10.dp))
                            .clickable { viewModel.playCombatCard(card) }
                            .testTag("battle_card_${card.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF6))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🔮 ${card.cost}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WarmWood,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Text(
                                text = card.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = DarkWood
                            )
                            Text(
                                text = card.description,
                                fontSize = 8.sp,
                                lineHeight = 10.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = when (card.type) {
                                    CombatCardType.ATTACK -> "⚔️伤害:${card.damage}"
                                    CombatCardType.DEFEND -> "🛡️护甲:${card.shield}"
                                    CombatCardType.HEAL -> "💖治愈:${card.heal}"
                                    else -> "🌟神技"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(card.color)
                            )
                        }
                    }
                }
            }

            // End Turn action button
            Button(
                onClick = { viewModel.endPlayerTurn() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("end_turn_btn")
            ) {
                Text("结束回合", fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

// --- CONVERSATION DIALOGUE BOX (SHEEPSKIN STYLE) ---
@Composable
fun DialogueBox(viewModel: GameViewModel) {
    val speaker by viewModel.activeDialogueSpeaker.collectAsState()
    val text by viewModel.activeDialogueText.collectAsState()
    val choices = viewModel.activeDialogueChoices

    if (speaker != null && text != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable {
                    if (choices.isEmpty()) {
                        viewModel.dismissDialogue()
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .padding(bottom = 110.dp)
                    .clickable(enabled = false) {}, // prevent clicking dialog background
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCEE)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(3.dp, WarmWood),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Speaker badge plaque
                    Box(
                        modifier = Modifier
                            .background(WarmWood, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = speaker!!,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Text
                    Text(
                        text = text!!,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = DarkWood,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialog Branch Options
                    if (choices.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (idx in choices.indices) {
                                Button(
                                    onClick = { viewModel.onDialogueChoiceSelected?.invoke(idx) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFEBE9)),
                                    border = BorderStroke(1.5.dp, WarmWood),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("dialog_choice_btn_$idx")
                                ) {
                                    Text(
                                        text = choices[idx],
                                        color = DarkWood,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "点击屏幕空白处继续...",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

// Helper to resolve cute emojis for items
private fun getEmojiForType(item: Item): String {
    return when (item.type) {
        ItemType.TOOL -> {
            when {
                item.id.contains("rod") -> "🎣"
                item.id.contains("shovel") -> "🪓"
                item.id.contains("net") -> "🦋"
                item.id.contains("watering_can") -> "💦"
                else -> "🎈"
            }
        }
        ItemType.FURNITURE -> {
            when {
                item.id.contains("bed") -> "🛏️"
                item.id.contains("chair") -> "🪑"
                item.id.contains("table") -> "🪵"
                item.id.contains("lamp") -> "💡"
                item.id.contains("tv") -> "📺"
                else -> "📦"
            }
        }
        ItemType.SEED -> "🌱"
        ItemType.FISH -> "🐟"
        ItemType.INSECT -> "🦋"
        ItemType.FOSSIL -> "🦴"
        ItemType.MATERIAL -> "🪵"
        ItemType.CLOTHING -> "👕"
        ItemType.FLOWER -> "🌷"
        ItemType.ORE -> "💎"
        else -> "🎁"
    }
}
