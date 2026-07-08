package com.example.game.ui

import android.graphics.*
import com.example.game.engine.*
import com.example.game.models.*
import com.example.game.data.PlayerData
import java.util.Calendar
import java.util.Random
import kotlin.math.sin

class GameRenderer {
    private val rand = Random()
    
    // Paints
    private val groundPaint = Paint().apply { isAntiAlias = true }
    private val skyPaint = Paint().apply { isAntiAlias = true }
    private val waterPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val beachPaint = Paint().apply { isAntiAlias = true; color = 0xFFF5F5DC.toInt() }
    private val woodPaint = Paint().apply { isAntiAlias = true; color = 0xFF8D6E63.toInt() }
    private val housePaint = Paint().apply { isAntiAlias = true }
    private val roofPaint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val uiPaint = Paint().apply { isAntiAlias = true }
    private val characterPaint = Paint().apply { isAntiAlias = true }
    private val leafPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

    // Weather Particles
    private var lastParticleTime = System.currentTimeMillis()
    private val weatherParticles = mutableListOf<Particle>()
    
    data class Particle(var x: Float, var y: Float, val speedX: Float, val speedY: Float, val size: Float, val color: Int)

    fun draw(canvas: Canvas, width: Int, height: Int, vm: GameViewModel, scrollX: Float) {
        val season = TimeManager.getCurrentSeason()
        val phase = TimeManager.getCurrentDayPhase()
        val weather = TimeManager.getWeatherForToday()
        
        // 1. Draw based on current scene
        when (vm.scene.value) {
            GameScene.TOWN -> drawTown(canvas, width, height, vm, scrollX, season, phase, weather)
            GameScene.HOME -> drawHomeInterior(canvas, width, height, vm, season, phase)
            GameScene.MUSEUM -> drawMuseumInterior(canvas, width, height, vm)
            GameScene.FOREST -> drawForest(canvas, width, height, vm, scrollX, season, phase, weather)
            GameScene.BATTLE -> drawBattleScene(canvas, width, height, vm)
            else -> {
                // Background default
                canvas.drawColor(0xFFFFFDD0.toInt()) // Soft cream watercolor
            }
        }

        // 2. Draw Weather Effects & Day-Cycle Time Overlays
        drawTimeAndWeatherOverlays(canvas, width, height, phase, weather)
    }

    private fun drawTown(
        canvas: Canvas, width: Int, height: Int, vm: GameViewModel, scrollX: Float,
        season: Season, phase: DayPhase, weather: Weather
    ) {
        val worldWidth = width * 3f
        val skyHeight = height * 0.35f
        val groundY = height * 0.35f

        // Draw Sky
        val skyGrad = when (phase) {
            DayPhase.DAY -> LinearGradient(0f, 0f, 0f, skyHeight, 0xFF81D4FA.toInt(), 0xFFE1F5FE.toInt(), Shader.TileMode.CLAMP)
            DayPhase.DUSK -> LinearGradient(0f, 0f, 0f, skyHeight, 0xFFE65100.toInt(), 0xFFFFB74D.toInt(), Shader.TileMode.CLAMP)
            DayPhase.NIGHT -> LinearGradient(0f, 0f, 0f, skyHeight, 0xFF1A237E.toInt(), 0xFF311B92.toInt(), Shader.TileMode.CLAMP)
        }
        skyPaint.shader = skyGrad
        canvas.drawRect(0f, 0f, width.toFloat(), skyHeight, skyPaint)

        // Draw stars at night
        if (phase == DayPhase.NIGHT) {
            val r = Random(42)
            paintStarTwinkle(canvas, width, skyHeight, r)
        }

        // Draw Sun/Moon
        val timeSec = (System.currentTimeMillis() / 1000) % 3600
        val sunX = (width * 0.2f + (timeSec / 3600f) * width * 0.6f)
        if (phase != DayPhase.NIGHT) {
            // Sun
            groundPaint.color = 0xFFFFEB3B.toInt()
            canvas.drawCircle(sunX, skyHeight * 0.35f, 45f, groundPaint)
        } else {
            // Moon
            groundPaint.color = 0xFFFFF9C4.toInt()
            canvas.drawCircle(width * 0.8f, skyHeight * 0.3f, 35f, groundPaint)
        }

        // Overlapping watercolor hills in the distance
        drawHills(canvas, width, skyHeight, scrollX, season)

        // Draw Ground (panned with scrollX)
        val groundColor = when (season) {
            Season.SPRING -> 0xFF81C784.toInt() // Soft pinkish grass green
            Season.SUMMER -> 0xFF4CAF50.toInt() // Lush bright green
            Season.AUTUMN -> 0xFFFFB74D.toInt() // Golden amber grass
            Season.WINTER -> 0xFFECEFF1.toInt() // Snowy ice-white blue
        }
        groundPaint.shader = null
        groundPaint.color = groundColor
        canvas.drawRect(0f, groundY, width.toFloat(), height.toFloat(), groundPaint)

        // Apply panning translate
        canvas.save()
        canvas.translate(-scrollX, 0f)

        // Draw Beach & Ocean Shore on the far right (virtual coordinate 2.2 * width to 3.0 * width)
        beachPaint.color = if (season == Season.WINTER) 0xFFCFD8DC.toInt() else 0xFFFFF59D.toInt()
        canvas.drawRect(worldWidth * 0.75f, groundY, worldWidth, height.toFloat(), beachPaint)
        
        waterPaint.color = if (phase == DayPhase.NIGHT) 0xFF0D47A1.toInt() else 0xFF29B6F6.toInt()
        canvas.drawRect(worldWidth * 0.83f, groundY, worldWidth, height.toFloat(), waterPaint)
        
        // Draw Wave Foam
        waterPaint.color = 0x80FFFFFF.toInt()
        val waveWobble = sin((System.currentTimeMillis() / 800.0)).toFloat() * 12f
        canvas.drawRect(worldWidth * 0.83f + waveWobble, groundY, worldWidth * 0.84f + waveWobble, height.toFloat(), waterPaint)

        // Draw River flowing vertically (virtual coordinates say 0.6 * width to 0.75 * width)
        waterPaint.color = if (phase == DayPhase.NIGHT) 0xFF1565C0.toInt() else 0xFF4FC3F7.toInt()
        canvas.drawRect(worldWidth * 0.22f, groundY, worldWidth * 0.28f, height.toFloat(), waterPaint)
        
        // River Bridge
        woodPaint.color = 0xFF8D6E63.toInt()
        canvas.drawRect(worldWidth * 0.21f, height * 0.6f, worldWidth * 0.29f, height * 0.68f, woodPaint)
        groundPaint.color = 0xFF5D4037.toInt()
        canvas.drawRect(worldWidth * 0.21f, height * 0.6f, worldWidth * 0.22f, height * 0.68f, groundPaint)
        canvas.drawRect(worldWidth * 0.28f, height * 0.6f, worldWidth * 0.29f, height * 0.68f, groundPaint)

        // Draw Plaza Town Square (around 1.0 * width)
        groundPaint.shader = null
        groundPaint.color = 0xFFCFD8DC.toInt() // Stone bricks
        canvas.drawRoundRect(worldWidth * 0.32f, groundY + 80f, worldWidth * 0.44f, height - 100f, 25f, 25f, groundPaint)
        
        // Plaza bulletin board / flag
        woodPaint.color = 0xFF5D4037.toInt()
        canvas.drawRect(worldWidth * 0.33f, groundY + 40f, worldWidth * 0.335f, groundY + 140f, woodPaint)
        groundPaint.color = 0xFFF48FB1.toInt() // pink flag
        canvas.drawRect(worldWidth * 0.335f, groundY + 40f, worldWidth * 0.36f, groundY + 80f, groundPaint)

        // Draw Buildings
        // 1. Player's house (around worldWidth * 0.12f)
        drawCuteHouse(canvas, worldWidth * 0.1f, groundY + 50f, "我的家", 0xFFE0F7FA.toInt(), 0xFFE57373.toInt(), vm.playerData.houseExpansionLevel)
        
        // 2. Nook's Cranny Shop (around worldWidth * 0.52f)
        drawCuteHouse(canvas, worldWidth * 0.48f, groundY + 50f, "狸猫商店", 0xFFFFF9C4.toInt(), 0xFF4FC3F7.toInt(), 2)

        // 3. Museum (around worldWidth * 0.64f)
        drawMuseumFacade(canvas, worldWidth * 0.62f, groundY + 40f)

        // Draw Flowers in yard
        for (flower in vm.placedFlowers) {
            drawFlowerSpec(canvas, worldWidth * 0.08f + flower.x * worldWidth * 0.12f, groundY + 220f + flower.y * 180f, flower)
        }

        // Draw Star Fossil Cracks on ground
        for (fossil in vm.playerData.fossilSpawns) {
            drawFossilCrack(canvas, fossil.first * worldWidth, groundY + 220f + fossil.second * (height - groundY - 350f))
        }

        // Draw Outdoor Furniture
        for (furn in vm.outdoorFurniture) {
            drawPlacedFurnitureIcon(canvas, furn.x * worldWidth, groundY + 120f + furn.y * (height - groundY - 320f), furn.itemId, furn.rotation)
        }

        // Draw Trees
        drawTreesInTown(canvas, worldWidth, groundY, height, season)

        // Draw Villagers wandering
        for (v in vm.villagers) {
            val vx = v.activityX * worldWidth
            val vy = groundY + 180f + v.activityY * (height - groundY - 300f)
            drawAnimalVillager(canvas, vx, vy, v)
        }

        // Draw Bugs crawling/flying
        for (bug in vm.activeBugs) {
            drawActiveBug(canvas, bug.x * worldWidth, groundY + 100f + bug.y * (height - groundY - 260f), bug)
        }

        // Update Bug position path randomly
        updateBugsMovement(vm, worldWidth, height, groundY)

        // Draw Player Character (Centered on screen horizontally usually, but calculated via playerX relative to world)
        val pVisualX = vm.playerX * worldWidth
        val pVisualY = groundY + 150f + vm.playerY * (height - groundY - 280f)
        drawPlayerCharacter(canvas, pVisualX, pVisualY, vm)

        // Draw active fishing indicator bobber
        if (vm.fishingState.value != FishingState.IDLE) {
            drawFishingBobber(canvas, vm.activeFishBobberX * worldWidth, groundY + 150f + vm.activeFishBobberY * (height - groundY - 280f), vm)
        }

        canvas.restore()
    }

