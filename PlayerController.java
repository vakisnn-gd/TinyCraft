import java.util.function.DoubleSupplier;

final class PlayerController {
    interface Host {
        boolean isCreativeMode();

        boolean isSpectatorMode();

        boolean isDeathScreenActive();

        int currentWorldDifficulty();

        void enterDeathScreen();
    }

    private final VoxelWorld world;
    private final PlayerState player;
    private final AudioEngine audio;
    private final Host host;
    private final DoubleSupplier timeSupplier;
    private final MutableVec3 playerFluidFlow = new MutableVec3();

    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean sprint;
    private boolean sneak;
    private boolean descend;
    private boolean jumpQueued;
    private boolean jumpHeld;
    private boolean creativeFlightEnabled;
    private double lastCreativeJumpTapTime = -1.0;
    private double waterAmbientCooldown;
    private double cactusDamageTimer;

    PlayerController(VoxelWorld world, PlayerState player, AudioEngine audio, Host host, DoubleSupplier timeSupplier) {
        this.world = world;
        this.player = player;
        this.audio = audio;
        this.host = host;
        this.timeSupplier = timeSupplier;
    }

    void update(double deltaTime) {
        updatePlayer(deltaTime);
    }

    private void updatePlayer(double deltaTime) {
        double moveX = 0.0;
        double moveZ = 0.0;
        boolean flightMode = host.isSpectatorMode() || (host.isCreativeMode() && creativeFlightEnabled);
        boolean wasGrounded = player.isGrounded;
        player.sneaking = sneak && !host.isSpectatorMode();
        boolean inWater = !flightMode && world.intersectsFluid(player.x, player.y, player.z, player.radius(), player.height(), GameConfig.WATER);
        player.headInWater = !flightMode && isPlayerHeadInWater();
        boolean canSprint = sprint && player.hunger > 6;
        double walkSpeed = flightMode ? GameConfig.CREATIVE_FLY_SPEED : (canSprint ? GameConfig.SPRINT_SPEED : GameConfig.WALK_SPEED);
        if (flightMode && sprint) {
            walkSpeed *= GameConfig.SPRINT_SPEED / GameConfig.WALK_SPEED;
        }
        if (player.sneaking && !flightMode) {
            walkSpeed *= GameConfig.SNEAK_SPEED_FACTOR;
        }
        if (inWater) {
            walkSpeed *= GameConfig.WATER_MOVE_FACTOR;
        }
        double sinYaw = Math.sin(player.yaw);
        double cosYaw = Math.cos(player.yaw);
        double forwardX = cosYaw;
        double forwardZ = sinYaw;
        double rightX = -sinYaw;
        double rightZ = cosYaw;

        if (forward) {
            moveX += forwardX;
            moveZ += forwardZ;
        }
        if (backward) {
            moveX -= forwardX;
            moveZ -= forwardZ;
        }
        if (left) {
            moveX -= rightX;
            moveZ -= rightZ;
        }
        if (right) {
            moveX += rightX;
            moveZ += rightZ;
        }

        double moveLengthSquared = moveX * moveX + moveZ * moveZ;
        boolean movingHorizontally = moveLengthSquared > 0.0;
        if (movingHorizontally) {
            double speedScale = walkSpeed * deltaTime / Math.sqrt(moveLengthSquared);
            moveX *= speedScale;
            moveZ *= speedScale;
        }

        double horizontalSpeed = movingHorizontally && deltaTime > 1e-8
            ? Math.sqrt(moveX * moveX + moveZ * moveZ) / deltaTime
            : 0.0;

        if (host.isSpectatorMode()) {
            jumpQueued = false;
            player.verticalVelocity = 0.0;
            player.isGrounded = false;
            player.x += moveX;
            player.z += moveZ;

            double moveY = 0.0;
            if (jumpHeld) {
                moveY += walkSpeed * deltaTime;
            }
            if (descend) {
                moveY -= walkSpeed * deltaTime;
            }
            player.y += moveY;
            updateViewBobbing(0.0, deltaTime);
            player.stepTimer = 0.0;
            player.fallDistance = 0.0;
            player.headInWater = false;
            resetPlayerAirSupply();
            player.wasInLiquid = false;
            return;
        }

        if (host.isCreativeMode() && creativeFlightEnabled) {
            jumpQueued = false;
            player.verticalVelocity = 0.0;
            tryMoveHorizontal(moveX, moveZ);

            double moveY = 0.0;
            if (jumpHeld) {
                moveY += walkSpeed * deltaTime;
            }
            if (descend) {
                moveY -= walkSpeed * deltaTime;
            }
            if (Math.abs(moveY) > 1e-8) {
                moveVertical(moveY);
            }

            player.isGrounded = isStandingOnGround();
            player.fallDistance = 0.0;
            updateViewBobbing(0.0, deltaTime);
            updateMovementAudio(movingHorizontally, deltaTime);
            player.headInWater = false;
            resetPlayerAirSupply();
            return;
        }

        if (inWater) {
            applyWaterFlowToPlayer(deltaTime);
            updateSwimmingVelocity(deltaTime);
            if (jumpQueued && canJumpFromFlowingWater()) {
                player.verticalVelocity = GameConfig.JUMP_SPEED;
                player.isGrounded = false;
                spendHunger(0.04);
            }
        } else if (jumpQueued && player.isGrounded) {
            player.verticalVelocity = GameConfig.JUMP_SPEED;
            player.isGrounded = false;
            spendHunger(sprint && movingHorizontally ? 0.16 : 0.08);
        }
        jumpQueued = false;

        tryMoveHorizontal(moveX, moveZ);
        if (!inWater) {
            player.verticalVelocity = Math.max(player.verticalVelocity - GameConfig.GRAVITY * deltaTime, -GameConfig.TERMINAL_VELOCITY);
        }
        double verticalVelocityBeforeMove = player.verticalVelocity;
        moveVertical(player.verticalVelocity * deltaTime);
        player.isGrounded = isStandingOnGround();
        updatePlayerFallDamage(wasGrounded, inWater, verticalVelocityBeforeMove, deltaTime);
        updateVoidDamage(deltaTime);
        player.headInWater = isPlayerHeadInWater();
        updateViewBobbing(horizontalSpeed, deltaTime);
        updateMovementAudio(movingHorizontally, deltaTime);
        updatePlayerAirSupply(deltaTime);
        updateSuffocationDamage(deltaTime);
        updateLavaAndFireDamage(deltaTime);
        updateCactusDamage(deltaTime);
        updatePlayerHunger(deltaTime, movingHorizontally && canSprint && player.isGrounded);
    }

