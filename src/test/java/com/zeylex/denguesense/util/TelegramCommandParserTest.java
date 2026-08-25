package com.zeylex.denguesense.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TelegramCommandParser")
class TelegramCommandParserTest {

    @Test
    void startDeepLink_extractsCode() {
        assertThat(TelegramCommandParser.extractCode("/start PHI-9238B766")).isEqualTo("PHI-9238B766");
        assertThat(TelegramCommandParser.isConnectCommand("/start PHI-9238B766")).isTrue();
    }

    @Test
    void registerCommand_extractsCode() {
        assertThat(TelegramCommandParser.extractCode("/register PHI-9238B766")).isEqualTo("PHI-9238B766");
    }

    @Test
    void startWithoutCode_isConnectButNoCode() {
        assertThat(TelegramCommandParser.isConnectCommand("/start")).isTrue();
        assertThat(TelegramCommandParser.extractCode("/start")).isNull();
    }

    @Test
    void otherMessages_ignored() {
        assertThat(TelegramCommandParser.isConnectCommand("hello")).isFalse();
        assertThat(TelegramCommandParser.extractCode("\\register PHI-9238B766")).isNull();
        assertThat(TelegramCommandParser.classify("hello")).isEqualTo(TelegramCommandParser.Command.UNKNOWN);
    }

    @Test
    void clustersAndHelp_fromSlashAndNavButtons() {
        assertThat(TelegramCommandParser.classify("/clusters")).isEqualTo(TelegramCommandParser.Command.CLUSTERS);
        assertThat(TelegramCommandParser.classify("/live")).isEqualTo(TelegramCommandParser.Command.CLUSTERS);
        assertThat(TelegramCommandParser.classify("🔴 Live clusters")).isEqualTo(TelegramCommandParser.Command.CLUSTERS);
        assertThat(TelegramCommandParser.classify("/help")).isEqualTo(TelegramCommandParser.Command.HELP);
        assertThat(TelegramCommandParser.classify("ℹ️ Help")).isEqualTo(TelegramCommandParser.Command.HELP);
    }
}
