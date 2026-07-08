package com.example.game.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import com.example.game.engine.*
import kotlin.math.abs

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var viewModel: GameViewModel? = null
    private val renderer = GameRenderer()
    
    // Camera scroll tracking
    private var scrollXOffset = 0f
    
    // Touch tracking for custom swiping (if desired) or simple walking taps
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDraggingCamera = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            updatePhysics()
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        // Start rendering cycle
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun updatePhysics() {
        val vm = viewModel ?: return
        
        // 1. Smoothly interpolate player position towards target coordinates
        val speed = 0.04f
        val dx = vm.targetPlayerX - vm.playerX
        val dy = vm.targetPlayerY - vm.playerY
        
        if (abs(dx) > 0.01f) vm.playerX += dx * speed
        else vm.playerX = vm.targetPlayerX

        if (abs(dy) > 0.01f) vm.playerY += dy * speed
        else vm.playerY = vm.targetPlayerY

        // 2. Camera follow: center scroll offset on player
        val worldWidth = width * 3f
        val playerWorldX = vm.playerX * worldWidth
        
        // Target scroll to center player
        val targetScroll = playerWorldX - width / 2f
        
        // Clamp scroll between far left (0) and far right (worldWidth - screen width)
        val maxScroll = (worldWidth - width).coerceAtLeast(0f)
        scrollXOffset = targetScroll.coerceIn(0f, maxScroll)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val vm = viewModel ?: return
        renderer.draw(canvas, width, height, vm, scrollXOffset)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val vm = viewModel ?: return true
        
        // Do not handle walking touches in battle or menu scenes
        val curScene = vm.scene.value
        if (curScene != GameScene.TOWN && curScene != GameScene.FOREST && curScene != GameScene.HOME && curScene != GameScene.MUSEUM) {
            return super.onTouchEvent(event)
        }

        val worldWidth = if (curScene == GameScene.TOWN) width * 3f else if (curScene == GameScene.FOREST) width * 2f else width.toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                isDraggingCamera = false
            }
            MotionEvent.ACTION_MOVE -> {
                // If the user swiped horizontally far enough with drag intent, we could pan
                val dx = event.x - touchStartX
                if (abs(dx) > 40f) {
                    isDraggingCamera = true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDraggingCamera) {
                    // It's a walk tap!
                    // Convert screen coordinate back to relative game coordinates considering the scrolling camera offset
                    val tapWorldX = event.x + scrollXOffset
                    val relativeX = (tapWorldX / worldWidth).coerceIn(0f, 1f)
                    
                    // Tap on vertical axis is mapped from walkable ground height: say 0.35f to 0.9f
                    val groundY = height * 0.35f
                    val relativeY = ((event.y - groundY) / (height - groundY - 200f)).coerceIn(0f, 1f)

                    // Set target coordinate to move smoothly
                    vm.targetPlayerX = relativeX
                    vm.targetPlayerY = relativeY

                    // Auto-interaction check: if tapped near a fossil, wild insect, or villager, set target near them
                    checkNearbyWalkInteractions(tapWorldX, event.y, vm, worldWidth, groundY)
                }
            }
        }
        return true
    }

    private fun checkNearbyWalkInteractions(tapWorldX: Float, tapY: Float, vm: GameViewModel, worldWidth: Float, groundY: Float) {
        // If they tapped on an animal villager, let's trigger dialogue when we walk close
        for (v in vm.villagers) {
            val vx = v.activityX * worldWidth
            val vy = groundY + 180f + v.activityY * (height - groundY - 300f)
            if (Math.hypot((vx - tapWorldX).toDouble(), (vy - tapY).toDouble()) < 120.0) {
                // Tapped the villager! Move player to them and talk
                vm.targetPlayerX = v.activityX
                vm.targetPlayerY = v.activityY + 0.05f
                
                // Trigger conversation immediately or when player arrives
                vm.talkToVillager(v)
                break
            }
        }

        // Tapped a fossil crack? Dig if shovel equipped
        for (fossil in vm.playerData.fossilSpawns) {
            val fx = fossil.first * worldWidth
            val fy = groundY + 220f + fossil.second * (height - groundY - 350f)
            if (Math.hypot((fx - tapWorldX).toDouble(), (fy - tapY).toDouble()) < 80.0) {
                vm.targetPlayerX = fossil.first
                vm.targetPlayerY = fossil.second
                vm.digFossilAt(fossil.first, fossil.second)
                break
            }
        }

        // Tapped a flying bug? Catch if net equipped
        for (bug in vm.activeBugs) {
            val bx = bug.x * worldWidth
            val by = groundY + 100f + bug.y * (height - groundY - 260f)
            if (Math.hypot((bx - tapWorldX).toDouble(), (by - tapY).toDouble()) < 90.0) {
                vm.targetPlayerX = bug.x
                vm.targetPlayerY = bug.y
                vm.catchBug(bug)
                break
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }
}
