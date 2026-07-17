import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChatSystemTest {
    @Test
    public void historyKeepsNewestHundredMessages() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        for (int i = 0; i < 105; i++) {
            chat.addMessage("message-" + i);
        }

        List<String> messages = chat.visibleMessages();
        assertEquals(100, messages.size());
        assertEquals("message-5", messages.get(0));
        assertEquals("message-104", messages.get(messages.size() - 1));
    }

    @Test
    public void closedChatShowsFiveNewestMessagesAndFadesForLastTwoSeconds() {
        AtomicLong now = new AtomicLong();
        ChatSystem chat = new ChatSystem(now::get);
        for (int i = 0; i < 6; i++) {
            chat.addMessage("message-" + i);
        }

        List<ChatSystem.VisibleMessage> recent = chat.renderMessages();
        assertEquals(5, recent.size());
        assertEquals("message-1", recent.get(0).text());
        assertEquals(1.0f, recent.get(0).alpha(), 0.001f);

        now.set(7_000_000_000L);
        List<ChatSystem.VisibleMessage> fading = chat.renderMessages();
        assertEquals(5, fading.size());
        assertEquals(0.5f, fading.get(0).alpha(), 0.001f);

        now.set(8_000_000_000L);
        assertTrue(chat.renderMessages().isEmpty());
    }

    @Test
    public void openChatShowsFullHistoryWithoutAgeFade() {
        AtomicLong now = new AtomicLong();
        ChatSystem chat = new ChatSystem(now::get);
        for (int i = 0; i < 12; i++) {
            chat.addMessage("message-" + i);
        }
        now.set(20_000_000_000L);
        chat.open();

        List<ChatSystem.VisibleMessage> messages = chat.renderMessages();
        assertEquals(12, messages.size());
        assertEquals(1.0f, messages.get(0).alpha(), 0.001f);
    }

    @Test
    public void mouseWheelScrollOffsetOnlyChangesWhileChatIsOpen() {
        ChatSystem chat = new ChatSystem(() -> 0L);

        chat.scroll(1);
        assertEquals(0, chat.scrollOffset());

        chat.open();
        chat.scroll(1);
        chat.scroll(1);
        assertEquals(6, chat.scrollOffset());
        chat.scroll(-1);
        assertEquals(3, chat.scrollOffset());

        chat.close();
        assertEquals(0, chat.scrollOffset());
    }

    @Test
    public void clearingMessagesRemovesPreviousWorldChatAndResetsScroll() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        chat.addMessage("old world");
        chat.open();
        chat.scroll(1);

        chat.clearMessages();

        assertTrue(chat.visibleMessages().isEmpty());
        assertEquals(0, chat.scrollOffset());
    }

    @Test
    public void arrowKeysWalkSubmittedInputAndRestoreTheDraft() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        submit(chat, "/seed");
        submit(chat, "/whereami");

        chat.open();
        append(chat, "/gi");
        chat.previousInput();
        assertEquals("/whereami", chat.inputText());
        chat.previousInput();
        assertEquals("/seed", chat.inputText());
        chat.nextInput();
        assertEquals("/whereami", chat.inputText());
        chat.nextInput();
        assertEquals("/gi", chat.inputText());
    }

    @Test
    public void tabCyclesCommandMatchesAndShiftTabCyclesBackwards() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        chat.open();
        append(chat, "/g");

        chat.cycleSuggestion(false);
        assertEquals("/gamemode", chat.inputText());
        chat.cycleSuggestion(false);
        assertEquals("/give", chat.inputText());
        chat.cycleSuggestion(true);
        assertEquals("/gamemode", chat.inputText());
    }

    @Test
    public void suggestionsShowCommandSyntaxAndContextArguments() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        chat.open();
        append(chat, "/");
        List<ChatSystem.CommandSuggestion> commands = chat.commandSuggestions();
        assertEquals(6, commands.size());
        assertTrue(commands.get(0).text().startsWith("/blockinfo"));

        chat.close();
        chat.open();
        append(chat, "/gamemode ");
        List<ChatSystem.CommandSuggestion> modes = chat.commandSuggestions();
        assertEquals(3, modes.size());
        assertEquals("creative", modes.get(0).text());
        chat.cycleSuggestion(false);
        assertEquals("/gamemode creative", chat.inputText());
    }

    @Test
    public void disabledCheatsBlockGameplayCommandsButKeepHelpAvailable() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        DummyCommandTarget target = new DummyCommandTarget();
        target.cheatsAllowed = false;

        submit(chat, "/gamemode creative", target);
        assertEquals(0, target.gameModeChanges);
        assertTrue(chat.visibleMessages().get(chat.visibleMessages().size() - 1).contains("not enabled"));

        submit(chat, "/help", target);
        assertTrue(chat.visibleMessages().get(chat.visibleMessages().size() - 1).contains("Commands:"));
    }

    @Test
    public void multiplayerGameplayCommandIsForwardedBeforeLocalPermissionCheck() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        DummyCommandTarget target = new DummyCommandTarget();
        target.cheatsAllowed = false;
        target.remoteCommands = true;

        submit(chat, "/gamemode creative", target);

        assertEquals(0, target.gameModeChanges);
        assertEquals("/gamemode creative", target.sentMessage);
    }

    @Test
    public void disabledCheatsHideGameplayCommandSuggestions() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        chat.setCheatSuggestionsEnabled(false);
        chat.open();
        append(chat, "/");

        List<ChatSystem.CommandSuggestion> suggestions = chat.commandSuggestions();

        assertTrue(suggestions.stream().anyMatch(suggestion -> suggestion.text().startsWith("/help")));
        assertFalse(suggestions.stream().anyMatch(suggestion -> suggestion.text().startsWith("/gamemode")));
    }

    @Test
    public void tpKillAndSummonCommandsUseExistingGameActions() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        DummyCommandTarget target = new DummyCommandTarget();

        submit(chat, "/tp 12 70 -4", target);
        assertEquals(12.0, target.teleportX, 0.001);
        assertEquals(70.0, target.teleportY, 0.001);
        assertEquals(-4.0, target.teleportZ, 0.001);

        submit(chat, "/summon minecraft:skeleton", target);
        assertEquals(MobKind.SKELETON, target.summonedMob);
        assertEquals(null, ChatSystem.resolveMobKind("other:zombie"));

        submit(chat, "/kill", target);
        assertTrue(target.killed);
    }

    @Test
    public void msgIsForwardedWithoutExtraAliases() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        DummyCommandTarget target = new DummyCommandTarget();

        submit(chat, "/msg Alex hello", target);
        assertEquals("/msg Alex hello", target.sentMessage);

        submit(chat, "/teleport 1 2 3", target);
        assertTrue(chat.visibleMessages().get(chat.visibleMessages().size() - 1).contains("Unknown command"));
    }

    @Test
    public void setBlockAndFillResolveBlockNamesAndLimitFillVolume() {
        ChatSystem chat = new ChatSystem(() -> 0L);
        DummyCommandTarget target = new DummyCommandTarget();

        submit(chat, "/setblock 1 70 -2 stone", target);
        assertEquals(1, target.setBlockCalls);
        assertEquals(GameConfig.STONE & 0xFF, target.lastBlock.type.numericId);

        submit(chat, "/fill 0 70 0 1 71 1 tinycraft:dirt", target);
        assertEquals(1, target.fillCalls);
        assertEquals(8, target.lastFillVolume);
        assertEquals(GameConfig.DIRT & 0xFF, target.lastBlock.type.numericId);

        submit(chat, "/fill 0 0 0 32 32 32 stone", target);
        assertEquals(1, target.fillCalls);
        assertTrue(chat.visibleMessages().get(chat.visibleMessages().size() - 1).contains("Maximum"));
        assertEquals(null, ChatSystem.resolveCommandBlockState("other:stone"));
    }

    private static void submit(ChatSystem chat, String text) {
        submit(chat, text, new DummyCommandTarget());
    }

    private static void submit(ChatSystem chat, String text, ChatSystem.CommandTarget target) {
        chat.open();
        append(chat, text);
        chat.submit(target);
    }

    private static void append(ChatSystem chat, String text) {
        for (int i = 0; i < text.length(); i++) {
            chat.appendCharacter(text.charAt(i));
        }
    }

    private static final class DummyCommandTarget implements ChatSystem.CommandTarget {
        private boolean cheatsAllowed = true;
        private boolean remoteCommands;
        private int gameModeChanges;
        private String sentMessage = "";
        private double teleportX;
        private double teleportY;
        private double teleportZ;
        private boolean killed;
        private MobKind summonedMob;
        private int setBlockCalls;
        private int fillCalls;
        private int lastFillVolume;
        private BlockState lastBlock;

        @Override public void teleportPlayer(double x, double y, double z) { teleportX = x; teleportY = y; teleportZ = z; }
        @Override public void setWorldTime(double worldTime) {}
        @Override public void setGameMode(String mode) { gameModeChanges++; }
        @Override public void clearInventory() {}
        @Override public boolean giveItem(byte itemId, int amount) { return true; }
        @Override public void killPlayer() { killed = true; }
        @Override public boolean summonMob(MobKind kind) { summonedMob = kind; return true; }
        @Override public int setBlock(int x, int y, int z, BlockState state) { setBlockCalls++; lastBlock = state; return 1; }
        @Override public int fillBlocks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
            fillCalls++;
            lastBlock = state;
            lastFillVolume = (int) ChatSystem.fillBlockCount(minX, minY, minZ, maxX, maxY, maxZ);
            return lastFillVolume;
        }
        @Override public String currentSeed() { return "0"; }
        @Override public String locateBiome(String biomeName) { return ""; }
        @Override public String locateStructure(String structureName) { return ""; }
        @Override public String placeStructure(String structureName, int rotation) { return ""; }
        @Override public String listStructures() { return ""; }
        @Override public String currentDebugLocation() { return ""; }
        @Override public String terrainDebugAt(int x, int z) { return ""; }
        @Override public String heightTest() { return ""; }
        @Override public String blockInfo() { return ""; }
        @Override public void sendChat(String message) { sentMessage = message; }
        @Override public boolean canUseCheatCommands() { return cheatsAllowed; }
        @Override public boolean shouldHandleCommandRemotely(String command) { return remoteCommands; }
    }
}
