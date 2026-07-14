package fr.maxlego08.zregions.common.command.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArgumentTokenizerTest {

    @Test
    void executeSplitsOnSpaces() {
        assertEquals(List.of("create", "spawn", "10"), ArgumentTokenizer.EXECUTE.tokenizeInput("create spawn 10"));
    }

    @Test
    void executeKeepsQuotedTokensTogether() {
        assertEquals(List.of("flag", "my region", "pvp", "deny"),
                ArgumentTokenizer.EXECUTE.tokenizeInput("flag \"my region\" pvp deny"));
    }

    @Test
    void executeDropsTrailingEmptyToken() {
        assertEquals(List.of("create", "spawn"), ArgumentTokenizer.EXECUTE.tokenizeInput("create spawn "));
    }

    @Test
    void tabCompletePreservesTrailingEmptyToken() {
        assertEquals(List.of("create", "spawn", ""), ArgumentTokenizer.TAB_COMPLETE.tokenizeInput("create spawn "));
    }

    @Test
    void tabCompleteKeepsLastPartialWithoutTrailingSpace() {
        assertEquals(List.of("create", "spa"), ArgumentTokenizer.TAB_COMPLETE.tokenizeInput("create spa"));
    }

    @Test
    void emptyInputYieldsNoTokens() {
        assertEquals(List.of(), ArgumentTokenizer.EXECUTE.tokenizeInput(""));
        assertEquals(List.of(), ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(""));
    }

    @Test
    void arrayOverloadJoinsWithSpaces() {
        assertEquals(List.of("flag", "my region", "pvp"),
                ArgumentTokenizer.EXECUTE.tokenizeInput(new String[]{"flag", "\"my", "region\"", "pvp"}));
    }

    @Test
    void unclosedQuoteReadsToEndOfInput() {
        assertEquals(List.of("flag", "my region"), ArgumentTokenizer.EXECUTE.tokenizeInput("flag \"my region"));
    }

    @Test
    void smartQuotesAreTreatedAsQuotes() {
        assertEquals(List.of("my region"), ArgumentTokenizer.EXECUTE.tokenizeInput("“my region”"));
    }

    @Test
    void emptyQuotedTokenIsPreservedInTheMiddle() {
        assertEquals(List.of("flag", "", "pvp"), ArgumentTokenizer.EXECUTE.tokenizeInput("flag \"\" pvp"));
    }

    @Test
    void quotedTrailingTokenIsNotTreatedAsEmpty() {
        assertEquals(List.of("remove", "spawn"), ArgumentTokenizer.TAB_COMPLETE.tokenizeInput("remove \"spawn\""));
    }
}
