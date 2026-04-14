package infra.http;

public final class Json {
    private Json() {
    }

    public static String readString(String body, String fieldName) {
        String rawValue = readRawValue(body, fieldName);
        if (rawValue == null) {
            return "";
        }

        if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
            return unescape(rawValue.substring(1, rawValue.length() - 1));
        }

        return rawValue;
    }

    public static int readInt(String body, String fieldName, int defaultValue) {
        String rawValue = readRawValue(body, fieldName);
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(rawValue.replace("\"", "").trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    public static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String readRawValue(String body, String fieldName) {
        if (body == null || body.isBlank()) {
            return null;
        }

        String search = "\"" + fieldName + "\"";
        int nameIndex = body.indexOf(search);
        if (nameIndex < 0) {
            return null;
        }

        int colonIndex = body.indexOf(':', nameIndex + search.length());
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < body.length() && Character.isWhitespace(body.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= body.length()) {
            return null;
        }

        char firstChar = body.charAt(valueStart);
        if (firstChar == '"') {
            int valueEnd = valueStart + 1;
            boolean escaped = false;

            while (valueEnd < body.length()) {
                char current = body.charAt(valueEnd);
                if (current == '"' && !escaped) {
                    return body.substring(valueStart, valueEnd + 1);
                }
                escaped = current == '\\' && !escaped;
                if (current != '\\') {
                    escaped = false;
                }
                valueEnd++;
            }
            return null;
        }

        int valueEnd = valueStart;
        while (valueEnd < body.length()) {
            char current = body.charAt(valueEnd);
            if (current == ',' || current == '}' || Character.isWhitespace(current)) {
                break;
            }
            valueEnd++;
        }

        return body.substring(valueStart, valueEnd).trim();
    }

    private static String unescape(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