    private fun drawHomeInterior(canvas: Canvas, width: Int, height: Int, vm: GameViewModel, season: Season, phase: DayPhase) {
        val sizeLevel = vm.playerData.houseExpansionLevel
        val border = 40f + (4 - sizeLevel) * 20f
        
        // Cozy Wallpaper base
        groundPaint.shader = null
        groundPaint.color = 0xFFD7CCC8.toInt() // Soft warm beige walls
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), groundPaint)

        // Drawing wall pattern (e.g., vertical stripes)
        val stripePaint = Paint().apply { color = 0x15000000; strokeWidth = 10f }
        var sx = 0f
        while (sx < width) {
            canvas.drawLine(sx, 0f, sx, height.toFloat(), stripePaint)
            sx += 40f
        }

        // Draw Wood Baseboard
        woodPaint.color = 0xFF5D4037.toInt()
        canvas.drawRect(0f, height * 0.45f, width.toFloat(), height * 0.48f, woodPaint)

        // Floor Wood planks (Planes)
        groundPaint.color = 0xFF8D6E63.toInt() // Parquet wood
        canvas.drawRect(0f, height * 0.48f, width.toFloat(), height.toFloat(), groundPaint)
        
        val plankPaint = Paint().apply { color = 0x30000000; strokeWidth = 3f }
        var plankY = height * 0.48f
        while (plankY < height) {
            canvas.drawLine(0f, plankY, width.toFloat(), plankY, plankPaint)
            plankY += 50f
        }

        // Draw Cozy window with light shifting depending on day phase
        val winLeft = width * 0.15f
        val winTop = height * 0.15f
        val winRight = width * 0.4f
        val winBottom = height * 0.33f
        
        // Window Frame
        woodPaint.color = 0xFF3E2723.toInt()
        canvas.drawRoundRect(winLeft - 8f, winTop - 8f, winRight + 8f, winBottom + 8f, 10f, 10f, woodPaint)
        
        // Window Glass
        groundPaint.color = when (phase) {
            DayPhase.DAY -> 0xFFB3E5FC.toInt()
            DayPhase.DUSK -> 0xFFFFCC80.toInt()
            DayPhase.NIGHT -> 0xFF1A237E.toInt()
        }
        canvas.drawRect(winLeft, winTop, winRight, winBottom, groundPaint)
        
        // Window Pane grids
        woodPaint.color = 0xFF3E2723.toInt()
        canvas.drawLine((winLeft + winRight)/2f, winTop, (winLeft + winRight)/2f, winBottom, woodPaint)
        canvas.drawLine(winLeft, (winTop + winBottom)/2f, winRight, (winTop + winBottom)/2f, woodPaint)

        // Draw Placed Furniture inside room
        for (furn in vm.indoorFurniture) {
            val fx = border + furn.x * (width - border * 2)
            val fy = height * 0.52f + furn.y * (height * 0.44f)
            drawPlacedFurnitureIcon(canvas, fx, fy, furn.itemId, furn.rotation)
        }

        // Draw Player Character inside home
        val px = border + vm.playerX * (width - border * 2)
        val py = height * 0.55f + vm.playerY * (height * 0.38f)
        drawPlayerCharacter(canvas, px, py, vm)
    }

    private fun drawMuseumInterior(canvas: Canvas, width: Int, height: Int, vm: GameViewModel) {
        // High ceilings, greek columns, 3 doors representing aquarium, insect hall, fossil hall
        groundPaint.shader = null
        groundPaint.color = 0xFFECEFF1.toInt() // Greek white marble wall
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), groundPaint)

        // Marble floor
        groundPaint.color = 0xFFCFD8DC.toInt()
        canvas.drawRect(0f, height * 0.5f, width.toFloat(), height.toFloat(), groundPaint)

        // Draw Columns
        val colPaint = Paint().apply { color = 0xFFB0BEC5.toInt(); style = Paint.Style.FILL }
        val strokePaint = Paint().apply { color = 0xFF78909C.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f }
        
        canvas.drawRect(width * 0.05f, height * 0.1f, width * 0.15f, height * 0.82f, colPaint)
        canvas.drawRect(width * 0.05f, height * 0.1f, width * 0.15f, height * 0.82f, strokePaint)
        canvas.drawRect(width * 0.85f, height * 0.1f, width * 0.95f, height * 0.82f, colPaint)
        canvas.drawRect(width * 0.85f, height * 0.1f, width * 0.95f, height * 0.82f, strokePaint)

        // Draw 3 exhibit gates
        drawExhibitGate(canvas, width * 0.23f, height * 0.45f, "水族馆\n(鱼类标本)", 0xFF4FC3F7.toInt())
        drawExhibitGate(canvas, width * 0.5f, height * 0.45f, "昆虫馆\n(昆虫标本)", 0xFF81C784.toInt())
        drawExhibitGate(canvas, width * 0.77f, height * 0.45f, "化石馆\n(骨骼模型)", 0xFFFFD54F.toInt())

        // Curator Owl Blathers (傅达) in the center
        drawBlathersOwl(canvas, width * 0.5f, height * 0.72f)

        // Draw Player Character inside museum foyer
        val px = width * 0.35f + vm.playerX * width * 0.3f
        val py = height * 0.65f + vm.playerY * height * 0.28f
        drawPlayerCharacter(canvas, px, py, vm)
    }

    private fun drawForest(canvas: Canvas, width: Int, height: Int, vm: GameViewModel, scrollX: Float, season: Season, phase: DayPhase, weather: Weather) {
        val worldWidth = width * 2f
        val groundY = height * 0.32f

        // Sky
        skyPaint.shader = LinearGradient(0f, 0f, 0f, groundY, 0xFFE8F5E9.toInt(), 0xFFC8E6C9.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), groundY, skyPaint)

        // Forest floor green watercolor
        val forestColor = when (season) {
            Season.SPRING -> 0xFF66BB6A.toInt()
            Season.SUMMER -> 0xFF2E7D32.toInt()
            Season.AUTUMN -> 0xFF8D6E63.toInt()
            Season.WINTER -> 0xFFCFD8DC.toInt()
        }
        groundPaint.color = forestColor
        canvas.drawRect(0f, groundY, width.toFloat(), height.toFloat(), groundPaint)

        canvas.save()
        canvas.translate(-scrollX, 0f)

        // Draw winding creek stream (river in forest)
        waterPaint.color = 0xFF40C4FF.toInt()
        val path = Path().apply {
            moveTo(worldWidth * 0.4f, groundY)
            quadTo(worldWidth * 0.3f, height * 0.6f, worldWidth * 0.5f, height.toFloat())
            lineTo(worldWidth * 0.58f, height.toFloat())
            quadTo(worldWidth * 0.38f, height * 0.6f, worldWidth * 0.48f, groundY)
            close()
        }
        canvas.drawPath(path, waterPaint)

        // Sparkle of water
        waterPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawCircle(worldWidth * 0.38f, height * 0.55f, 5f, waterPaint)
        canvas.drawCircle(worldWidth * 0.44f, height * 0.72f, 7f, waterPaint)

        // Draw lots of trees in the background
        var tx = 50f
        while (tx < worldWidth) {
            if (tx < worldWidth * 0.35f || tx > worldWidth * 0.6f) {
                drawForestTree(canvas, tx, groundY + 80f + (tx % 130), season)
            }
            tx += 110f
        }

        // Draw star cracks inside Forest
        for (fossil in vm.playerData.fossilSpawns) {
            // translate fossil town coordinate slightly to make forest cracks playable too
            drawFossilCrack(canvas, fossil.first * worldWidth, groundY + 180f + fossil.second * (height - groundY - 260f))
        }

        // Draw Bugs in forest
        for (bug in vm.activeBugs) {
            drawActiveBug(canvas, bug.x * worldWidth, groundY + 120f + bug.y * (height - groundY - 240f), bug)
        }
        updateBugsMovement(vm, worldWidth, height, groundY)

        // Draw Player character in Forest
        val px = vm.playerX * worldWidth
        val py = groundY + 150f + vm.playerY * (height - groundY - 260f)
        drawPlayerCharacter(canvas, px, py, vm)

        canvas.restore()
    }

    private fun drawBattleScene(canvas: Canvas, width: Int, height: Int, vm: GameViewModel) {
        // Deep magical forest battle background
        val grad = LinearGradient(0f, 0f, 0f, height.toFloat(), 0xFF121C16.toInt(), 0xFF0D2519.toInt(), Shader.TileMode.CLAMP)
        groundPaint.shader = grad
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), groundPaint)
        groundPaint.shader = null

        // Draw mystical glowing particle fireflies in background
        val r = Random(12345)
        groundPaint.color = 0x80D4E157.toInt()
        for (i in 0 until 12) {
            val px = r.nextFloat() * width
            val py = r.nextFloat() * height * 0.5f
            val wobble = sin((System.currentTimeMillis() / 600.0) + i).toFloat() * 10f
            canvas.drawCircle(px + wobble, py, 6f + r.nextFloat() * 4f, groundPaint)
        }

        // 1. Draw Player's pet pup "Abby" (阿奇) on the left (around bottom left)
        val petX = width * 0.25f
        val petY = height * 0.42f
        drawCombatPet(canvas, petX, petY, vm)

        // HP bar for pet
        drawHealthBar(canvas, petX, petY - 140f, vm.combatPetHp.value, vm.combatPetMaxHp.value, vm.combatPetShield.value, "阿奇")

        // 2. Draw Enemy Monster on the right (around upper right)
        val monster = vm.combatMonster.value
        if (monster != null) {
            val monX = width * 0.72f
            val monY = height * 0.32f
            drawCombatMonster(canvas, monX, monY, monster)
            
            // HP bar for monster
            drawHealthBar(canvas, monX, monY - 120f, monster.hp, monster.maxHp, 0, monster.name)
        }
    }

    private fun drawCombatPet(canvas: Canvas, cx: Float, cy: Float, vm: GameViewModel) {
        // Rounded cartoon pup drawing
        val breath = sin(System.currentTimeMillis() / 250.0).toFloat() * 4f
        
        // Shadow
        characterPaint.color = 0x33000000
        canvas.drawOval(cx - 50f, cy + 30f, cx + 50f, cy + 45f, characterPaint)
        
        // Body (Light orange/brown)
        characterPaint.color = 0xFFFFB74D.toInt()
        canvas.drawRoundRect(cx - 45f, cy - 20f + breath, cx + 45f, cy + 35f, 25f, 25f, characterPaint)
        
        // Belly white accent
        characterPaint.color = Color.WHITE
        canvas.drawCircle(cx, cy + 10f + breath, 22f, characterPaint)

        // Head
        characterPaint.color = 0xFFFFB74D.toInt()
        canvas.drawCircle(cx, cy - 45f + breath, 38f, characterPaint)
        
        // Snout
        characterPaint.color = Color.WHITE
        canvas.drawOval(cx - 15f, cy - 40f + breath, cx + 15f, cy - 20f + breath, characterPaint)
        characterPaint.color = Color.BLACK
        canvas.drawCircle(cx, cy - 35f + breath, 5f, characterPaint) // Nose
        
        // Cute floppy ears
        characterPaint.color = 0xFFD84315.toInt() // dark orange brown
        canvas.drawRoundRect(cx - 48f, cy - 75f + breath, cx - 28f, cy - 30f + breath, 15f, 15f, characterPaint)
        canvas.drawRoundRect(cx + 28f, cy - 75f + breath, cx + 48f, cy - 30f + breath, 15f, 15f, characterPaint)

        // Beady Eyes
        characterPaint.color = Color.BLACK
        canvas.drawCircle(cx - 14f, cy - 48f + breath, 4.5f, characterPaint)
        canvas.drawCircle(cx + 14f, cy - 48f + breath, 4.5f, characterPaint)
        
        // Tail
        characterPaint.color = 0xFFD84315.toInt()
        val tailWobble = sin(System.currentTimeMillis() / 150.0).toFloat() * 15f
        canvas.drawRoundRect(cx - 60f, cy - 10f + breath + tailWobble, cx - 40f, cy + 10f + breath + tailWobble, 10f, 10f, characterPaint)

        // Puppy collar / bandana
        characterPaint.color = 0xFFE53935.toInt() // Red collar
        canvas.drawRect(cx - 30f, cy - 18f + breath, cx + 30f, cy - 10f + breath, characterPaint)
    }

    private fun drawCombatMonster(canvas: Canvas, mx: Float, my: Float, monster: CombatMonster) {
        val wobble = sin(System.currentTimeMillis() / 200.0).toFloat() * 6f
        
        // Shadow
        characterPaint.color = 0x33000000
        canvas.drawOval(mx - 60f, my + 40f, mx + 60f, my + 55f, characterPaint)

        characterPaint.color = monster.color
        when (monster.id) {
            "m_slime" -> {
                // Jello slime draw
                val sHeight = 70f + wobble
                canvas.drawRoundRect(mx - 55f, my + 35f - sHeight, mx + 55f, my + 35f, 40f, 40f, characterPaint)
                // Cute eyes
                characterPaint.color = Color.BLACK
                canvas.drawCircle(mx - 15f, my - 15f + wobble, 4f, characterPaint)
                canvas.drawCircle(mx + 15f, my - 15f + wobble, 4f, characterPaint)
                // Cheeks
                characterPaint.color = 0x50FF3D00
                canvas.drawCircle(mx - 22f, my - 10f + wobble, 6f, characterPaint)
                canvas.drawCircle(mx + 22f, my - 10f + wobble, 6f, characterPaint)
            }
            "m_beetle" -> {
                // Giant heavy beetle
                canvas.drawRoundRect(mx - 65f, my - 45f + wobble, mx + 65f, my + 35f + wobble, 20f, 20f, characterPaint)
                // Horn
                characterPaint.color = 0xFF3E2723.toInt()
                canvas.drawRect(mx - 8f, my - 85f + wobble, mx + 8f, my - 40f + wobble, characterPaint)
                canvas.drawRect(mx - 25f, my - 85f + wobble, mx + 25f, my - 75f + wobble, characterPaint)
                
                // Red angry bug eyes
                characterPaint.color = Color.RED
                canvas.drawCircle(mx - 25f, my - 20f + wobble, 6f, characterPaint)
                canvas.drawCircle(mx + 25f, my - 20f + wobble, 6f, characterPaint)
            }
            else -> {
                // Treant tree monster
                // Trunk
                canvas.drawRect(mx - 40f, my - 70f + wobble, mx + 40f, my + 35f + wobble, characterPaint)
                // Foliage head
                leafPaint.color = 0xFF2E7D32.toInt()
                canvas.drawCircle(mx, my - 80f + wobble, 65f, leafPaint)
                canvas.drawCircle(mx - 40f, my - 70f + wobble, 45f, leafPaint)
                canvas.drawCircle(mx + 40f, my - 70f + wobble, 45f, leafPaint)
                
                // Yellow glowing eyes inside bark
                characterPaint.color = Color.YELLOW
                canvas.drawCircle(mx - 15f, my - 25f + wobble, 5f, characterPaint)
                canvas.drawCircle(mx + 15f, my - 25f + wobble, 5f, characterPaint)
            }
        }
    }

    private fun drawHealthBar(canvas: Canvas, x: Float, y: Float, hp: Int, maxHp: Int, shield: Int, name: String) {
        val barWidth = 240f
        val barHeight = 16f
        val pct = (hp.toFloat() / maxHp).coerceIn(0f, 1f)

        // Draw Name
        textPaint.color = Color.WHITE
        textPaint.textSize = 28f
        canvas.drawText(name, x - barWidth/2f, y - 14f, textPaint)

        // Draw BG bar (Dark transparent grey)
        uiPaint.color = 0x66000000
        canvas.drawRoundRect(x - barWidth/2f, y, x + barWidth/2f, y + barHeight, 5f, 5f, uiPaint)

        // Draw HP filling (Green/Yellow/Red depending on health)
        uiPaint.color = when {
            pct > 0.5f -> 0xFF4CAF50.toInt()
            pct > 0.2f -> 0xFFFFC107.toInt()
            else -> 0xFFF44336.toInt()
        }
        canvas.drawRoundRect(x - barWidth/2f, y, x - barWidth/2f + barWidth * pct, y + barHeight, 5f, 5f, uiPaint)

        // Draw text fraction
        textPaint.textSize = 20f
        canvas.drawText("$hp / $maxHp" + (if (shield > 0) " (🛡️$shield)" else ""), x - 40f, y + 40f, textPaint)
    }

    // Drawing basic elements: Houses
    private fun drawCuteHouse(canvas: Canvas, x: Float, y: Float, label: String, wallCol: Int, roofCol: Int, level: Int) {
        val width = 160f
        val height = 120f
        
        // Base Wall
        housePaint.color = wallCol
        canvas.drawRoundRect(x, y, x + width, y + height, 15f, 15f, housePaint)

        // Roof triangle shape
        roofPaint.color = roofCol
        val path = Path().apply {
            moveTo(x - 15f, y + 5f)
            lineTo(x + width/2f, y - 55f)
            lineTo(x + width + 15f, y + 5f)
            close()
        }
        canvas.drawPath(path, roofPaint)

        // Door
        woodPaint.color = 0xFF5D4037.toInt()
        canvas.drawRect(x + width/2f - 20f, y + height - 50f, x + width/2f + 20f, y + height, woodPaint)
        // Door knob
        roofPaint.color = 0xFFFFC107.toInt()
        canvas.drawCircle(x + width/2f - 12f, y + height - 25f, 4f, roofPaint)

        // Window
        housePaint.color = 0xFFE1F5FE.toInt()
        canvas.drawRoundRect(x + 20f, y + 25f, x + 55f, y + 60f, 5f, 5f, housePaint)
        woodPaint.color = wallCol
        canvas.drawLine(x + 37.5f, y + 25f, x + 37.5f, y + 60f, woodPaint)
        canvas.drawLine(x + 20f, y + 42.5f, x + 55f, y + 42.5f, woodPaint)

        // Chimney (If expanded)
        if (level > 0) {
            woodPaint.color = 0xFF7D5260.toInt()
            canvas.drawRect(x + width - 35f, y - 45f, x + width - 15f, y - 10f, woodPaint)
            // Smoke puff
            woodPaint.color = 0x66FFFFFF.toInt()
            val wobble = sin(System.currentTimeMillis() / 400.0).toFloat() * 10f
            canvas.drawCircle(x + width - 25f + wobble, y - 65f, 15f, woodPaint)
        }

        // Tag label
        textPaint.color = Color.BLACK
        textPaint.textSize = 24f
        canvas.drawText(label, x + width/2f - textPaint.measureText(label)/2f, y + height + 28f, textPaint)
    }

    private fun drawMuseumFacade(canvas: Canvas, x: Float, y: Float) {
        val width = 200f
        val height = 135f

        // Roman Museum pillars & facade
        groundPaint.color = 0xFFECEFF1.toInt() // marble white
        canvas.drawRect(x, y, x + width, y + height, groundPaint)

        // Pediment Triangle
        roofPaint.color = 0xFFB0BEC5.toInt()
        val path = Path().apply {
            moveTo(x - 10f, y)
            lineTo(x + width/2f, y - 45f)
            lineTo(x + width + 10f, y)
            close()
        }
        canvas.drawPath(path, roofPaint)

        // 4 pillars
        groundPaint.color = 0xFFCFD8DC.toInt()
        canvas.drawRect(x + 15f, y + 20f, x + 30f, y + height, groundPaint)
        canvas.drawRect(x + 65f, y + 20f, x + 80f, y + height, groundPaint)
        canvas.drawRect(x + 120f, y + 135f, x + 135f, y + height, groundPaint) // offset fix
        canvas.drawRect(x + 120f, y + 20f, x + 135f, y + height, groundPaint)
        canvas.drawRect(x + 170f, y + 20f, x + 185f, y + height, groundPaint)

        // Big Double wooden gate
        woodPaint.color = 0xFF4E342E.toInt()
        canvas.drawRoundRect(x + width/2f - 30f, y + height - 70f, x + width/2f + 30f, y + height, 8f, 8f, woodPaint)
        
        // Label
        textPaint.color = Color.BLACK
        textPaint.textSize = 24f
        canvas.drawText("博物馆", x + width/2f - textPaint.measureText("博物馆")/2f, y + height + 28f, textPaint)
    }

    private fun drawExhibitGate(canvas: Canvas, cx: Float, cy: Float, label: String, bannerCol: Int) {
        val width = 120f
        val height = 170f
        
        // Doorway arch
        woodPaint.color = 0xFF5D4037.toInt()
        canvas.drawRoundRect(cx - width/2f, cy - height/2f, cx + width/2f, cy + height/2f, 20f, 20f, woodPaint)
        
        // Inner portal
        groundPaint.color = 0xFF121212.toInt() // dark exit
        canvas.drawRoundRect(cx - width/2f + 10f, cy - height/2f + 10f, cx + width/2f - 10f, cy + height/2f, 15f, 15f, groundPaint)

        // Banner header
        uiPaint.color = bannerCol
        canvas.drawRect(cx - width/2f + 12f, cy - height/2f + 25f, cx + width/2f - 12f, cy - height/2f + 55f, uiPaint)

        // Label text split
        textPaint.textSize = 20f
        textPaint.color = Color.WHITE
        val lines = label.split("\n")
        var ly = cy - 20f
        for (line in lines) {
            canvas.drawText(line, cx - textPaint.measureText(line)/2f, ly, textPaint)
            ly += 30f
        }
    }

    private fun drawBlathersOwl(canvas: Canvas, cx: Float, cy: Float) {
        // Blathers the owl (cozy illustration)
        val breath = sin(System.currentTimeMillis() / 350.0).toFloat() * 3f
        
        // Body
        characterPaint.color = 0xFF5D4037.toInt() // Dark brown
        canvas.drawOval(cx - 35f, cy - 25f + breath, cx + 35f, cy + 45f + breath, characterPaint)
        
        // Chest yellow argyle sweater vest style
        characterPaint.color = 0xFFFFF9C4.toInt()
        canvas.drawOval(cx - 22f, cy - 10f + breath, cx + 22f, cy + 35f + breath, characterPaint)
        
        // Red bow tie
        characterPaint.color = 0xFFD32F2F.toInt()
        canvas.drawCircle(cx, cy - 12f + breath, 7f, characterPaint)
        val ribbon = Path().apply {
            moveTo(cx - 15f, cy - 18f + breath)
            lineTo(cx + 15f, cy - 6f + breath)
            lineTo(cx + 15f, cy - 18f + breath)
            lineTo(cx - 15f, cy - 6f + breath)
            close()
        }
        canvas.drawPath(ribbon, characterPaint)

        // Head
        characterPaint.color = 0xFF5D4037.toInt()
        canvas.drawCircle(cx, cy - 45f + breath, 34f, characterPaint)

        // Huge white eyes with green rims
        characterPaint.color = 0xFF2E7D32.toInt() // Green rim
        canvas.drawCircle(cx - 15f, cy - 48f + breath, 16f, characterPaint)
        canvas.drawCircle(cx + 15f, cy - 48f + breath, 16f, characterPaint)
        
        characterPaint.color = Color.WHITE
        canvas.drawCircle(cx - 15f, cy - 48f + breath, 12f, characterPaint)
        canvas.drawCircle(cx + 15f, cy - 48f + breath, 12f, characterPaint)
        
        characterPaint.color = Color.BLACK
        canvas.drawCircle(cx - 12f, cy - 46f + breath, 6f, characterPaint)
        canvas.drawCircle(cx + 12f, cy - 46f + breath, 6f, characterPaint)

        // Orange Beak
        characterPaint.color = 0xFFFF9800.toInt()
        val beak = Path().apply {
            moveTo(cx - 8f, cy - 38f + breath)
            lineTo(cx, cy - 22f + breath)
            lineTo(cx + 8f, cy - 38f + breath)
            close()
        }
        canvas.drawPath(beak, characterPaint)

        // Feet
        canvas.drawRect(cx - 20f, cy + 42f + breath, cx - 10f, cy + 50f + breath, characterPaint)
        canvas.drawRect(cx + 10f, cy + 42f + breath, cx + 20f, cy + 50f + breath, characterPaint)

        // Sleeping symbol ( Owls sleep during the day!)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 6..18) {
            textPaint.textSize = 28f
            textPaint.color = 0xFF03A9F4.toInt()
            val sleepWobble = sin(System.currentTimeMillis() / 300.0).toFloat() * 5f
            canvas.drawText("Zzz..", cx + 40f + sleepWobble, cy - 70f, textPaint)
        }
    }

    private fun drawTreesInTown(canvas: Canvas, worldWidth: Float, groundY: Float, height: Int, season: Season) {
        // Place a few dynamic vector trees
        val positions = listOf(0.05f, 0.2f, 0.3f, 0.45f, 0.6f, 0.72f, 0.9f)
        for ((idx, pct) in positions.withIndex()) {
            val tx = pct * worldWidth
            val ty = groundY + 110f + (idx * 30) % 80
            drawForestTree(canvas, tx, ty, season)
        }
    }

    private fun drawForestTree(canvas: Canvas, x: Float, y: Float, season: Season) {
        // Trunk
        woodPaint.color = 0xFF6D4C41.toInt()
        canvas.drawRect(x - 14f, y - 20f, x + 14f, y + 60f, woodPaint)

        // Foliage based on season
        val (fColor, fColorDark) = when (season) {
            Season.SPRING -> Pair(0xFFF48FB1.toInt(), 0xFFF06292.toInt()) // Sakura pink
            Season.SUMMER -> Pair(0xFF43A047.toInt(), 0xFF2E7D32.toInt()) // Vibrant dark green
            Season.AUTUMN -> Pair(0xFFFFB300.toInt(), 0xFFE65100.toInt()) // Red/Orange autumn leaves
            Season.WINTER -> Pair(0xFFECEFF1.toInt(), 0xFFCFD8DC.toInt()) // Snow capped pine
        }

        leafPaint.color = fColorDark
        canvas.drawCircle(x, y - 40f, 50f, leafPaint)
        canvas.drawCircle(x - 30f, y - 25f, 40f, leafPaint)
        canvas.drawCircle(x + 30f, y - 25f, 40f, leafPaint)
        
        leafPaint.color = fColor
        canvas.drawCircle(x, y - 48f, 44f, leafPaint)
        canvas.drawCircle(x - 26f, y - 30f, 34f, leafPaint)
        canvas.drawCircle(x + 26f, y - 30f, 34f, leafPaint)

        // Apples (Fruit tree representation on some trees!)
        if (season != Season.WINTER && (x.toInt() % 3 == 0)) {
            leafPaint.color = 0xFFD32F2F.toInt() // Red apples
            canvas.drawCircle(x - 15f, y - 35f, 10f, leafPaint)
            canvas.drawCircle(x + 20f, y - 45f, 10f, leafPaint)
            canvas.drawCircle(x, y - 10f, 10f, leafPaint)
        }
    }

    private fun drawFlowerSpec(canvas: Canvas, x: Float, y: Float, flower: PlacedFlower) {
        val col = when (flower.color) {
            "Red" -> 0xFFE53935.toInt()
            "White" -> Color.WHITE
            "Yellow" -> 0xFFFFEB3B.toInt()
            "Pink" -> 0xFFF48FB1.toInt()
            "Orange" -> 0xFFFF9800.toInt()
            "Purple" -> 0xFF9C27B0.toInt()
            "Blue" -> 0xFF2196F3.toInt()
            "Black" -> 0xFF212121.toInt()
            "Gold" -> 0xFFFFD54F.toInt()
            else -> Color.RED
        }

        // Stem
        woodPaint.color = 0xFF4CAF50.toInt()
        canvas.drawRect(x - 3f, y - 15f, x + 3f, y + 25f, woodPaint)
        canvas.drawRect(x - 12f, y - 2f, x + 12f, y + 3f, woodPaint) // leaves

        // Growth stage rendering
        when (flower.growthStage) {
            0, 1 -> { // Seed / Sprout
                woodPaint.color = 0xFF81C784.toInt()
                canvas.drawCircle(x, y + 5f, 6f, woodPaint)
            }
            2 -> { // Bud
                uiPaint.color = col
                canvas.drawOval(x - 10f, y - 20f, x + 10f, y - 8f, uiPaint)
            }
            3 -> { // Bloom
                uiPaint.color = col
                // Petals
                canvas.drawCircle(x - 12f, y - 20f, 10f, uiPaint)
                canvas.drawCircle(x + 12f, y - 20f, 10f, uiPaint)
                canvas.drawCircle(x, y - 32f, 10f, uiPaint)
                canvas.drawCircle(x, y - 10f, 10f, uiPaint)
                // Center bud
                uiPaint.color = 0xFFFFD54F.toInt()
                canvas.drawCircle(x, y - 20f, 8f, uiPaint)
            }
        }

        // Sparkling water indicators
        if (flower.watered) {
            val wobble = sin(System.currentTimeMillis() / 150.0).toFloat() * 4f
            uiPaint.color = 0xAA00E5FF.toInt()
            canvas.drawCircle(x - 15f, y - 30f + wobble, 4f, uiPaint)
            canvas.drawCircle(x + 18f, y - 22f - wobble, 3f, uiPaint)
        }
    }

    private fun drawFossilCrack(canvas: Canvas, x: Float, y: Float) {
        // Draw Star fossil mark
        val path = Path()
        val numPoints = 5
        val outerRadius = 18f
        val innerRadius = 8f
        var angle = -Math.PI / 2
        val angleIncrement = Math.PI / numPoints

        path.moveTo((x + Math.cos(angle) * outerRadius).toFloat(), (y + Math.sin(angle) * outerRadius).toFloat())
        for (i in 0 until numPoints * 2) {
            angle += angleIncrement
            val r = if (i % 2 == 0) innerRadius else outerRadius
            path.lineTo((x + Math.cos(angle) * r).toFloat(), (y + Math.sin(angle) * r).toFloat())
        }
        path.close()

        uiPaint.color = 0xFF3E2723.toInt() // dark brown fossil hole crack
        canvas.drawPath(path, uiPaint)
        
        // Inner cracks
        uiPaint.color = 0xFF5D4037.toInt()
        canvas.drawLine(x, y - 10f, x, y + 10f, uiPaint)
        canvas.drawLine(x - 10f, x, x + 10f, y, uiPaint)
    }

    private fun drawPlacedFurnitureIcon(canvas: Canvas, x: Float, y: Float, itemId: String, rotation: Float) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)

        // Draw basic shapes to stand in for beautifully themed furniture items
        val item = PlayerData.ITEMS[itemId]
        val theme = item?.extra ?: "wood"
        
        val fCol = when (theme) {
            "pastoral" -> 0xFFD7CCC8.toInt() // Pastel wicker
            "wood" -> 0xFF8D6E63.toInt() // Brown wood
            "fruit" -> 0xFFFF7043.toInt() // Juicy orange / apple red
            "star" -> 0xFF7E57C2.toInt() // Nebula purple
            "ocean" -> 0xFF29B6F6.toInt() // Lagoon turquoise
            else -> 0xFFB0BEC5.toInt()
        }

        uiPaint.color = fCol
        when {
            itemId.contains("chair") || itemId.contains("stool") -> {
                // Chair shape
                canvas.drawRoundRect(-25f, -25f, 25f, 25f, 5f, 5f, uiPaint)
                // Backrest
                uiPaint.color = 0x88000000.toInt()
                canvas.drawRect(-25f, -25f, 25f, -15f, uiPaint)
            }
            itemId.contains("table") || itemId.contains("desk") -> {
                // Table
                canvas.drawCircle(0f, 0f, 32f, uiPaint)
                // Highlights
                uiPaint.color = 0x44FFFFFF.toInt()
                canvas.drawCircle(-8f, -8f, 10f, uiPaint)
            }
            itemId.contains("bed") -> {
                // Big cozy bed
                canvas.drawRoundRect(-30f, -45f, 30f, 45f, 10f, 10f, uiPaint)
                // Pillow
                uiPaint.color = Color.WHITE
                canvas.drawRoundRect(-24f, -40f, 24f, -20f, 5f, 5f, uiPaint)
                // Blanket fold
                uiPaint.color = 0x44000000
                canvas.drawRect(-30f, -10f, 30f, 45f, uiPaint)
            }
            itemId.contains("tv") -> {
                // TV set
                canvas.drawRoundRect(-35f, -25f, 35f, 25f, 15f, 15f, uiPaint) // Apple shaped frame
                uiPaint.color = Color.BLACK // Screen
                canvas.drawRect(-24f, -16f, 24f, 16f, uiPaint)
            }
            itemId.contains("lamp") -> {
                // Glow lamp
                val wobble = sin(System.currentTimeMillis() / 200.0).toFloat() * 6f
                uiPaint.color = 0xFFFFF176.toInt() // yellow glow
                canvas.drawCircle(0f, 0f, 18f + wobble, uiPaint)
                uiPaint.color = fCol
                canvas.drawRect(-10f, 10f, 10f, 22f, uiPaint)
            }
            else -> {
                // Wardrobe or generic chest
                canvas.drawRect(-28f, -38f, 28f, 38f, uiPaint)
                woodPaint.color = 0xFF3E2723.toInt()
                canvas.drawLine(-28f, 0f, 28f, 0f, woodPaint) // Split drawers
                canvas.drawCircle(-8f, -10f, 3f, woodPaint) // Handles
                canvas.drawCircle(-8f, 15f, 3f, woodPaint)
            }
        }

        canvas.restore()
    }

    private fun drawActiveBug(canvas: Canvas, x: Float, y: Float, bug: GameViewModel.ActiveBug) {
        val wingWobble = sin(System.currentTimeMillis() / 50.0).toFloat() * 12f
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(bug.angle)

        // Draw cute simple bug
        val bodyCol = when (bug.spec.rarity) {
            "Legendary" -> 0xFFFFD54F.toInt() // Golden Stag
            "Rare" -> 0xFF81C784.toInt() // Jade green butterfly
            else -> 0xFFE57373.toInt() // Ladybug red
        }

        // Draw body
        uiPaint.color = bodyCol
        canvas.drawOval(-12f, -6f, 12f, 6f, uiPaint)

        // Wings flapping
        uiPaint.color = 0x99FFFFFF.toInt()
        canvas.drawOval(-8f, -18f - wingWobble, 8f, -6f, uiPaint)
        canvas.drawOval(-8f, 6f, 8f, 18f + wingWobble, uiPaint)

        // Beady eyes
        uiPaint.color = Color.BLACK
        canvas.drawCircle(10f, -3f, 2.5f, uiPaint)
        canvas.drawCircle(10f, 3f, 2.5f, uiPaint)

        canvas.restore()
    }

    private fun drawPlayerCharacter(canvas: Canvas, cx: Float, cy: Float, vm: GameViewModel) {
        val walkCycle = sin((System.currentTimeMillis() / 150.0)).toFloat() * 8f
        // Only bob/bounce when actively moving
        val isMoving = Math.hypot((vm.playerX - vm.targetPlayerX).toDouble(), (vm.playerY - vm.targetPlayerY).toDouble()) > 0.02
        val bounce = if (isMoving) walkCycle else sin((System.currentTimeMillis() / 400.0)).toFloat() * 2f

        // Shadow
        characterPaint.color = 0x33000000
        canvas.drawOval(cx - 35f, cy + 32f, cx + 35f, cy + 42f, characterPaint)

        // Legs/Feet
        characterPaint.color = 0xFF5D4037.toInt()
        if (isMoving) {
            canvas.drawCircle(cx - 15f, cy + 32f + bounce, 10f, characterPaint)
            canvas.drawCircle(cx + 15f, cy + 32f - bounce, 10f, characterPaint)
        } else {
            canvas.drawCircle(cx - 12f, cy + 32f, 10f, characterPaint)
            canvas.drawCircle(cx + 12f, cy + 32f, 10f, characterPaint)
        }

        // Body (Shirt/Clothing based on ID)
        val clothCol = when (vm.currentClothing.value) {
            "cloth_straw_hat" -> 0xFFFFD54F.toInt()
            "cloth_red_tee" -> 0xFFE53935.toInt()
            "cloth_sweater" -> 0xFFFFB74D.toInt()
            "cloth_starry_robe" -> 0xFF5E35B1.toInt()
            "cloth_crown" -> 0xFFFFD54F.toInt()
            else -> 0xFFE53935.toInt()
        }
        characterPaint.color = clothCol
        canvas.drawRoundRect(cx - 28f, cy - 10f + bounce, cx + 28f, cy + 30f + bounce, 12f, 12f, characterPaint)

        // Arms
        characterPaint.color = 0xFFFFD54F.toInt() // Skin tone peach
        canvas.drawCircle(cx - 34f, cy + 5f + bounce, 8f, characterPaint)
        canvas.drawCircle(cx + 34f, cy + 5f + bounce, 8f, characterPaint)

        // Head (Peach skin)
        canvas.drawCircle(cx, cy - 30f + bounce, 28f, characterPaint)

        // Face features
        // Big round anime eyes
        characterPaint.color = Color.BLACK
        canvas.drawCircle(cx - 10f, cy - 30f + bounce, 5f, characterPaint)
        canvas.drawCircle(cx + 10f, cy - 30f + bounce, 5f, characterPaint)
        
        // Eye shines
        characterPaint.color = Color.WHITE
        canvas.drawCircle(cx - 12f, cy - 32f + bounce, 2f, characterPaint)
        canvas.drawCircle(cx + 8f, cy - 32f + bounce, 2f, characterPaint)

        // Blush cheeks
        characterPaint.color = 0x66FF8A80.toInt()
        canvas.drawCircle(cx - 16f, cy - 22f + bounce, 6f, characterPaint)
        canvas.drawCircle(cx + 16f, cy - 22f + bounce, 6f, characterPaint)

        // Smile
        characterPaint.color = Color.BLACK
        characterPaint.style = Paint.Style.STROKE
        characterPaint.strokeWidth = 3f
        canvas.drawArc(cx - 6f, cy - 25f + bounce, cx + 6f, cy - 15f + bounce, 0f, 180f, false, characterPaint)
        characterPaint.style = Paint.Style.FILL

        // Hair (Hairstyle & color)
        characterPaint.color = vm.playerData.currentHairColor
        when (vm.currentHairstyle.value) {
            0 -> { // Short boy cut / default
                canvas.drawArc(cx - 32f, cy - 62f + bounce, cx + 32f, cy - 26f + bounce, 180f, 180f, true, characterPaint)
                // Fringe/bangs
                val bangs = Path().apply {
                    moveTo(cx - 28f, cy - 44f + bounce)
                    lineTo(cx - 15f, cy - 36f + bounce)
                    lineTo(cx, cy - 44f + bounce)
                    lineTo(cx + 15f, cy - 36f + bounce)
                    lineTo(cx + 28f, cy - 44f + bounce)
                    close()
                }
                canvas.drawPath(bangs, characterPaint)
            }
            1 -> { // Pigtails / Ponytails
                canvas.drawArc(cx - 32f, cy - 62f + bounce, cx + 32f, cy - 26f + bounce, 180f, 180f, true, characterPaint)
                // Left pigtail
                canvas.drawCircle(cx - 36f, cy - 45f + bounce, 14f, characterPaint)
                // Right pigtail
                canvas.drawCircle(cx + 36f, cy - 45f + bounce, 14f, characterPaint)
            }
            else -> { // Long elegant hair
                canvas.drawArc(cx - 32f, cy - 62f + bounce, cx + 32f, cy - 26f + bounce, 180f, 180f, true, characterPaint)
                // Hair tails down sides
                canvas.drawRect(cx - 32f, cy - 35f + bounce, cx - 24f, cy + 5f + bounce, characterPaint)
                canvas.drawRect(cx + 24f, cy - 35f + bounce, cx + 32f, cy + 5f + bounce, characterPaint)
            }
        }

        // Wear hats
        if (vm.currentClothing.value == "cloth_straw_hat") {
            // Straw hat straw color
            uiPaint.color = 0xFFFFD54F.toInt()
            canvas.drawOval(cx - 45f, cy - 65f + bounce, cx + 45f, cy - 45f + bounce, uiPaint) // brim
            canvas.drawRoundRect(cx - 26f, cy - 82f + bounce, cx + 26f, cy - 56f + bounce, 10f, 10f, uiPaint) // crown
            // Red band
            uiPaint.color = Color.RED
            canvas.drawRect(cx - 26f, cy - 62f + bounce, cx + 26f, cy - 56f + bounce, uiPaint)
        } else if (vm.currentClothing.value == "cloth_crown") {
            // Gold crown
            uiPaint.color = 0xFFFBC02D.toInt()
            val cPath = Path().apply {
                moveTo(cx - 25f, cy - 55f + bounce)
                lineTo(cx - 25f, cy - 75f + bounce)
                lineTo(cx - 12f, cy - 62f + bounce)
                lineTo(cx, cy - 82f + bounce)
                lineTo(cx + 12f, cy - 62f + bounce)
                lineTo(cx + 25f, cy - 75f + bounce)
                lineTo(cx + 25f, cy - 55f + bounce)
                close()
            }
            canvas.drawPath(cPath, uiPaint)
            // Blue gem
            uiPaint.color = Color.BLUE
            canvas.drawCircle(cx, cy - 64f + bounce, 4.5f, uiPaint)
        }
    }

    private fun drawAnimalVillager(canvas: Canvas, vx: Float, vy: Float, v: Villager) {
        val breath = sin(System.currentTimeMillis() / 280.0).toFloat() * 3f
        
        // Shadow
        characterPaint.color = 0x33000000
        canvas.drawOval(vx - 32f, vy + 28f, vx + 32f, vy + 38f, characterPaint)

        // Body (Simple robes)
        characterPaint.color = v.houseColor
        canvas.drawRoundRect(vx - 24f, vy - 10f + breath, vx + 24f, vy + 28f + breath, 10f, 10f, characterPaint)

        // Feet
        characterPaint.color = 0xFF5D4037.toInt()
        canvas.drawCircle(vx - 10f, vy + 29f + breath, 7f, characterPaint)
        canvas.drawCircle(vx + 10f, vy + 29f + breath, 7f, characterPaint)

        // Head (Based on Animal Type)
        characterPaint.color = v.houseColor
        canvas.drawCircle(vx, vy - 26f + breath, 24f, characterPaint)

        // Draw Custom features (Ears/Snouts)
        characterPaint.color = v.houseColor
        when (v.animalType) {
            "Cat" -> {
                // Pointy cat ears
                val leftEar = Path().apply {
                    moveTo(vx - 22f, vy - 38f + breath)
                    lineTo(vx - 18f, vy - 54f + breath)
                    lineTo(vx - 4f, vy - 42f + breath)
                    close()
                }
                val rightEar = Path().apply {
                    moveTo(vx + 22f, vy - 38f + breath)
                    lineTo(vx + 18f, vy - 54f + breath)
                    lineTo(vx + 4f, vy - 42f + breath)
                    close()
                }
                canvas.drawPath(leftEar, characterPaint)
                canvas.drawPath(rightEar, characterPaint)
            }
            "Dog" -> {
                // Drooping floppy pup ears
                characterPaint.color = 0xFF4E342E.toInt()
                canvas.drawRoundRect(vx - 32f, vy - 42f + breath, vx - 22f, vy - 14f + breath, 8f, 8f, characterPaint)
                canvas.drawRoundRect(vx + 22f, vy - 42f + breath, vx + 32f, vy - 14f + breath, 8f, 8f, characterPaint)
            }
            "Rabbit" -> {
                // Very long rabbit ears
                canvas.drawRoundRect(vx - 18f, vy - 68f + breath, vx - 6f, vy - 36f + breath, 8f, 8f, characterPaint)
                canvas.drawRoundRect(vx + 6f, vy - 68f + breath, vx + 18f, vy - 36f + breath, 8f, 8f, characterPaint)
                // Pink inner ear
                characterPaint.color = 0xFFF8BBD0.toInt()
                canvas.drawRoundRect(vx - 15f, vy - 64f + breath, vx - 9f, vy - 38f + breath, 5f, 5f, characterPaint)
                canvas.drawRoundRect(vx + 9f, vy - 64f + breath, vx + 15f, vy - 38f + breath, 5f, 5f, characterPaint)
            }
            "Bear" -> {
                // Round bear ears
                canvas.drawCircle(vx - 20f, vy - 44f + breath, 10f, characterPaint)
                canvas.drawCircle(vx + 20f, vy - 44f + breath, 10f, characterPaint)
            }
            "Deer" -> {
                // Antlers
                characterPaint.color = 0xFFD7CCC8.toInt()
                canvas.drawRect(vx - 18f, vy - 56f + breath, vx - 14f, vy - 36f + breath, characterPaint)
                canvas.drawRect(vx - 26f, vy - 54f + breath, vx - 14f, vy - 50f + breath, characterPaint)
                canvas.drawRect(vx + 14f, vy - 56f + breath, vx + 18f, vy - 36f + breath, characterPaint)
                canvas.drawRect(vx + 14f, vy - 54f + breath, vx + 26f, vy - 50f + breath, characterPaint)
            }
            else -> { // Penguin
                // White face masking overlay
                characterPaint.color = Color.WHITE
                canvas.drawCircle(vx, vy - 22f + breath, 18f, characterPaint)
                // Yellow beak
                characterPaint.color = 0xFFFFEB3B.toInt()
                canvas.drawOval(vx - 8f, vy - 24f + breath, vx + 8f, vy - 14f + breath, characterPaint)
            }
        }

        // Eyes (Vary based on personality)
        characterPaint.color = Color.BLACK
        if (v.personality == "暴躁") {
            // Angry sharp eyes
            canvas.drawCircle(vx - 8f, vy - 30f + breath, 3.5f, characterPaint)
            canvas.drawCircle(vx + 8f, vy - 30f + breath, 3.5f, characterPaint)
            canvas.drawLine(vx - 14f, vy - 36f + breath, vx - 4f, vy - 32f + breath, characterPaint) // eyebrows
            canvas.drawLine(vx + 14f, vy - 36f + breath, vx + 4f, vy - 32f + breath, characterPaint)
        } else {
            // Happy round eyes
            canvas.drawCircle(vx - 8f, vy - 28f + breath, 4f, characterPaint)
            canvas.drawCircle(vx + 8f, vy - 28f + breath, 4f, characterPaint)
        }

        // Display Tag Label above their head
        textPaint.color = 0xFF3E2723.toInt()
        textPaint.textSize = 20f
        val nick = if (v.nickname.isNotEmpty()) " \"${v.nickname}\"" else ""
        val tag = "${v.name}$nick"
        canvas.drawText(tag, vx - textPaint.measureText(tag)/2f, vy - 72f + breath, textPaint)

        // Action symbol (fish rod if fishing!)
        if (v.currentActivity == VillagerState.FISHING) {
            woodPaint.color = 0xFF8D6E63.toInt()
            canvas.drawLine(vx + 15f, vy + breath, vx + 45f, vy - 15f + breath, woodPaint) // pole
            uiPaint.color = Color.WHITE
            canvas.drawLine(vx + 45f, vy - 15f + breath, vx + 45f, vy + 40f + breath, uiPaint) // string
        }
    }

    private fun drawFishingBobber(canvas: Canvas, bx: Float, by: Float, vm: GameViewModel) {
        val wobble = sin(System.currentTimeMillis() / 220.0).toFloat() * 4f
        val state = vm.fishingState.value
        
        // Floating string from player to bobber
        val px = vm.playerX * (canvas.width * 3f)
        val py = (canvas.height * 0.35f) + 150f + vm.playerY * (canvas.height * 0.65f - 280f)
        
        uiPaint.color = 0xAAFFFFFF.toInt()
        uiPaint.strokeWidth = 2f
        uiPaint.style = Paint.Style.STROKE
        canvas.drawLine(px, py, bx, by, uiPaint)
        uiPaint.style = Paint.Style.FILL

        // Bobber floats differently based on bite vs idle
        val bobberYOffset = if (state == FishingState.BITE) 18f + wobble * 0.5f else wobble

        // Splash circles when biting
        if (state == FishingState.BITE) {
            uiPaint.color = 0x77FFFFFF.toInt()
            val radius = 20f + (System.currentTimeMillis() % 400) * 0.12f
            canvas.drawCircle(bx, by + bobberYOffset, radius, uiPaint)
        }

        // Draw Red/White Fishing Bobber float
        uiPaint.color = Color.RED
        canvas.drawArc(bx - 12f, by - 12f + bobberYOffset, bx + 12f, by + 12f + bobberYOffset, 180f, 180f, true, uiPaint)
        uiPaint.color = Color.WHITE
        canvas.drawArc(bx - 12f, by - 12f + bobberYOffset, bx + 12f, by + 12f + bobberYOffset, 0f, 180f, true, uiPaint)
        
        // Antenna rod
        uiPaint.color = Color.BLACK
        canvas.drawRect(bx - 2f, by - 24f + bobberYOffset, bx + 2f, by - 10f + bobberYOffset, uiPaint)
        uiPaint.color = Color.RED
        canvas.drawCircle(bx, by - 25f + bobberYOffset, 4f, uiPaint)

        // Draw Fish Shadow swim toward bobber
        if (state == FishingState.SHADOW_APPROACHING || state == FishingState.BITE) {
            uiPaint.color = 0x66000000 // Water depth shadow
            canvas.drawOval(
                vm.activeFishShadowX * canvas.width * 3f - 22f,
                by + 10f,
                vm.activeFishShadowX * canvas.width * 3f + 22f,
                by + 28f,
                uiPaint
            )
        }
    }

    private fun drawHills(canvas: Canvas, width: Int, skyHeight: Float, scrollX: Float, season: Season) {
        val hillCol = when (season) {
            Season.SPRING -> 0xFF81C784.toInt()
            Season.SUMMER -> 0xFF388E3C.toInt()
            Season.AUTUMN -> 0xFFF57C00.toInt()
            Season.WINTER -> 0xFFB0BEC5.toInt()
        }
        
        // We draw overlapping curves for watercolor scenery depth
        val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        
        // Hill 1 far back (Slower parallax)
        paint.color = hillCol
        paint.alpha = 80
        val p1 = Path().apply {
            moveTo(0f, skyHeight)
            quadTo(width * 0.35f, skyHeight - 120f, width * 0.7f, skyHeight)
            lineTo(width.toFloat(), skyHeight)
            lineTo(width.toFloat(), skyHeight + 100f)
            lineTo(0f, skyHeight + 100f)
            close()
        }
        canvas.save()
        canvas.translate(-scrollX * 0.15f, 0f)
        canvas.drawPath(p1, paint)
        canvas.restore()

        // Hill 2 closer (Faster parallax)
        paint.alpha = 130
        val p2 = Path().apply {
            moveTo(0f, skyHeight)
            quadTo(width * 0.65f, skyHeight - 80f, width * 1.3f, skyHeight)
            lineTo(width * 1.3f, skyHeight + 100f)
            lineTo(0f, skyHeight + 100f)
            close()
        }
        canvas.save()
        canvas.translate(-scrollX * 0.3f, 0f)
        canvas.drawPath(p2, paint)
        canvas.restore()
    }

    private fun paintStarTwinkle(canvas: Canvas, width: Int, skyHeight: Float, r: Random) {
        uiPaint.color = Color.WHITE
        val now = System.currentTimeMillis()
        for (i in 0 until 30) {
            val sx = r.nextFloat() * width
            val sy = r.nextFloat() * skyHeight * 0.8f
            // Twinkle pulse effect
            val pulse = sin(now / 200.0 + i).toFloat()
            if (pulse > -0.2f) {
                canvas.drawCircle(sx, sy, 2f + pulse * 2f, uiPaint)
            }
        }
    }

    private fun drawTimeAndWeatherOverlays(canvas: Canvas, width: Int, height: Int, phase: DayPhase, weather: Weather) {
        // Overlay for day phases
        uiPaint.shader = null
        uiPaint.style = Paint.Style.FILL
        when (phase) {
            DayPhase.DUSK -> {
                uiPaint.color = 0x22FF5722.toInt() // sunset amber overlay
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), uiPaint)
            }
            DayPhase.NIGHT -> {
                uiPaint.color = 0x26000000 // dark blue tint overlay
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), uiPaint)
            }
            else -> {}
        }

        // Draw Weather Particles
        val now = System.currentTimeMillis()
        val dt = (now - lastParticleTime) / 1000f
        lastParticleTime = now

        if (weather == Weather.RAINY) {
            // Spawn rain lines
            if (weatherParticles.size < 50 && rand.nextInt(5) == 0) {
                weatherParticles.add(Particle(rand.nextFloat() * width, -20f, -50f, 400f + rand.nextFloat() * 100f, 2f + rand.nextFloat() * 3f, 0x884FC3F7.toInt()))
            }
        } else if (weather == Weather.SNOWY) {
            // Spawn snow circles
            if (weatherParticles.size < 40 && rand.nextInt(7) == 0) {
                weatherParticles.add(Particle(rand.nextFloat() * width, -20f, -20f + rand.nextFloat() * 40f, 80f + rand.nextFloat() * 50f, 5f + rand.nextFloat() * 6f, 0xBBFFFFFF.toInt()))
            }
        } else if (TimeManager.getCurrentSeason() == Season.SPRING) {
            // Spring Cherry Petal drift
            if (weatherParticles.size < 20 && rand.nextInt(15) == 0) {
                weatherParticles.add(Particle(rand.nextFloat() * width, -20f, -80f - rand.nextFloat() * 40f, 120f + rand.nextFloat() * 50f, 8f + rand.nextFloat() * 6f, 0x99FF8A80.toInt()))
            }
        }

        // Update & Render particles
        val iterator = weatherParticles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.speedX * dt
            p.y += p.speedY * dt

            // Draw particles
            uiPaint.color = p.color
            if (weather == Weather.RAINY) {
                uiPaint.strokeWidth = p.size
                canvas.drawLine(p.x, p.y, p.x - 6f, p.y + 24f, uiPaint)
            } else {
                canvas.drawCircle(p.x, p.y, p.size, uiPaint)
            }

            // Remove out of bounds
            if (p.y > height + 20f || p.x < -20f || p.x > width + 20f) {
                iterator.remove()
            }
        }
    }

    private fun updateBugsMovement(vm: GameViewModel, worldWidth: Float, height: Int, groundY: Float) {
        val rand = Random()
        val dt = 0.016f // approximate frame step
        for (bug in vm.activeBugs) {
            // Move bug inside logical boundary
            bug.x += bug.speed * sin(bug.angle.toDouble()).toFloat() * dt
            bug.y += bug.speed * Math.cos(bug.angle.toDouble()).toFloat() * dt

            // Clamp and bounce
            if (bug.x < 0.05f || bug.x > 0.95f || bug.y < 0.1f || bug.y > 0.8f) {
                bug.angle = (bug.angle + 180f) % 360f
                bug.x = bug.x.coerceIn(0.06f, 0.94f)
                bug.y = bug.y.coerceIn(0.12f, 0.78f)
            }

            // Randomly shift flying angles
            if (rand.nextInt(40) == 0) {
                bug.angle += -45f + rand.nextFloat() * 90f
            }
        }
    }
}