    private void updatePlayerFallDamage(boolean wasGrounded, boolean inWater, double verticalVelocityBeforeMove, double deltaTime) {
        if (host.isCreativeMode() || host.isSpectatorMode() || inWater || player.health <= 0) {
            player.fallDistance = 0.0;
            return;
        }
        if (verticalVelocityBeforeMove < 0.0 && !player.isGrounded) {
            player.fallDistance += -verticalVelocityBeforeMove * deltaTime;
        }
        if (!wasGrounded && player.isGrounded) {
            double damage = Math.max(0.0, Math.floor(player.fallDistance - 4.0) * 0.5);
            if (damage > 0) {
                applyPlayerDamage(damage);
            }
            player.fallDistance = 0.0;
        } else if (player.isGrounded) {
            player.fallDistance = 0.0;
        }
    }

    private void updateVoidDamage(double deltaTime) {
        if (host.isSpectatorMode() || host.isDeathScreenActive() || player.health <= 0.0) {
            return;
        }
        if (player.y < GameConfig.WORLD_MIN_Y - 24.0) {
            player.health = 0.0;
            host.enterDeathScreen();
            return;
        }
        if (player.y < GameConfig.WORLD_MIN_Y - 8.0) {
            player.health = Math.max(0.0, player.health - 12.0 * deltaTime);
            if (player.health <= 0.0) {
                host.enterDeathScreen();
            }
        }
    }

