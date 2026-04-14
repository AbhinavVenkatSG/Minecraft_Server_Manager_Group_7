package infra.http;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonTest {

    @Test
    void readString_withQuotedValue_returnsUnescapedString() {
        String body = "{\"name\":\"test\"}";
        assertEquals("test", Json.readString(body, "name"));
    }

    @Test
    void readString_withMissingField_returnsEmptyString() {
        String body = "{\"other\":\"value\"}";
        assertEquals("", Json.readString(body, "name"));
    }

    @Test
    void readString_withNullBody_returnsEmptyString() {
        assertEquals("", Json.readString(null, "name"));
    }

    @Test
    void readString_withBlankBody_returnsEmptyString() {
        assertEquals("", Json.readString("", "name"));
        assertEquals("", Json.readString("   ", "name"));
    }

    @Test
    void readString_withEscapedNewline_returnsUnescaped() {
        String body = "{\"text\":\"line1\\nline2\"}";
        assertEquals("line1\nline2", Json.readString(body, "text"));
    }

    @Test
    void readString_withEscapedCarriageReturn_returnsUnescaped() {
        String body = "{\"text\":\"line1\\rline2\"}";
        assertEquals("line1\rline2", Json.readString(body, "text"));
    }

    @Test
    void readString_withEscapedQuote_returnsUnescaped() {
        String body = "{\"text\":\"say \\\"hello\\\"\"}";
        assertEquals("say \"hello\"", Json.readString(body, "text"));
    }

    @Test
    void readString_withEscapedBackslash_returnsUnescaped() {
        String body = "{\"path\":\"C:\\\\Users\"}";
        assertEquals("C:\\Users", Json.readString(body, "path"));
    }

    @Test
    void readString_withUnquotedValue_returnsRawValue() {
        String body = "{\"count\":42}";
        assertEquals("42", Json.readString(body, "count"));
    }

    @Test
    void readString_withWhitespaceAfterColon_handlesCorrectly() {
        String body = "{\"name\" : \"value\"}";
        assertEquals("value", Json.readString(body, "name"));
    }

    @Test
    void readString_withFieldAtEndOfObject_handlesCorrectly() {
        String body = "{\"other\":1,\"name\":\"last\"}";
        assertEquals("last", Json.readString(body, "name"));
    }

    @Test
    void readString_withEmptyValue_returnsEmptyString() {
        String body = "{\"name\":\"\"}";
        assertEquals("", Json.readString(body, "name"));
    }

    @Test
    void readString_withMultipleOccurrences_returnsFirst() {
        String body = "{\"name\":\"first\",\"name\":\"second\"}";
        assertEquals("first", Json.readString(body, "name"));
    }

    @Test
    void readInt_withValidInteger_returnsParsedValue() {
        String body = "{\"port\":25565}";
        assertEquals(25565, Json.readInt(body, "port", 0));
    }

    @Test
    void readInt_withQuotedInteger_returnsParsedValue() {
        String body = "{\"port\":\"25565\"}";
        assertEquals(25565, Json.readInt(body, "port", 0));
    }

    @Test
    void readInt_withMissingField_returnsDefaultValue() {
        String body = "{\"other\":100}";
        assertEquals(8080, Json.readInt(body, "port", 8080));
    }

    @Test
    void readInt_withInvalidFormat_returnsDefaultValue() {
        String body = "{\"port\":\"not-a-number\"}";
        assertEquals(8080, Json.readInt(body, "port", 8080));
    }

    @Test
    void readInt_withNullBody_returnsDefaultValue() {
        assertEquals(8080, Json.readInt(null, "port", 8080));
    }

    @Test
    void readInt_withBlankValue_returnsDefaultValue() {
        String body = "{\"port\":\"   \"}";
        assertEquals(8080, Json.readInt(body, "port", 8080));
    }

    @Test
    void readInt_withNegativeNumber_returnsParsedValue() {
        String body = "{\"offset\":-10}";
        assertEquals(-10, Json.readInt(body, "offset", 0));
    }

    @Test
    void readInt_withZeroValue_returnsZero() {
        String body = "{\"count\":0}";
        assertEquals(0, Json.readInt(body, "count", 999));
    }

    @Test
    void quote_withString_wrapsInQuotes() {
        assertEquals("\"hello\"", Json.quote("hello"));
    }

    @Test
    void quote_withEmptyString_returnsEmptyQuotes() {
        assertEquals("\"\"", Json.quote(""));
    }

    @Test
    void quote_withNullString_returnsEmptyQuotes() {
        assertEquals("\"\"", Json.quote(null));
    }

    @Test
    void quote_withSpecialCharacters_escapesCorrectly() {
        assertEquals("\"line1\\nline2\"", Json.quote("line1\nline2"));
        assertEquals("\"line1\\rline2\"", Json.quote("line1\rline2"));
        assertEquals("\"say \\\"hi\\\"\"", Json.quote("say \"hi\""));
        assertEquals("\"path\\\\file\"", Json.quote("path\\file"));
    }

    @Test
    void escape_withNull_returnsEmptyString() {
        assertEquals("", Json.escape(null));
    }

    @Test
    void escape_withAllSpecialCharacters_escapesCorrectly() {
        String input = "test\\n\r\"end";
        String result = Json.escape(input);
        assertTrue(result.contains("\\\\"));
        assertTrue(result.contains("\\n"));
        assertTrue(result.contains("\\r"));
        assertTrue(result.contains("\\\""));
    }

    @Test
    void roundTrip_preservesOriginalValue() {
        String original = "Hello \"World\"\nLine2\rEnd";
        String quoted = Json.quote(original);
        String body = "{\"text\":" + quoted + "}";
        String extracted = Json.readString(body, "text");
        assertEquals(original, extracted);
    }

    @Test
    void nestedQuotes_inJsonValue_handledCorrectly() {
        String body = "{\"json\":\"{\\\"key\\\":\\\"value\\\"}\"}";
        String value = Json.readString(body, "json");
        assertEquals("{\"key\":\"value\"}", value);
    }

    @Test
    void readString_withTrailingComma_handlesGracefully() {
        String body = "{\"name\":\"value\",}";
        assertEquals("value", Json.readString(body, "name"));
    }

    @Test
    void readString_withNumericValue_returnsAsString() {
        String body = "{\"count\":12345}";
        assertEquals("12345", Json.readString(body, "count"));
    }

    @Test
    void readString_withBooleanValue_returnsAsString() {
        String body = "{\"enabled\":true}";
        assertEquals("true", Json.readString(body, "enabled"));
    }
}
