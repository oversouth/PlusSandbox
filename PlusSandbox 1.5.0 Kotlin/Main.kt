import java.awt.*
import java.awt.event.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*

class Main : JPanel(), Runnable, MouseListener, MouseMotionListener, KeyListener {
    companion object {
        private const val WIDTH = 800
        private const val HEIGHT = 600
        private const val CELL_SIZE = 4
        private const val COLS = WIDTH / CELL_SIZE
        private const val ROWS = HEIGHT / CELL_SIZE

        // Элементы
        const val EMPTY = 0
        const val SAND = 1
        const val WATER = 2
        const val LAVA = 3
        const val FIRE = 4
        const val EARTH = 5
        const val STONE = 6
        const val SMOKE = 7
        const val SEED = 8
        const val GRASS = 9
        const val WOOD = 10
        const val ICE = 11
        const val OIL = 12
        const val ERASER = 13
        const val IRON = 14
        const val NITROGEN = 15
        const val UNBREAKABLE = 16
        const val ACID = 17
        const val GLASS = 18
        const val DYNAMITE = 19
        const val GOLD = 20
        const val COPPER = 21
        const val SALT = 22
        const val CEMENT = 23
        const val RUBBER = 24
        const val GASOLINE = 25
        const val MERCURY = 26
        const val URANIUM = 27
        const val MOLTEN_IRON = 28
        const val MOLTEN_GOLD = 29
        const val MOLTEN_COPPER = 30
        const val LAMP = 31
        const val MEAT = 32
        const val ROTTEN_MEAT = 33
        const val PUMPKIN = 34
        const val WATERMELON = 35
        const val JUICE = 36
        const val PUMPKIN_SEED = 37
        const val WATERMELON_SEED = 38
        const val APPLE_SEED = 39
        const val CRUDE_OIL = 40
        const val GAS = 41
        const val LIQUEFIED_GAS = 42
        const val COAL = 43
        const val SANDSTONE = 44
        const val GRAY_SAND = 45
        const val PEAT = 46

        @JvmStatic
        fun main(args: Array<String>) {
            val frame = JFrame("PlusSandbox - v1.5.0 (Kotlin)")
            val game = Main()
            
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.isResizable = false
            frame.add(game)
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
            
            Thread(game).start()
        }
    }

    private var grid = Array(COLS) { IntArray(ROWS) }
    private var gridBuffer = Array(COLS) { IntArray(ROWS) }
    private var running = true
    private var paused = false
    private var showSaveMenu = false
    private var showLoadMenu = false
    private val random = Random()
    
    private var currentElement = SAND
    private var brushSize = 3
    private var mousePressed = false
    
    private var fps = 0
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var simulationSpeed = 1.0f
    
    private var heatGrid = Array(COLS) { IntArray(ROWS) }
    private var fireLifeGrid = Array(COLS) { IntArray(ROWS) }
    private var roundBrush = true
    private var lightGrid = Array(COLS) { IntArray(ROWS) }
    private val explosions = mutableListOf<Explosion>()
    
    private var saveFileName = ""
    private var saveFiles = emptyArray<String>()
    private var selectedSaveIndex = -1
    
    private var decayGrid = Array(COLS) { IntArray(ROWS) }

    inner class Explosion(val x: Int, val y: Int, val radius: Int) {
        var life = 20
        
        fun update() {
            life--
        }
        
        fun isAlive() = life > 0
    }

    init {
        preferredSize = Dimension(WIDTH, HEIGHT)
        background = Color.BLACK
        addMouseListener(this)
        addMouseMotionListener(this)
        addKeyListener(this)
        isFocusable = true
        
        // Создаем папку saves если её нет
        val savesDir = File("saves")
        if (!savesDir.exists()) {
            savesDir.mkdir()
        }
        
        lastFpsTime = System.currentTimeMillis()
        refreshSaveFiles()
    }
    
    private fun refreshSaveFiles() {
        saveFiles = getSaveFiles()
    }
    
    override fun run() {
        while (running) {
            val startTime = System.currentTimeMillis()
            
            if (!paused) {
                repeat(simulationSpeed.toInt()) {
                    updatePhysics()
                    updateLighting()
                }
                updateExplosions()
            }
            updateFPS()
            repaint()
            
            val endTime = System.currentTimeMillis()
            val sleepTime = Math.max(1, 16 - (endTime - startTime))
            
            try {
                Thread.sleep((sleepTime / simulationSpeed).toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
    
    private fun updateFPS() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 1000) {
            fps = frameCount
            frameCount = 0
            lastFpsTime = currentTime
        }
    }
    
    private fun updatePhysics() {
        // Копируем текущее состояние в буфер
        for (x in 0 until COLS) {
            for (y in 0 until ROWS) {
                gridBuffer[x][y] = grid[x][y]
            }
        }
        
        // Обновляем физику снизу вверх для стабильности
        for (y in ROWS - 2 downTo 0) {
            for (x in 0 until COLS) {
                val element = grid[x][y]
                
                if (element == EMPTY) continue
                
                when (element) {
                    SAND -> updateSand(x, y)
                    WATER -> updateWater(x, y)
                    LAVA -> updateLava(x, y)
                    FIRE -> updateFire(x, y)
                    EARTH -> updateEarth(x, y)
                    STONE -> updateStone(x, y)
                    SMOKE -> updateSmoke(x, y)
                    SEED -> updateSeed(x, y)
                    GRASS -> updateGrass(x, y)
                    WOOD -> updateWood(x, y)
                    ICE -> updateIce(x, y)
                    OIL -> updateOil(x, y)
                    IRON -> updateIron(x, y)
                    NITROGEN -> updateNitrogen(x, y)
                    UNBREAKABLE -> updateUnbreakable(x, y)
                    ACID -> updateAcid(x, y)
                    GLASS -> updateGlass(x, y)
                    DYNAMITE -> updateDynamite(x, y)
                    GOLD -> updateGold(x, y)
                    COPPER -> updateCopper(x, y)
                    SALT -> updateSalt(x, y)
                    CEMENT -> updateCement(x, y)
                    RUBBER -> updateRubber(x, y)
                    GASOLINE -> updateGasoline(x, y)
                    MERCURY -> updateMercury(x, y)
                    URANIUM -> updateUranium(x, y)
                    MOLTEN_IRON -> updateMoltenIron(x, y)
                    MOLTEN_GOLD -> updateMoltenGold(x, y)
                    MOLTEN_COPPER -> updateMoltenCopper(x, y)
                    LAMP -> updateLamp(x, y)
                    MEAT -> updateMeat(x, y)
                    ROTTEN_MEAT -> updateRottenMeat(x, y)
                    PUMPKIN -> updatePumpkin(x, y)
                    WATERMELON -> updateWatermelon(x, y)
                    JUICE -> updateJuice(x, y)
                    PUMPKIN_SEED -> updatePumpkinSeed(x, y)
                    WATERMELON_SEED -> updateWatermelonSeed(x, y)
                    APPLE_SEED -> updateAppleSeed(x, y)
                    CRUDE_OIL -> updateCrudeOil(x, y)
                    GAS -> updateGas(x, y)
                    LIQUEFIED_GAS -> updateLiquefiedGas(x, y)
                    COAL -> updateCoal(x, y)
                    SANDSTONE -> updateSandstone(x, y)
                    GRAY_SAND -> updateGraySand(x, y)
                    PEAT -> updatePeat(x, y)
                }
            }
        }
        
        // Копируем буфер обратно в основную сетку
        for (x in 0 until COLS) {
            for (y in 0 until ROWS) {
                grid[x][y] = gridBuffer[x][y]
            }
        }
    }
    
    private fun updateLighting() {
        // Сбрасываем свет
        for (x in 0 until COLS) {
            for (y in 0 until ROWS) {
                lightGrid[x][y] = 0
            }
        }
        
        // Распространяем свет от источников
        for (x in 0 until COLS) {
            for (y in 0 until ROWS) {
                val element = grid[x][y]
                var lightLevel = 0
                
                lightLevel = when (element) {
                    LAMP -> 100
                    FIRE -> 60
                    LAVA -> 40
                    URANIUM -> if (heatGrid[x][y] > 50) 30 else 0
                    COAL -> if (heatGrid[x][y] > 10) 20 else 0
                    else -> 0
                }
                
                if (lightLevel > 0) {
                    spreadLight(x, y, lightLevel)
                }
            }
        }
    }
    
    private fun spreadLight(x: Int, y: Int, lightLevel: Int) {
        if (lightLevel <= 0) return
        
        // Устанавливаем свет в текущей клетке
        if (lightGrid[x][y] < lightLevel) {
            lightGrid[x][y] = lightLevel
        }
        
        // Распространяем свет в соседние клетки
        val directions = arrayOf(intArrayOf(0, -1), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 0))
        for (dir in directions) {
            val nx = x + dir[0]
            val ny = y + dir[1]
            
            if (nx in 0 until COLS && ny in 0 until ROWS) {
                // Свет проходит через прозрачные материалы и песок
                if (grid[nx][ny] == EMPTY || grid[nx][ny] == GLASS || 
                    grid[nx][ny] == WATER || grid[nx][ny] == SMOKE ||
                    grid[nx][ny] == NITROGEN || grid[nx][ny] == GAS ||
                    grid[nx][ny] == SAND) {
                    val newLightLevel = lightLevel - 15
                    if (newLightLevel > lightGrid[nx][ny]) {
                        spreadLight(nx, ny, newLightLevel)
                    }
                }
            }
        }
    }
    
    private fun updateSand(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryMoveDiagonal(x, y)) return
        
        // НОВАЯ МЕХАНИКА: Песок нагревается и превращается в песчаник
        if (checkFireNearby(x, y) || checkLavaNearby(x, y)) {
            heatGrid[x][y]++
            if (heatGrid[x][y] > 50) {
                // При сильном нагреве песок превращается в песчаник
                gridBuffer[x][y] = SANDSTONE
                heatGrid[x][y] = 0
            }
        } else if (heatGrid[x][y] > 0) {
            // Постепенно остывает
            heatGrid[x][y]--
        }
        
        // НОВАЯ МЕХАНИКА: Песок ОЧЕНЬ медленно горит и превращается в серый песок
        if (checkFireNearby(x, y) && random.nextFloat() < 0.0001f) {
            gridBuffer[x][y] = GRAY_SAND
        }
    }
    