    private void updatePlayerHunger(double deltaTime, boolean sprintingHorizontally) {
        if (host.isCreativeMode() || host.isSpectatorMode() || player.health <= 0) {
            player.hungerDrainTimer = 0.0;
            player.hungerDamageTimer = 0.0;
            return;
        }
        if (host.currentWorldDifficulty() <= 0) {
            player.hunger = GameConfig.MAX_HUNGER;
            player.hungerDamageTimer = 0.0;
            player.hungerDrainTimer = 0.0;
            if (player.health < GameConfig.MAX_HEALTH) {
                player.hungerRegenTimer += deltaTime;
                while (player.hungerRegenTimer >= 2.0 && player.health < GameConfig.MAX_HEALTH) {
                    player.hungerRegenTimer -= 2.0;
                    player.health = Math.min(GameConfig.MAX_HEALTH, player.health + 0.5);
                }
            } else {
                player.hungerRegenTimer = 0.0;
            }
            return;
        }

        double elapsedTime = Math.max(0.0, deltaTime);
        double activeHungerTime = elapsedTime;
        if (player.hungerGraceRemaining > 0.0) {
            double graceTime = Math.min(player.hungerGraceRemaining, activeHungerTime);
            player.hungerGraceRemaining = Math.max(0.0, player.hungerGraceRemaining - graceTime);
            activeHungerTime -= graceTime;
            player.hungerDrainTimer = 0.0;
            player.hungerDamageTimer = 0.0;
        }

        if (sprintingHorizontally && player.hunger > 0) {
            player.hungerDrainTimer += activeHungerTime;
            while (player.hungerDrainTimer >= GameConfig.HUNGER_SPRINT_DRAIN_SECONDS && player.hunger > 0) {
                player.hungerDrainTimer -= GameConfig.HUNGER_SPRINT_DRAIN_SECONDS;
                player.hunger = Math.max(0.0, player.hunger - 0.5);
            }
        } else {
            player.hungerDrainTimer = Math.max(0.0, player.hungerDrainTimer - activeHungerTime * 0.5);
        }
        if (player.hunger <= 0) {
            player.hungerDamageTimer += activeHungerTime;
            while (player.hungerDamageTimer >= GameConfig.HUNGER_STARVE_DAMAGE_INTERVAL && player.health > 0) {
                player.hungerDamageTimer -= GameConfig.HUNGER_STARVE_DAMAGE_INTERVAL;
                applyPlayerDamage(0.5);
            }
        } else {
            player.hungerDamageTimer = 0.0;
        }
        if (player.hunger >= GameConfig.HUNGER_REGEN_MINIMUM && player.health < GameConfig.MAX_HEALTH) {
            player.hungerRegenTimer += elapsedTime;
            while (player.hungerRegenTimer >= GameConfig.HUNGER_REGEN_INTERVAL_SECONDS
                && player.health < GameConfig.MAX_HEALTH
                && player.hunger >= GameConfig.HUNGER_REGEN_MINIMUM) {
                player.hungerRegenTimer -= GameConfig.HUNGER_REGEN_INTERVAL_SECONDS;
                player.health = Math.min(GameConfig.MAX_HEALTH,
                    player.health + GameConfig.HUNGER_REGEN_HEALTH);
                spendHungerForNaturalHealing();
            }
        } else {
            player.hungerRegenTimer = 0.0;
        }
    }

    private void spendHungerForNaturalHealing() {
        double visibleHungerCost = GameConfig.HUNGER_REGEN_COST;
        if (player.hungerGraceRemaining > 0.0) {
            double graceCost = Math.min(player.hungerGraceRemaining,
                GameConfig.HUNGER_REGEN_GRACE_COST_SECONDS);
            player.hungerGraceRemaining -= graceCost;
            visibleHungerCost *= 1.0 - graceCost / GameConfig.HUNGER_REGEN_GRACE_COST_SECONDS;
        }
        player.hunger = Math.max(0.0, player.hunger - visibleHungerCost);
    }

    private void updateSwimmingVelocity(double deltaTime) {
        if (jumpHeld) {
            player.verticalVelocity += GameConfig.WATER_SWIM_ACCELERATION * 1.5 * deltaTime;
        } else if (descend) {
            player.verticalVelocity -= GameConfig.WATER_SINK_ACCELERATION * 2.0 * deltaTime;
        } else {
            player.verticalVelocity -= GameConfig.WATER_SINK_ACCELERATION * deltaTime;
        }
        double tickScale = deltaTime / GameConfig.PHYSICS_TICK_SECONDS;
        player.verticalVelocity *= Math.pow(GameConfig.WATER_DRAG_PER_TICK, tickScale);
        player.verticalVelocity = clamp(player.verticalVelocity, -5.2, 6.4);
    }

    private void applyWaterFlowToPlayer(double deltaTime) {
        world.sampleFluidFlow(player.x, player.y, player.z, player.radius(), player.height(), GameConfig.WATER, playerFluidFlow);
        double moveX = playerFluidFlow.x * GameConfig.WATER_FLOW_PUSH * deltaTime;
        double moveZ = playerFluidFlow.z * GameConfig.WATER_FLOW_PUSH * deltaTime;
        if (!world.collides(player.x + moveX, player.y, player.z, player.radius(), player.height())) {
            player.x += moveX;
        }
        if (!world.collides(player.x, player.y, player.z + moveZ, player.radius(), player.height())) {
            player.z += moveZ;
        }
        player.verticalVelocity += playerFluidFlow.y * GameConfig.WATER_VERTICAL_FLOW_PUSH * deltaTime;
    }

    private boolean isPlayerHeadInWater() {
        return world.isPointInsideFluid(player.x, player.y + player.eyeHeight(), player.z, GameConfig.WATER)
            || world.isPointInsideFluid(player.x, player.y + player.height() - 0.08, player.z, GameConfig.WATER);
    }

    private boolean canJumpFromFlowingWater() {
        if (world.collides(player.x, player.y - 0.08, player.z, player.radius(), player.height())) {
            return false;
        }
        return world.canStandOnFluid(player.x, player.y, player.z, player.radius(), GameConfig.WATER_FLOWING);
    }

    private void updatePlayerAirSupply(double deltaTime) {
        if (host.isCreativeMode() || host.isSpectatorMode() || player.health <= 0) {
            resetPlayerAirSupply();
            return;
        }
        if (!player.headInWater) {
            recoverPlayerAirSupply(deltaTime);
            return;
        }

        player.airUnitTimer += deltaTime;
        while (player.airUnits > 0 && player.airUnitTimer >= GameConfig.AIR_UNIT_INTERVAL) {
            player.airUnitTimer -= GameConfig.AIR_UNIT_INTERVAL;
            player.airUnits--;
        }

        if (player.airUnits > 0) {
            player.drowningTimer = 0.0;
            return;
        }

        player.drowningTimer += deltaTime;
        while (player.drowningTimer >= GameConfig.DROWNING_DAMAGE_INTERVAL && player.health > 0) {
            player.drowningTimer -= GameConfig.DROWNING_DAMAGE_INTERVAL;
            applyPlayerDamage(GameConfig.DROWNING_DAMAGE);
        }
    }

    private void recoverPlayerAirSupply(double deltaTime) {
        player.drowningTimer = 0.0;
        if (player.airUnits >= GameConfig.MAX_AIR_UNITS) {
            player.airUnits = GameConfig.MAX_AIR_UNITS;
            player.airUnitTimer = 0.0;
            return;
        }

        player.airUnitTimer += deltaTime;
        while (player.airUnits < GameConfig.MAX_AIR_UNITS && player.airUnitTimer >= GameConfig.AIR_RECOVERY_INTERVAL) {
            player.airUnitTimer -= GameConfig.AIR_RECOVERY_INTERVAL;
            player.airUnits++;
        }
    }

    void resetPlayerAirSupply() {
        player.airUnits = GameConfig.MAX_AIR_UNITS;
        player.airUnitTimer = 0.0;
        player.drowningTimer = 0.0;
    }