    private fun updatePeat(x: Int, y: Int) {
        // Торф - горючий материал, похожий на землю
        if (checkFireNearby(x, y) && random.nextFloat() < 0.005f) {
            gridBuffer[x][y] = FIRE
            fireLifeGrid[x][y] = 100
            createSmokeAround(x, y)
        }
        
        // Торф может медленно тлеть
        if (gridBuffer[x][y] == FIRE && random.nextFloat() < 0.01f) {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until COLS && ny in 0 until ROWS && 
                        gridBuffer[nx][ny] == PEAT && random.nextFloat() < 0.1f) {
                        gridBuffer[nx][ny] = FIRE
                        fireLifeGrid[nx][ny] = 80
                    }
                }
            }
        }
    }
    
    private fun updateSandstone(x: Int, y: Int) {
        // Песчаник - твердый материал, неподвижен
    }
    
    private fun updateGraySand(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryMoveDiagonal(x, y)) return
    }
    
    private fun updateWater(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        checkLavaInteraction(x, y, WATER, STONE)
        checkFireInteraction(x, y, WATER, SMOKE)
    }
    
    private fun updateLava(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (random.nextFloat() < 0.3f && tryFlow(x, y)) return
        
        if (random.nextFloat() < 0.02f) {
            createFireAround(x, y)
        }
        
        // Охлаждение лавы в камень
        if (random.nextFloat() < 0.005f) {
            gridBuffer[x][y] = STONE
        }
        
        // Нагрев металлов
        heatMetal(x, y, IRON, MOLTEN_IRON, 0.8f)
        heatMetal(x, y, GOLD, MOLTEN_GOLD, 0.7f)
        heatMetal(x, y, COPPER, MOLTEN_COPPER, 0.9f)
        
        // Застывание от жидкого азота
        if (checkLiquidNitrogenNearby(x, y) && random.nextFloat() < 0.8f) {
            gridBuffer[x][y] = STONE
        }
        
        // Застывание от воды
        if (checkWaterNearby(x, y) && random.nextFloat() < 0.5f) {
            gridBuffer[x][y] = STONE
        }
    }
    
    private fun updateFire(x: Int, y: Int) {
        if (tryMove(x, y, 0, -1)) return
        
        if (random.nextFloat() < 0.3f) {
            val dx = random.nextInt(3) - 1
            if (tryMove(x, y, dx, -1)) return
        }
        
        checkCombustibleMaterials(x, y)
        
        if (random.nextFloat() < 0.1f) {
            createSmokeAround(x, y)
        }
        
        // Нагрев металлов огнем
        heatMetal(x, y, IRON, MOLTEN_IRON, 0.3f)
        heatMetal(x, y, GOLD, MOLTEN_GOLD, 0.2f)
        heatMetal(x, y, COPPER, MOLTEN_COPPER, 0.4f)
        
        // Нагрев урана
        if (checkUraniumNearby(x, y) && random.nextFloat() < 0.1f) {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until COLS && ny in 0 until ROWS && 
                        gridBuffer[nx][ny] == URANIUM) {
                        heatGrid[nx][ny]++
                        if (heatGrid[nx][ny] > 50) {
                            createNuclearExplosion(nx, ny)
                            gridBuffer[nx][ny] = EMPTY
                        }
                    }
                }
            }
        }
        
        // Время горения огня
        if (fireLifeGrid[x][y] > 0) {
            fireLifeGrid[x][y]--
        } else {
            if (random.nextFloat() < 0.03f) {
                gridBuffer[x][y] = EMPTY
            }
        }
    }
    
    private fun updateEarth(x: Int, y: Int) {
        tryMove(x, y, 0, 1)
    }
    
    private fun updateStone(x: Int, y: Int) {
        // Камень неподвижен и не плавится от лавы
    }
    
    private fun updateSmoke(x: Int, y: Int) {
        if (y > 0 && gridBuffer[x][y - 1] == EMPTY) {
            gridBuffer[x][y] = EMPTY
            gridBuffer[x][y - 1] = SMOKE
            return
        }
        
        if (random.nextFloat() < 0.4f) {
            val dx = if (random.nextBoolean()) 1 else -1
            val newX = x + dx
            if (newX in 0 until COLS && gridBuffer[newX][y] == EMPTY) {
                gridBuffer[x][y] = EMPTY
                gridBuffer[newX][y] = SMOKE
                return
            }
        }
        
        if (random.nextFloat() < 0.03f) {
            gridBuffer[x][y] = EMPTY
        }
    }
    
    private fun updateSeed(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        
        if (y < ROWS - 1) {
            val below = gridBuffer[x][y + 1]
            if (below == EARTH) {
                val hasWater = checkWaterNearby(x, y)
                val growthChance = if (hasWater) 0.02f else 0.005f
                
                if (random.nextFloat() < growthChance) {
                    gridBuffer[x][y] = GRASS
                }
            }
        }
    }
    
    private fun updateGrass(x: Int, y: Int) {
        if (y > 0) {
            val hasEarthBelow = (y < ROWS - 1) && (gridBuffer[x][y + 1] == EARTH || gridBuffer[x][y + 1] == GRASS)
            val hasWater = checkWaterNearby(x, y)
            
            if (hasEarthBelow && gridBuffer[x][y - 1] == EMPTY) {
                val growthChance = if (hasWater) 0.001f else 0.0002f
                if (random.nextFloat() < growthChance) {
                    gridBuffer[x][y - 1] = GRASS
                }
            }
        }
        
        if (random.nextFloat() < 0.001f) {
            val directions = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1))
            for (dir in directions) {
                val nx = x + dir[0]
                val ny = y + dir[1]
                if (nx in 0 until COLS && ny in 0 until ROWS) {
                    if (gridBuffer[nx][ny] == EARTH) {
                        val hasWater = checkWaterNearby(nx, ny)
                        if (hasWater || random.nextFloat() < 0.3f) {
                            gridBuffer[nx][ny] = GRASS
                        }
                    }
                }
            }
        }
        
        if (checkFireNearby(x, y) && random.nextFloat() < 0.02f) {
            gridBuffer[x][y] = FIRE
            fireLifeGrid[x][y] = 30
            createSmokeAround(x, y)
        }
    }
    
    private fun updateWood(x: Int, y: Int) {
        if (checkFireNearby(x, y) && random.nextFloat() < 0.01f) {
            gridBuffer[x][y] = FIRE
            fireLifeGrid[x][y] = 80
            createSmokeAround(x, y)
        }
    }
    
    private fun updateIce(x: Int, y: Int) {
        if (y < ROWS - 1 && gridBuffer[x][y + 1] == WATER) {
            if (random.nextFloat() < 0.1f) {
                gridBuffer[x][y] = WATER
                gridBuffer[x][y + 1] = ICE
                return
            }
        }
        
        if (checkFireNearby(x, y) || checkLavaNearby(x, y)) {
            gridBuffer[x][y] = WATER
            return
        }
        
        if (y < ROWS - 1 && (gridBuffer[x][y + 1] == LAVA || gridBuffer[x][y + 1] == OIL)) {
            if (random.nextFloat() < 0.05f) {
                gridBuffer[x][y] = gridBuffer[x][y + 1]
                gridBuffer[x][y + 1] = ICE
            }
        }
    }
    
    private fun updateOil(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        if (checkFireNearby(x, y) && random.nextFloat() < 0.3f) {
            gridBuffer[x][y] = FIRE
            fireLifeGrid[x][y] = 40
            createSmokeAround(x, y)
            for (i in -1..1) {
                for (j in -1..1) {
                    val nx = x + i
                    val ny = y + j
                    if (nx in 0 until COLS && ny in 0 until ROWS && 
                        gridBuffer[nx][ny] == OIL) {
                        gridBuffer[nx][ny] = FIRE
                        fireLifeGrid[nx][ny] = 40
                    }
                }
            }
        }
    }
    
    private fun updateIron(x: Int, y: Int) {
        // Охлаждение расплавленного железа
        if (checkLiquidNitrogenNearby(x, y) && random.nextFloat() < 0.05f) {
            if (heatGrid[x][y] > 0) {
                heatGrid[x][y]--
            }
        }
    }
    
    private fun updateNitrogen(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        // Застывание лавы
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS) {
                    if (gridBuffer[nx][ny] == LAVA && random.nextFloat() < 0.8f) {
                        gridBuffer[nx][ny] = STONE
                    }
                    // Застывание расплавленных металлов
                    if ((gridBuffer[nx][ny] == MOLTEN_IRON || 
                         gridBuffer[nx][ny] == MOLTEN_GOLD || 
                         gridBuffer[nx][ny] == MOLTEN_COPPER) && random.nextFloat() < 0.1f) {
                        gridBuffer[nx][ny] = when (gridBuffer[nx][ny]) {
                            MOLTEN_IRON -> IRON
                            MOLTEN_GOLD -> GOLD
                            else -> COPPER
                        }
                        heatGrid[nx][ny] = 0
                    }
                    // Заморозка воды
                    if (gridBuffer[nx][ny] == WATER && random.nextFloat() < 0.1f) {
                        gridBuffer[nx][ny] = ICE
                    }
                    // Заморозка кислоты
                    if (gridBuffer[nx][ny] == ACID && random.nextFloat() < 0.05f) {
                        gridBuffer[nx][ny] = ICE
                    }
                }
            }
        }
        
        // Испарение азота
        if (random.nextFloat() < 0.02f) {
            gridBuffer[x][y] = EMPTY
        }
    }
    
    private fun updateUnbreakable(x: Int, y: Int) {
        // Абсолютно нерушимая стена
    }
    
    private fun updateAcid(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        // Растворение большинства материалов
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS) {
                    val target = gridBuffer[nx][ny]
                    if (target != EMPTY && target != UNBREAKABLE && target != RUBBER && 
                        target != ACID && random.nextFloat() < 0.3f) {
                        gridBuffer[nx][ny] = EMPTY
                    }
                }
            }
        }
    }
    
    private fun updateGlass(x: Int, y: Int) {
        // Может плавиться от лавы
        if (checkLavaNearby(x, y) && random.nextFloat() < 0.05f) {
            gridBuffer[x][y] = LAVA
        }
    }
    
    private fun updateDynamite(x: Int, y: Int) {
        if (checkFireNearby(x, y) || checkLavaNearby(x, y)) {
            createExplosion(x, y, 8)
            gridBuffer[x][y] = EMPTY
        }
    }
    
    private fun updateGold(x: Int, y: Int) {
        // Охлаждение
        if (checkLiquidNitrogenNearby(x, y) && random.nextFloat() < 0.05f) {
            if (heatGrid[x][y] > 0) {
                heatGrid[x][y]--
            }
        }
    }
    
    private fun updateCopper(x: Int, y: Int) {
        // Охлаждение
        if (checkLiquidNitrogenNearby(x, y) && random.nextFloat() < 0.05f) {
            if (heatGrid[x][y] > 0) {
                heatGrid[x][y]--
            }
        }
    }
    
    private fun updateSalt(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryMoveDiagonal(x, y)) return
        
        // Растворение в воде
        if (checkWaterNearby(x, y) && random.nextFloat() < 0.1f) {
            gridBuffer[x][y] = EMPTY
        }
    }
    
    private fun updateCement(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        
        // Затвердевание при контакте с водой
        if (checkWaterNearby(x, y) && random.nextFloat() < 0.01f) {
            gridBuffer[x][y] = STONE
        }
    }
    
    private fun updateRubber(x: Int, y: Int) {
        tryMove(x, y, 0, 1)
    }
    
    private fun updateGasoline(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        if (checkFireNearby(x, y) && random.nextFloat() < 0.5f) {
            gridBuffer[x][y] = FIRE
            fireLifeGrid[x][y] = 60
            createSmokeAround(x, y)
            for (i in -2..2) {
                for (j in -2..2) {
                    val nx = x + i
                    val ny = y + j
                    if (nx in 0 until COLS && ny in 0 until ROWS && 
                        gridBuffer[nx][ny] == GASOLINE) {
                        gridBuffer[nx][ny] = FIRE
                        fireLifeGrid[nx][ny] = 60
                    }
                }
            }
        }
    }
    
    private fun updateMercury(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        if (y < ROWS - 1 && gridBuffer[x][y + 1] == WATER) {
            if (random.nextFloat() < 0.1f) {
                gridBuffer[x][y] = WATER
                gridBuffer[x][y + 1] = MERCURY
            }
        }
    }
    
    private fun updateUranium(x: Int, y: Int) {
        // Постепенно нагревается сам по себе
        if (random.nextFloat() < 0.001f) {
            heatGrid[x][y]++
        }
        
        // Взрыв при перегреве
        if (heatGrid[x][y] > 100) {
            createNuclearExplosion(x, y)
            gridBuffer[x][y] = EMPTY
        }
        
        // Свечение при нагреве
        if (heatGrid[x][y] > 50 && random.nextFloat() < 0.1f) {
            createFireAround(x, y)
        }
    }
    
    private fun updateMoltenIron(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        // Охлаждение и застывание
        if (checkLiquidNitrogenNearby(x, y) && random.nextFloat() < 0.1f) {
            gridBuffer[x][y] = IRON
            heatGrid[x][y] = 0
        } else if (random.nextFloat() < 0.001f) {
            gridBuffer[x][y] = IRON
            heatGrid[x][y] = 0
        }
    }
    
    private fun updateMoltenGold(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        // Охлаждение и застывание
        if (checkLiquidNitrogenNearby(x, y) && random.nextFloat() < 0.1f) {
            gridBuffer[x][y] = GOLD
            heatGrid[x][y] = 0
        } else if (random.nextFloat() < 0.001f) {
            gridBuffer[x][y] = GOLD
            heatGrid[x][y] = 0
        }
    }
    
    private fun updateMoltenCopper(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        // Охлаждение и застывание
        if (checkLiquidNitrogenNearby(x, y) && random.nextFloat() < 0.1f) {
            gridBuffer[x][y] = COPPER
            heatGrid[x][y] = 0
        } else if (random.nextFloat() < 0.001f) {
            gridBuffer[x][y] = COPPER
            heatGrid[x][y] = 0
        }
    }
    
    private fun updateLamp(x: Int, y: Int) {
        // Лампа - статичный светящийся элемент
    }
    
    private fun updateMeat(x: Int, y: Int) {
        if (decayGrid[x][y] < 500) {
            decayGrid[x][y]++
        } else {
            if (random.nextFloat() < 0.005f) {
                gridBuffer[x][y] = ROTTEN_MEAT
                if (random.nextFloat() < 0.3f) {
                    createSmokeAround(x, y)
                }
            }
        }
        
        if (checkFireNearby(x, y) && random.nextFloat() < 0.01f) {
            gridBuffer[x][y] = FIRE
            fireLifeGrid[x][y] = 50
        }
    }
    
    private fun updateRottenMeat(x: Int, y: Int) {
        if (checkFireNearby(x, y) && random.nextFloat() < 0.05f) {
            gridBuffer[x][y] = FIRE
            fireLifeGrid[x][y] = 60
            createSmokeAround(x, y)
        }
        
        if (random.nextFloat() < 0.001f) {
            gridBuffer[x][y] = EMPTY
            if (random.nextFloat() < 0.5f) {
                createSmokeAround(x, y)
            }
        }
    }
    
    private fun updatePumpkin(x: Int, y: Int) {
        if (checkExplosionNearby(x, y) || checkFireNearby(x, y) || checkLavaNearby(x, y) || 
            checkAcidNearby(x, y)) {
            if (random.nextFloat() < 0.1f) {
                gridBuffer[x][y] = JUICE
                
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until COLS && ny in 0 until ROWS && 
                            gridBuffer[nx][ny] == EMPTY && random.nextFloat() < 0.3f) {
                            gridBuffer[nx][ny] = PUMPKIN_SEED
                        }
                    }
                }
            }
        }
    }
    
    private fun updateWatermelon(x: Int, y: Int) {
        if (checkExplosionNearby(x, y) || checkFireNearby(x, y) || checkLavaNearby(x, y) || 
            checkAcidNearby(x, y)) {
            if (random.nextFloat() < 0.1f) {
                gridBuffer[x][y] = JUICE
                
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until COLS && ny in 0 until ROWS && 
                            gridBuffer[nx][ny] == EMPTY && random.nextFloat() < 0.3f) {
                            gridBuffer[nx][ny] = WATERMELON_SEED
                        }
                    }
                }
            }
        }
    }
    
    private fun updateJuice(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        if (random.nextFloat() < 0.001f) {
            gridBuffer[x][y] = EMPTY
        }
    }
    
    private fun updatePumpkinSeed(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        
        if (y < ROWS - 1) {
            val below = gridBuffer[x][y + 1]
            if (below == EARTH || below == GRASS) {
                val hasWater = checkWaterNearby(x, y)
                val growthChance = if (hasWater) 0.01f else 0.002f
                
                if (random.nextFloat() < growthChance) {
                    if (y > 0 && gridBuffer[x][y - 1] == EMPTY) {
                        gridBuffer[x][y - 1] = PUMPKIN
                        gridBuffer[x][y] = EMPTY
                    }
                }
            }
        }
    }
    
    private fun updateWatermelonSeed(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        
        if (y < ROWS - 1) {
            val below = gridBuffer[x][y + 1]
            if (below == EARTH || below == GRASS) {
                val hasWater = checkWaterNearby(x, y)
                val growthChance = if (hasWater) 0.01f else 0.002f
                
                if (random.nextFloat() < growthChance) {
                    if (y > 0 && gridBuffer[x][y - 1] == EMPTY) {
                        gridBuffer[x][y - 1] = WATERMELON
                        gridBuffer[x][y] = EMPTY
                    }
                }
            }
        }
    }
    
    private fun updateAppleSeed(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        
        if (y < ROWS - 1) {
            val below = gridBuffer[x][y + 1]
            if (below == EARTH || below == GRASS) {
                val hasWater = checkWaterNearby(x, y)
                val growthChance = if (hasWater) 0.008f else 0.001f
                
                if (random.nextFloat() < growthChance) {
                    gridBuffer[x][y] = WOOD
                }
            }
        }
    }
    
    private fun updateCrudeOil(x: Int, y: Int) {
        if (random.nextFloat() < 0.7f && tryMove(x, y, 0, 1)) return
        if (random.nextFloat() < 0.5f && tryFlow(x, y)) return
        
        if (checkFireNearby(x, y) && random.nextFloat() < 0.4f) {
            gridBuffer[x][y] = FIRE
            fireLifeGrid[x][y] = 80
            createSmokeAround(x, y)
            
            for (i in -1..1) {
                for (j in -1..1) {
                    val nx = x + i
                    val ny = y + j
                    if (nx in 0 until COLS && ny in 0 until ROWS && 
                        gridBuffer[nx][ny] == CRUDE_OIL && random.nextFloat() < 0.6f) {
                        gridBuffer[nx][ny] = FIRE
                        fireLifeGrid[nx][ny] = 80
                    }
                }
            }
        }
        
        if ((checkFireNearby(x, y) || checkLavaNearby(x, y)) && random.nextFloat() < 0.01f) {
            gridBuffer[x][y] = GASOLINE
        }
    }
    
    private fun updateGas(x: Int, y: Int) {
        // Газ теперь синий и поднимается вверх
        if (tryMove(x, y, 0, -1)) return
        
        if (random.nextFloat() < 0.6f) {
            val dx = random.nextInt(3) - 1
            if (tryMove(x, y, dx, -1)) return
        }
        
        // Газ легко воспламеняется
        if (checkFireNearby(x, y) && random.nextFloat() < 0.8f) {
            createExplosion(x, y, 5)
            gridBuffer[x][y] = EMPTY
        }
        
        // Газ рассеивается со временем
        if (random.nextFloat() < 0.05f) {
            gridBuffer[x][y] = EMPTY
        }
        
        // Газ может превращаться обратно в сжиженный газ при охлаждении
        if (checkLiquidNitrogenNearby(x, y) && random.nextFloat() < 0.1f) {
            gridBuffer[x][y] = LIQUEFIED_GAS
        }
    }
    
    private fun updateLiquefiedGas(x: Int, y: Int) {
        if (tryMove(x, y, 0, 1)) return
        if (tryFlow(x, y)) return
        
        // Сжиженный газ испаряется в газ
        if (random.nextFloat() < 0.02f) {
            gridBuffer[x][y] = GAS
        }
        
        // Сжиженный газ очень взрывоопасен
        if (checkFireNearby(x, y) && random.nextFloat() < 0.9f) {
            createExplosion(x, y, 8)
            gridBuffer[x][y] = EMPTY
        }
    }
    
    private fun updateCoal(x: Int, y: Int) {
        // Уголь - твердый материал, может гореть долко и выделять тепло
        if (checkFireNearby(x, y) && random.nextFloat() < 0.02f) {
            // Уголь загорается
            heatGrid[x][y] = 100
            if (random.nextFloat() < 0.01f) {
                // Создаем огонь вокруг угля
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until COLS && ny in 0 until ROWS && 
                            gridBuffer[nx][ny] == EMPTY && random.nextFloat() < 0.3f) {
                            gridBuffer[nx][ny] = FIRE
                            fireLifeGrid[nx][ny] = 40
                        }
                    }
                }
            }
        }
        
        // Горящий уголь постепенно остывает и сгорает
        if (heatGrid[x][y] > 0) {
            heatGrid[x][y]--
            if (heatGrid[x][y] == 0) {
                // Уголь полностью сгорает
                gridBuffer[x][y] = EMPTY
                if (random.nextFloat() < 0.5f) {
                    createSmokeAround(x, y)
                }
            }
        }
        
        // Уголь может нагревать соседние металлы
        if (heatGrid[x][y] > 50) {
            heatMetal(x, y, IRON, MOLTEN_IRON, 0.1f)
            heatMetal(x, y, GOLD, MOLTEN_GOLD, 0.08f)
            heatMetal(x, y, COPPER, MOLTEN_COPPER, 0.12f)
        }
    }
    
    // Вспомогательные методы
    private fun tryMove(x: Int, y: Int, dx: Int, dy: Int): Boolean {
        val newX = x + dx
        val newY = y + dy
        
        if (newX in 0 until COLS && newY in 0 until ROWS && 
            gridBuffer[newX][newY] == EMPTY) {
            gridBuffer[x][y] = EMPTY
            gridBuffer[newX][newY] = grid[x][y]
            heatGrid[newX][newY] = heatGrid[x][y]
            fireLifeGrid[newX][newY] = fireLifeGrid[x][y]
            decayGrid[newX][newY] = decayGrid[x][y]
            heatGrid[x][y] = 0
            fireLifeGrid[x][y] = 0
            decayGrid[x][y] = 0
            return true
        }
        return false
    }
    
    private fun tryMoveDiagonal(x: Int, y: Int): Boolean {
        val left = x > 0 && gridBuffer[x - 1][y + 1] == EMPTY
        val right = x < COLS - 1 && gridBuffer[x + 1][y + 1] == EMPTY
        
        return when {
            left && right -> {
                if (random.nextBoolean()) {
                    tryMove(x, y, -1, 1)
                } else {
                    tryMove(x, y, 1, 1)
                }
            }
            left -> tryMove(x, y, -1, 1)
            right -> tryMove(x, y, 1, 1)
            else -> false
        }
    }
    
    private fun tryFlow(x: Int, y: Int): Boolean {
        val directions = intArrayOf(-1, 1)
        if (random.nextBoolean()) {
            val temp = directions[0]
            directions[0] = directions[1]
            directions[1] = temp
        }
        
        for (dx in directions) {
            if (tryMove(x, y, dx, 0)) return true
        }
        
        for (dx in directions) {
            if (y > 0 && tryMove(x, y, dx, -1)) return true
        }
        
        return false
    }
    
    private fun heatMetal(x: Int, y: Int, solidMetal: Int, moltenMetal: Int, heatRate: Float) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS) {
                    if (gridBuffer[nx][ny] == solidMetal && random.nextFloat() < heatRate) {
                        heatGrid[nx][ny]++
                        if (heatGrid[nx][ny] > 30) {
                            gridBuffer[nx][ny] = moltenMetal
                        }
                    }
                }
            }
        }
    }
    
    private fun checkWaterNearby(x: Int, y: Int): Boolean {
        for (dx in -2..2) {
            for (dy in -2..2) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS && 
                    gridBuffer[nx][ny] == WATER) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun checkFireNearby(x: Int, y: Int): Boolean {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS && 
                    (gridBuffer[nx][ny] == FIRE || gridBuffer[nx][ny] == LAVA)) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun checkLavaNearby(x: Int, y: Int): Boolean {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS && 
                    gridBuffer[nx][ny] == LAVA) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun checkLiquidNitrogenNearby(x: Int, y: Int): Boolean {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS && 
                    gridBuffer[nx][ny] == NITROGEN) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun checkUraniumNearby(x: Int, y: Int): Boolean {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS && 
                    gridBuffer[nx][ny] == URANIUM) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun checkUnbreakableNearby(x: Int, y: Int): Boolean {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS && 
                    gridBuffer[nx][ny] == UNBREAKABLE) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun checkExplosionNearby(x: Int, y: Int): Boolean {
        for (explosion in explosions) {
            if (explosion.isAlive()) {
                val dx = x - explosion.x
                val dy = y - explosion.y
                if (dx * dx + dy * dy <= explosion.radius * explosion.radius) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun checkAcidNearby(x: Int, y: Int): Boolean {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS && 
                    gridBuffer[nx][ny] == ACID) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun checkLavaInteraction(x: Int, y: Int, element: Int, result: Int) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS) {
                    if ((gridBuffer[x][y] == LAVA && gridBuffer[nx][ny] == element) ||
                        (gridBuffer[x][y] == element && gridBuffer[nx][ny] == LAVA)) {
                        gridBuffer[x][y] = result
                        gridBuffer[nx][ny] = result
                    }
                }
            }
        }
    }
    
    private fun checkFireInteraction(x: Int, y: Int, element: Int, result: Int) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS) {
                    if ((gridBuffer[x][y] == FIRE && gridBuffer[nx][ny] == element) ||
                        (gridBuffer[x][y] == element && gridBuffer[nx][ny] == FIRE)) {
                        gridBuffer[x][y] = result
                        gridBuffer[nx][ny] = result
                    }
                }
            }
        }
    }
    
    private fun checkCombustibleMaterials(x: Int, y: Int) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS) {
                    val neighbor = gridBuffer[nx][ny]
                    if ((neighbor == SAND || neighbor == EARTH || neighbor == GRASS || 
                         neighbor == WOOD || neighbor == SEED || neighbor == ROTTEN_MEAT ||
                         neighbor == PEAT) && random.nextFloat() < 0.1f) {
                        gridBuffer[nx][ny] = FIRE
                        fireLifeGrid[nx][ny] = 50
                    }
                }
            }
        }
    }
    
    private fun createFireAround(x: Int, y: Int) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS && 
                    gridBuffer[nx][ny] == EMPTY && random.nextFloat() < 0.3f) {
                    gridBuffer[nx][ny] = FIRE
                    fireLifeGrid[nx][ny] = 40
                }
            }
        }
    }
    
    private fun createSmokeAround(x: Int, y: Int) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS && 
                    gridBuffer[nx][ny] == EMPTY && random.nextFloat() < 0.4f) {
                    gridBuffer[nx][ny] = SMOKE
                }
            }
        }
    }
    
    private fun createExplosion(x: Int, y: Int, radius: Int) {
        explosions.add(Explosion(x, y, radius))
        
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (dx * dx + dy * dy <= radius * radius) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until COLS && ny in 0 until ROWS) {
                        // Нерушимая стена не разрушается
                        if (gridBuffer[nx][ny] != UNBREAKABLE && 
                            gridBuffer[nx][ny] != STONE && gridBuffer[nx][ny] != IRON && 
                            gridBuffer[nx][ny] != GOLD && gridBuffer[nx][ny] != COPPER &&
                            gridBuffer[nx][ny] != SANDSTONE) {
                            gridBuffer[nx][ny] = EMPTY
                        }
                        if (dx * dx + dy * dy >= (radius - 1) * (radius - 1)) {
                            if (random.nextFloat() < 0.3f) {
                                gridBuffer[nx][ny] = FIRE
                                fireLifeGrid[nx][ny] = 60
                            }
                            if (random.nextFloat() < 0.5f) {
                                createSmokeAround(nx, ny)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun createNuclearExplosion(x: Int, y: Int) {
        explosions.add(Explosion(x, y, 15))
        
        for (dx in -10..10) {
            for (dy in -10..10) {
                if (dx * dx + dy * dy <= 100) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until COLS && ny in 0 until ROWS) {
                        // Нерушимая стена не разрушается даже ядерным взрывом
                        if (gridBuffer[nx][ny] != UNBREAKABLE) {
                            gridBuffer[nx][ny] = EMPTY
                        }
                        if (dx * dx + dy * dy >= 64) {
                            if (random.nextFloat() < 0.5f) {
                                gridBuffer[nx][ny] = FIRE
                                fireLifeGrid[nx][ny] = 100
                            }
                            if (random.nextFloat() < 0.7f) {
                                createSmokeAround(nx, ny)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun updateExplosions() {
        explosions.removeAll { explosion ->
            explosion.update()
            !explosion.isAlive()
        }
    }
    
    // Методы для сохранения/загрузки
    private fun saveGame(fileName: String) {
        try {
            val file = File("saves/$fileName.sand")
            val fos = FileOutputStream(file)
            val oos = ObjectOutputStream(fos)
            
            oos.writeObject(grid)
            oos.close()
            fos.close()
            
            println("Игра сохранена: $fileName")
            refreshSaveFiles()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    private fun loadGame(fileName: String) {
        try {
            val file = File("saves/$fileName.sand")
            val fis = FileInputStream(file)
            val ois = ObjectInputStream(fis)
            
            @Suppress("UNCHECKED_CAST")
            grid = ois.readObject() as Array<IntArray>
            ois.close()
            fis.close()
            
            explosions.clear()
            heatGrid = Array(COLS) { IntArray(ROWS) }
            fireLifeGrid = Array(COLS) { IntArray(ROWS) }
            lightGrid = Array(COLS) { IntArray(ROWS) }
            decayGrid = Array(COLS) { IntArray(ROWS) }
            println("Игра загружена: $fileName")
        } catch (e: Exception) {
            e.printStackTrace()
            JOptionPane.showMessageDialog(this, "Ошибка загрузки файла: $fileName", "Ошибка", JOptionPane.ERROR_MESSAGE)
        }
    }
    
    private fun getSaveFiles(): Array<String> {
        val savesDir = File("saves")
        val files = savesDir.listFiles { _, name -> name.endsWith(".sand") }
        if (files == null) return emptyArray()
        
        return Array(files.size) { i -> files[i].name.replace(".sand", "") }
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        // Отрисовка элементов с освещением
        for (x in 0 until COLS) {
            for (y in 0 until ROWS) {
                val element = grid[x][y]
                if (element != EMPTY) {
                    var color = getColorForElement(element)
                    
                    // Эффект нагрева для металлов, урана и угля
                    if ((element == IRON || element == GOLD || element == COPPER || 
                         element == URANIUM || element == COAL) && heatGrid[x][y] > 0) {
                        val heatFactor = (heatGrid[x][y] / 30.0f).coerceAtMost(1.0f)
                        color = applyHeatEffect(color, heatFactor)
                    }
                    
                    // Эффект гниения для мяса
                    if (element == MEAT && decayGrid[x][y] > 250) {
                        val decayFactor = ((decayGrid[x][y] - 250) / 250.0f).coerceAtMost(1.0f)
                        color = applyDecayEffect(color, decayFactor)
                    }
                    
                    // Эффект освещения
                    if (lightGrid[x][y] > 0) {
                        color = applyLightEffect(color, lightGrid[x][y])
                    }
                    
                    g.color = color
                    g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE)
                }
            }
        }
        
        // Отрисовка взрывов
        for (explosion in explosions) {
            if (explosion.isAlive()) {
                val alpha = explosion.life / 20.0f
                g.color = Color(255, 165, 0, (alpha * 255).toInt())
                val size = (explosion.radius * CELL_SIZE * 2 * alpha).toInt()
                g.fillOval(explosion.x * CELL_SIZE - size/2, explosion.y * CELL_SIZE - size/2, size, size)
            }
        }
        
        // Отрисовка UI
        g.color = Color.WHITE
        g.drawString("Элемент: ${getElementName(currentElement)} | Кисть: $brushSize", 10, 20)
        g.drawString("Форма: ${if (roundBrush) "Круглая" else "Квадратная"} | Скорость: ${simulationSpeed}x", 10, 40)
        g.drawString("1-9,0,A-Z: элементы | +/-: размер | Ctrl+C: очистить | ПРОБЕЛ: пауза", 10, 60)
        g.drawString("Ctrl+A: круглая кисть | Ctrl+S: квадратная | Стрелки: скорость", 10, 80)
        g.drawString("Ctrl+X: сохранить | Ctrl+L: загрузить", 10, 100)
        g.drawString("Ё: песчаник | Alt+Ё: серый песок | Alt+1: торф", 10, 120)
        
        // FPS
        g.drawString("FPS: $fps", WIDTH - 80, 20)
        
        if (paused) {
            g.color = Color.RED
            g.drawString("ПАУЗА", WIDTH - 60, 40)
        }
        
        // Меню сохранения
        if (showSaveMenu) {
            drawSaveMenu(g)
        }
        
        // Меню загрузки
        if (showLoadMenu) {
            drawLoadMenu(g)
        }
    }
    
    private fun applyHeatEffect(baseColor: Color, heatFactor: Float): Color {
        var r = baseColor.red
        var g = baseColor.green
        val b = baseColor.blue
        
        r = (r + 100 * heatFactor).toInt().coerceAtMost(255)
        g = (g - 50 * heatFactor).toInt().coerceAtLeast(0)
        
        return Color(r, g, b)
    }
    
    private fun applyDecayEffect(baseColor: Color, decayFactor: Float): Color {
        var r = baseColor.red
        var g = baseColor.green
        var b = baseColor.blue
        
        r = (r - 50 * decayFactor).toInt().coerceAtLeast(0)
        g = (g - 30 * decayFactor).toInt().coerceAtLeast(0)
        b = (b - 70 * decayFactor).toInt().coerceAtLeast(0)
        
        return Color(r, g, b)
    }
    
    private fun applyLightEffect(baseColor: Color, lightLevel: Int): Color {
        var r = baseColor.red
        var g = baseColor.green
        var b = baseColor.blue
        
        val lightFactor = (lightLevel / 100.0f).coerceAtMost(1.0f)
        r = (r + (255 - r) * lightFactor * 0.3f).toInt().coerceAtMost(255)
        g = (g + (255 - g) * lightFactor * 0.3f).toInt().coerceAtMost(255)
        b = (b + (255 - b) * lightFactor * 0.3f).toInt().coerceAtMost(255)
        
        return Color(r, g, b)
    }
    
    private fun drawSaveMenu(g: Graphics) {
        g.color = Color(0, 0, 0, 200)
        g.fillRect(100, 100, WIDTH - 200, HEIGHT - 200)
        
        g.color = Color.WHITE
        g.drawString("МЕНЮ СОХРАНЕНИЯ", WIDTH/2 - 60, 130)
        g.drawString("Введите имя файла: $saveFileName", 120, 160)
        g.drawString("Нажмите ENTER для сохранения", 120, 180)
        g.drawString("Нажмите ESC для отмены", 120, 200)
        
        g.drawString("Существующие сохранения:", 120, 230)
        for (i in saveFiles.indices.take(10)) {
            g.drawString("${i + 1}. ${saveFiles[i]}", 120, 250 + i * 20)
        }
    }
    
    private fun drawLoadMenu(g: Graphics) {
        g.color = Color(0, 0, 0, 200)
        g.fillRect(100, 100, WIDTH - 200, HEIGHT - 200)
        
        g.color = Color.WHITE
        g.drawString("МЕНЮ ЗАГРУЗКИ", WIDTH/2 - 50, 130)
        g.drawString("Выберите сохранение для загрузки:", 120, 160)
        g.drawString("Нажмите ENTER для загрузки", 120, 180)
        g.drawString("Нажмите ESC для отмены", 120, 200)
        g.drawString("Стрелки ВВЕРХ/ВНИЗ для выбора", 120, 220)
        
        g.drawString("Доступные сохранения:", 120, 250)
        for (i in saveFiles.indices.take(10)) {
            if (i == selectedSaveIndex) {
                g.color = Color.YELLOW
                g.drawString("> ${saveFiles[i]}", 120, 280 + i * 20)
                g.color = Color.WHITE
            } else {
                g.drawString("${i + 1}. ${saveFiles[i]}", 120, 280 + i * 20)
            }
        }
        
        if (saveFiles.isEmpty()) {
            g.drawString("Нет сохраненных игр", 120, 280)
        }
    }
    
    private fun getColorForElement(element: Int): Color {
        return when (element) {
            SAND -> Color(240, 230, 140)
            WATER -> Color(30, 144, 255, 180)
            LAVA -> Color(255, 69, 0)
            FIRE -> arrayOf(Color.RED, Color.ORANGE, Color.YELLOW)[random.nextInt(3)]
            EARTH -> Color(139, 69, 19)
            STONE -> Color(128, 128, 128)
            SMOKE -> Color(105, 105, 105, 180)
            SEED -> Color(34, 139, 34)
            GRASS -> Color(50, 205, 50)
            WOOD -> Color(101, 67, 33)
            ICE -> Color(200, 230, 255, 220)
            OIL -> Color(25, 25, 25)
            IRON -> Color(192, 192, 192)
            NITROGEN -> Color(70, 130, 180, 200)
            UNBREAKABLE -> Color(50, 50, 50)
            ACID -> Color(50, 255, 50, 200)
            GLASS -> Color(200, 200, 255, 100)
            DYNAMITE -> Color(178, 34, 34)
            GOLD -> Color(255, 215, 0)
            COPPER -> Color(184, 115, 51)
            SALT -> Color(255, 255, 255)
            CEMENT -> Color(210, 210, 210)
            RUBBER -> Color(40, 40, 40)
            GASOLINE -> Color(255, 255, 0, 150)
            MERCURY -> Color(220, 220, 220)
            URANIUM -> Color(0, 255, 0)
            MOLTEN_IRON -> Color(255, 100, 0)
            MOLTEN_GOLD -> Color(255, 200, 0)
            MOLTEN_COPPER -> Color(255, 150, 50)
            LAMP -> Color(255, 255, 200)
            ERASER -> Color.WHITE
            MEAT -> Color(200, 50, 50)
            ROTTEN_MEAT -> Color(100, 80, 50)
            PUMPKIN -> Color(255, 140, 0)
            WATERMELON -> Color(0, 150, 0)
            JUICE -> Color(255, 200, 100, 180)
            PUMPKIN_SEED -> Color(150, 100, 50)
            WATERMELON_SEED -> Color(100, 150, 50)
            APPLE_SEED -> Color(120, 80, 40)
            CRUDE_OIL -> Color(20, 20, 20)
            GAS -> Color(100, 100, 255, 150)
            LIQUEFIED_GAS -> Color(120, 120, 255)
            COAL -> Color(30, 30, 30)
            SANDSTONE -> Color(210, 180, 140)
            GRAY_SAND -> Color(160, 160, 160)
            PEAT -> Color(80, 60, 40)
            else -> Color.BLACK
        }
    }
    
    private fun getElementName(element: Int): String {
        return when (element) {
            SAND -> "Песок"
            WATER -> "Вода"
            LAVA -> "Лава"
            FIRE -> "Огонь"
            EARTH -> "Земля"
            STONE -> "Камень"
            SMOKE -> "Дым"
            SEED -> "Семена"
            GRASS -> "Трава"
            WOOD -> "Дерево"
            ICE -> "Лёд"
            OIL -> "Масло"
            IRON -> "Железо"
            NITROGEN -> "Жидкий азот"
            UNBREAKABLE -> "Нерушимая стена"
            ACID -> "Кислота"
            GLASS -> "Стекло"
            DYNAMITE -> "Динамит"
            GOLD -> "Золото"
            COPPER -> "Медь"
            SALT -> "Соль"
            CEMENT -> "Цемент"
            RUBBER -> "Резина"
            GASOLINE -> "Бензин"
            MERCURY -> "Ртуть"
            URANIUM -> "Уран"
            MOLTEN_IRON -> "Расплавленное железо"
            MOLTEN_GOLD -> "Расплавленное золото"
            MOLTEN_COPPER -> "Расплавленная медь"
            LAMP -> "Лампа"
            ERASER -> "Ластик"
            MEAT -> "Мясо"
            ROTTEN_MEAT -> "Гнилое мясо"
            PUMPKIN -> "Тыква"
            WATERMELON -> "Арбуз"
            JUICE -> "Сок"
            PUMPKIN_SEED -> "Семена тыквы"
            WATERMELON_SEED -> "Семена арбуза"
            APPLE_SEED -> "Семена яблони"
            CRUDE_OIL -> "Нефть"
            GAS -> "Газ"
            LIQUEFIED_GAS -> "Сжиженный газ"
            COAL -> "Уголь"
            SANDSTONE -> "Песчаник"
            GRAY_SAND -> "Серый песок"
            PEAT -> "Торф"
            else -> "Пустота"
        }
    }
    
    private fun placeElement(x: Int, y: Int) {
        if (showSaveMenu || showLoadMenu) return
        
        val gridX = x / CELL_SIZE
        val gridY = y / CELL_SIZE
        
        for (dx in -brushSize..brushSize) {
            for (dy in -brushSize..brushSize) {
                val newX = gridX + dx
                val newY = gridY + dy
                
                if (newX in 0 until COLS && newY in 0 until ROWS) {
                    val shouldPlace = if (roundBrush) {
                        dx * dx + dy * dy <= brushSize * brushSize
                    } else {
                        Math.abs(dx) <= brushSize && Math.abs(dy) <= brushSize
                    }
                    
                    if (shouldPlace) {
                        if (currentElement == ERASER) {
                            grid[newX][newY] = EMPTY
                            heatGrid[newX][newY] = 0
                            fireLifeGrid[newX][newY] = 0
                            lightGrid[newX][newY] = 0
                            decayGrid[newX][newY] = 0
                        } else {
                            grid[newX][newY] = currentElement
                            if (currentElement != URANIUM && 
                                currentElement != IRON && 
                                currentElement != GOLD && 
                                currentElement != COPPER &&
                                currentElement != COAL &&
                                currentElement != SAND) {
                                heatGrid[newX][newY] = 0
                            }
                            if (currentElement == FIRE) {
                                fireLifeGrid[newX][newY] = 100
                            } else {
                                fireLifeGrid[newX][newY] = 0
                            }
                            if (currentElement == MEAT) {
                                decayGrid[newX][newY] = 0
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Mouse events
    override fun mousePressed(e: MouseEvent) {
        if (showSaveMenu || showLoadMenu) return
        mousePressed = true
        placeElement(e.x, e.y)
    }
    
    override fun mouseReleased(e: MouseEvent) {
        mousePressed = false
    }
    
    override fun mouseDragged(e: MouseEvent) {
        if (showSaveMenu || showLoadMenu) return
        if (mousePressed) {
            placeElement(e.x, e.y)
        }
    }
    
    override fun keyPressed(e: KeyEvent) {
        if (showSaveMenu) {
            handleSaveMenuInput(e)
            return
        }
        
        if (showLoadMenu) {
            handleLoadMenuInput(e)
            return
        }
        
        // Проверка комбинаций с Alt
        if (e.isAltDown) {
            when (e.keyCode) {
                KeyEvent.VK_1 -> currentElement = PEAT
                KeyEvent.VK_BACK_QUOTE -> currentElement = GRAY_SAND
            }
            return
        }
        
        when (e.keyCode) {
            KeyEvent.VK_1 -> currentElement = SAND
            KeyEvent.VK_2 -> currentElement = WATER
            KeyEvent.VK_3 -> currentElement = LAVA
            KeyEvent.VK_4 -> currentElement = FIRE
            KeyEvent.VK_5 -> currentElement = EARTH
            KeyEvent.VK_6 -> currentElement = STONE
            KeyEvent.VK_7 -> currentElement = SMOKE
            KeyEvent.VK_8 -> currentElement = SEED
            KeyEvent.VK_9 -> currentElement = GRASS
            KeyEvent.VK_0 -> currentElement = ERASER
            KeyEvent.VK_Q -> currentElement = WOOD
            KeyEvent.VK_W -> currentElement = ICE
            KeyEvent.VK_E -> currentElement = IRON
            KeyEvent.VK_R -> currentElement = NITROGEN
            KeyEvent.VK_T -> currentElement = UNBREAKABLE
            KeyEvent.VK_Y -> currentElement = ACID
            KeyEvent.VK_U -> currentElement = GLASS
            KeyEvent.VK_I -> currentElement = DYNAMITE
            KeyEvent.VK_O -> currentElement = GOLD
            KeyEvent.VK_P -> currentElement = COPPER
            KeyEvent.VK_A -> {
                if (e.isControlDown) {
                    roundBrush = true
                } else {
                    currentElement = SALT
                }
            }
            KeyEvent.VK_S -> {
                if (e.isControlDown) {
                    roundBrush = false
                } else {
                    currentElement = CEMENT
                }
            }
            KeyEvent.VK_D -> currentElement = RUBBER
            KeyEvent.VK_F -> currentElement = GASOLINE
            KeyEvent.VK_G -> currentElement = MERCURY
            KeyEvent.VK_H -> currentElement = URANIUM
            KeyEvent.VK_J -> currentElement = LAMP
            KeyEvent.VK_K -> currentElement = MEAT
            KeyEvent.VK_L -> {
                if (e.isControlDown) {
                    showLoadMenu = true
                    refreshSaveFiles()
                    selectedSaveIndex = if (saveFiles.isNotEmpty()) 0 else -1
                } else {
                    currentElement = PUMPKIN
                }
            }
            KeyEvent.VK_Z -> currentElement = WATERMELON
            KeyEvent.VK_X -> {
                if (e.isControlDown) {
                    showSaveMenu = true
                    saveFileName = "save_" + SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    refreshSaveFiles()
                } else {
                    currentElement = JUICE
                }
            }
            KeyEvent.VK_C -> {
                if (e.isControlDown) {
                    grid = Array(COLS) { IntArray(ROWS) }
                    heatGrid = Array(COLS) { IntArray(ROWS) }
                    fireLifeGrid = Array(COLS) { IntArray(ROWS) }
                    lightGrid = Array(COLS) { IntArray(ROWS) }
                    decayGrid = Array(COLS) { IntArray(ROWS) }
                    explosions.clear()
                } else {
                    currentElement = PUMPKIN_SEED
                }
            }
            KeyEvent.VK_V -> currentElement = WATERMELON_SEED
            KeyEvent.VK_B -> currentElement = APPLE_SEED
            KeyEvent.VK_N -> currentElement = ROTTEN_MEAT
            KeyEvent.VK_M -> currentElement = CRUDE_OIL
            KeyEvent.VK_COMMA -> currentElement = GAS
            KeyEvent.VK_PERIOD -> currentElement = LIQUEFIED_GAS
            KeyEvent.VK_SLASH -> currentElement = COAL
            KeyEvent.VK_BACK_QUOTE -> currentElement = SANDSTONE
            KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS -> brushSize = (brushSize + 1).coerceAtMost(15)
            KeyEvent.VK_MINUS -> brushSize = (brushSize - 1).coerceAtLeast(1)
            KeyEvent.VK_SPACE -> paused = !paused
            KeyEvent.VK_UP -> simulationSpeed = (simulationSpeed + 0.5f).coerceAtMost(5.0f)
            KeyEvent.VK_DOWN -> simulationSpeed = (simulationSpeed - 0.5f).coerceAtLeast(0.1f)
        }
    }
    
    private fun handleSaveMenuInput(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_ENTER -> {
                if (saveFileName.trim().isNotEmpty()) {
                    saveGame(saveFileName)
                    showSaveMenu = false
                    saveFileName = ""
                }
            }
            KeyEvent.VK_ESCAPE -> {
                showSaveMenu = false
                saveFileName = ""
            }
            KeyEvent.VK_BACK_SPACE -> {
                if (saveFileName.isNotEmpty()) {
                    saveFileName = saveFileName.substring(0, saveFileName.length - 1)
                }
            }
            else -> {
                if (e.keyChar.isLetterOrDigit() || e.keyChar == '_' || e.keyChar == '-') {
                    saveFileName += e.keyChar
                }
            }
        }
        repaint()
    }
    
    private fun handleLoadMenuInput(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_ENTER -> {
                if (selectedSaveIndex in saveFiles.indices) {
                    loadGame(saveFiles[selectedSaveIndex])
                    showLoadMenu = false
                    selectedSaveIndex = -1
                }
            }
            KeyEvent.VK_ESCAPE -> {
                showLoadMenu = false
                selectedSaveIndex = -1
            }
            KeyEvent.VK_UP -> {
                if (saveFiles.isNotEmpty()) {
                    selectedSaveIndex = (selectedSaveIndex - 1 + saveFiles.size) % saveFiles.size
                }
            }
            KeyEvent.VK_DOWN -> {
                if (saveFiles.isNotEmpty()) {
                    selectedSaveIndex = (selectedSaveIndex + 1) % saveFiles.size
                }
            }
        }
        repaint()
    }
    
    // Остальные методы интерфейсов
    override fun mouseClicked(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
    override fun mouseMoved(e: MouseEvent) {}
    override fun keyTyped(e: KeyEvent) {}
    override fun keyReleased(e: KeyEvent) {}
}