    private void tryMoveHorizontal(double moveX, double moveZ) {
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(moveX), Math.abs(moveZ)) / GameConfig.MAX_COLLISION_STEP));
        double stepX = moveX / steps;
        double stepZ = moveZ / steps;

        for (int i = 0; i < steps; i++) {
            double stepUpY = player.y + GameConfig.CAMERA_STEP_HEIGHT;
            boolean currentlyInsideBlock = world.collides(player.x, player.y, player.z, player.radius(), player.height());

            if (!world.collides(player.x + stepX, player.y, player.z, player.radius(), player.height())
                && canSneakTo(player.x + stepX, player.z)) {
                player.x += stepX;
            } else if (!currentlyInsideBlock
                && player.isGrounded
                && !player.sneaking
                && !world.collides(player.x + stepX, stepUpY, player.z, player.radius(), player.height())) {
                player.x += stepX;
                player.y = stepUpY;
            }

            if (!world.collides(player.x, player.y, player.z + stepZ, player.radius(), player.height())
                && canSneakTo(player.x, player.z + stepZ)) {
                player.z += stepZ;
            } else if (!currentlyInsideBlock
                && player.isGrounded
                && !player.sneaking
                && !world.collides(player.x, stepUpY, player.z + stepZ, player.radius(), player.height())) {
                player.z += stepZ;
                player.y = stepUpY;
            }
        }
    }

    private boolean canSneakTo(double x, double z) {
        if (!player.sneaking || !player.isGrounded) {
            return true;
        }
        return world.collides(x, player.y - 0.08, z, player.radius(), 0.05);
    }

    private void moveVertical(double moveY) {
        if (Math.abs(moveY) < 1e-8) {
            return;
        }

        double nextY = player.y + moveY;
        if (!world.collides(player.x, nextY, player.z, player.radius(), player.height())) {
            player.y = nextY;
            return;
        }

        if (moveY < 0.0) {
            player.isGrounded = true;
        }
        player.verticalVelocity = 0.0;
    }

    boolean isStandingOnGround() {
        return world.collides(player.x, player.y - 0.05, player.z, player.radius(), player.height());
    }

    private void updateSuffocationDamage(double deltaTime) {
        if (host.isCreativeMode() || host.isSpectatorMode() || player.health <= 0) {
            player.suffocationTimer = 0.0;
            return;
        }
        if (!world.collides(player.x, player.y, player.z, player.radius(), player.height())) {
            player.suffocationTimer = 0.0;
            return;
        }

        player.suffocationTimer += deltaTime;
        while (player.suffocationTimer >= GameConfig.SUFFOCATION_INTERVAL && player.health > 0) {
            player.suffocationTimer -= GameConfig.SUFFOCATION_INTERVAL;
            applyPlayerDamage(GameConfig.SUFFOCATION_DAMAGE);
        }
    }

    private void updateLavaAndFireDamage(double deltaTime) {
        if (host.isCreativeMode() || host.isSpectatorMode() || player.health <= 0) {
            player.lavaDamageTimer = 0.0;
            player.fireDamageTimer = 0.0;
            player.fireTimer = 0.0;
            return;
        }

        if (isPlayerTouchingBlock(GameConfig.WATER)) {
            player.fireTimer = 0.0;
            player.fireDamageTimer = 0.0;
        }

        if (world.intersectsFluid(player.x, player.y, player.z, player.radius(), player.height(), GameConfig.LAVA)) {
            player.fireTimer = Math.max(player.fireTimer, 5.0);
            player.lavaDamageTimer += deltaTime;
            while (player.lavaDamageTimer >= GameConfig.LAVA_DAMAGE_INTERVAL && player.health > 0) {
                player.lavaDamageTimer -= GameConfig.LAVA_DAMAGE_INTERVAL;
                applyPlayerDamage(lavaDamageAmount());
            }
        } else {
            player.lavaDamageTimer = 0.0;
        }

        if (player.fireTimer > 0.0) {
            player.fireTimer = Math.max(0.0, player.fireTimer - deltaTime);
            player.fireDamageTimer += deltaTime;
            while (player.fireDamageTimer >= GameConfig.FIRE_DAMAGE_INTERVAL && player.health > 0) {
                player.fireDamageTimer -= GameConfig.FIRE_DAMAGE_INTERVAL;
                applyPlayerDamage(GameConfig.FIRE_DAMAGE);
            }
        } else {
            player.fireDamageTimer = 0.0;
        }
    }

    private void updateCactusDamage(double deltaTime) {
        if (host.isCreativeMode() || host.isSpectatorMode() || player.health <= 0) {
            cactusDamageTimer = 0.0;
            return;
        }
        if (!world.touchesBlock(player.x, player.y, player.z, player.radius() + 0.09, player.height(), GameConfig.CACTUS)) {
            cactusDamageTimer = 0.0;
            return;
        }
        cactusDamageTimer += deltaTime;
        while (cactusDamageTimer >= 0.65 && player.health > 0) {
            cactusDamageTimer -= 0.65;
            applyPlayerDamage(1.0);
        }
    }

    private void applyPlayerDamage(double amount) {
        if (host.isDeathScreenActive() || host.isCreativeMode() || host.isSpectatorMode() || player.health <= 0.0) {
            if (player.health <= 0.0) {
                player.health = 0.0;
            }
            return;
        }
        int armor = Math.max(0, Math.min(20, player.armorProtection));
        double protectedDamage = Math.ceil(amount * (1.0 - armor * 0.04) * 2.0) / 2.0;
        player.health = Math.max(0.0, player.health - Math.max(0.5, protectedDamage));
        if (player.health <= 0.0) {
            host.enterDeathScreen();
        }
    }

    private int lavaDamageAmount() {
        return host.currentWorldDifficulty() >= 3 ? 2 : GameConfig.LAVA_DAMAGE;
    }

    void spendHunger(double amount) {
        if (amount <= 0.0
            || host.isCreativeMode()
            || host.isSpectatorMode()
            || host.currentWorldDifficulty() <= 0
            || player.health <= 0.0
            || player.hungerGraceRemaining > 0.0) {
            return;
        }
        player.hunger = Math.max(0.0, player.hunger - amount);
        if (player.hunger < 18.0) {
            player.hungerRegenTimer = 0.0;
        }
    }

    private void updateViewBobbing(double horizontalSpeed, double deltaTime) {
        double targetAmount = player.isGrounded && horizontalSpeed > 0.05
            ? clamp(horizontalSpeed / GameConfig.SPRINT_SPEED, 0.0, 1.0)
            : 0.0;
        player.cameraBobAmount += (targetAmount - player.cameraBobAmount) * Math.min(1.0, deltaTime * 10.0);
        if (player.cameraBobAmount > 0.01) {
            player.cameraBobPhase += horizontalSpeed * deltaTime * 2.4;
        }
    }

    private void updateMovementAudio(boolean movingHorizontally, double deltaTime) {
        boolean inLiquid = isPlayerInsideLiquid();
        boolean inWater = world.intersectsFluid(player.x, player.y, player.z, player.radius(), player.height(), GameConfig.WATER);
        if (inWater != player.wasInWater) {
            audio.playSplash(player.x, player.y + 0.2, player.z);
        }
        player.wasInWater = inWater;
        player.wasInLiquid = inLiquid;

        if (host.isSpectatorMode() || !movingHorizontally || !player.isGrounded || inLiquid) {
            player.stepTimer = 0.0;
            return;
        }

        player.stepTimer -= deltaTime;
        if (player.stepTimer > 0.0) {
            return;
        }

        byte blockBelow = getBlockBelowPlayer();
        audio.playStep(blockBelow, player.x, player.y, player.z);
        player.stepTimer = sprint ? GameConfig.SPRINT_STEP_INTERVAL : GameConfig.WALK_STEP_INTERVAL;
    }

    boolean isPlayerInsideLiquid() {
        return world.intersectsFluid(player.x, player.y, player.z, player.radius(), player.height(), GameConfig.WATER)
            || world.intersectsFluid(player.x, player.y, player.z, player.radius(), player.height(), GameConfig.LAVA);
    }

    private boolean isPlayerTouchingBlock(byte block) {
        return world.touchesBlock(player.x, player.y, player.z, player.radius(), player.height(), block);
    }

    private byte getBlockBelowPlayer() {
        int blockX = (int) Math.floor(player.x);
        int blockY = (int) Math.floor(player.y - 0.12);
        int blockZ = (int) Math.floor(player.z);
        if (!world.isInside(blockX, blockY, blockZ)) {
            return GameConfig.COBBLESTONE;
        }
        return world.getBlock(blockX, blockY, blockZ);
    }

    void handleJumpPress() {
        if (host.isSpectatorMode()) {
            jumpQueued = false;
            return;
        }
        if (!host.isCreativeMode()) {
            return;
        }

        double now = timeSupplier.getAsDouble();
        if (lastCreativeJumpTapTime >= 0.0 && now - lastCreativeJumpTapTime <= 0.30) {
            creativeFlightEnabled = !creativeFlightEnabled;
            if (!creativeFlightEnabled) {
                player.verticalVelocity = 0.0;
                player.isGrounded = isStandingOnGround();
            } else {
                player.verticalVelocity = 0.0;
            }
            player.creativeMode = host.isCreativeMode();
            player.spectatorMode = host.isSpectatorMode();
            player.flightEnabled = host.isSpectatorMode() || (host.isCreativeMode() && creativeFlightEnabled);
            player.sneaking = sneak && !host.isSpectatorMode();
            lastCreativeJumpTapTime = -1.0;
        } else {
            lastCreativeJumpTapTime = now;
        }
    }

    void resetMovement() {
        forward = false;
        backward = false;
        left = false;
        right = false;
        sprint = false;
        sneak = false;
        descend = false;
        player.sneaking = false;
        jumpQueued = false;
        jumpHeld = false;
    }

    void resetPlayerDamageTimers() {
        player.suffocationTimer = 0.0;
        player.lavaDamageTimer = 0.0;
        player.fireTimer = 0.0;
        player.fireDamageTimer = 0.0;
    }

    void resetPlayerFluidState() {
        player.headInWater = false;
        player.wasInWater = false;
        player.wasInLiquid = false;
        waterAmbientCooldown = 0.0;
        resetPlayerAirSupply();
    }

    boolean isForward() {
        return forward;
    }

    void setForward(boolean forward) {
        this.forward = forward;
    }

    boolean isBackward() {
        return backward;
    }

    void setBackward(boolean backward) {
        this.backward = backward;
    }

    boolean isLeft() {
        return left;
    }

    void setLeft(boolean left) {
        this.left = left;
    }

    boolean isRight() {
        return right;
    }

    void setRight(boolean right) {
        this.right = right;
    }

    boolean isSprint() {
        return sprint;
    }

    void setSprint(boolean sprint) {
        this.sprint = sprint;
    }

    boolean isSneak() {
        return sneak;
    }

    void setSneak(boolean sneak) {
        this.sneak = sneak;
    }

    void setDescend(boolean descend) {
        this.descend = descend;
    }

    boolean isJumpQueued() {
        return jumpQueued;
    }

    void setJumpQueued(boolean jumpQueued) {
        this.jumpQueued = jumpQueued;
    }

    void setJumpHeld(boolean jumpHeld) {
        this.jumpHeld = jumpHeld;
    }

    boolean isCreativeFlightEnabled() {
        return creativeFlightEnabled;
    }

    void setCreativeFlightEnabled(boolean creativeFlightEnabled) {
        this.creativeFlightEnabled = creativeFlightEnabled;
    }

    void resetLastCreativeJumpTapTime() {
        lastCreativeJumpTapTime = -1.0;
    }

    double getWaterAmbientCooldown() {
        return waterAmbientCooldown;
    }

    void setWaterAmbientCooldown(double waterAmbientCooldown) {
        this.waterAmbientCooldown = waterAmbientCooldown;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
