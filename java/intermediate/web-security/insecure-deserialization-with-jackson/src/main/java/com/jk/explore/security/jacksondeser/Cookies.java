package com.jk.explore.security.jacksondeser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

/** Writes and reads a cart cookie: URL-safe Base64 of JSON. Whatever Jackson complains about becomes a short, repeatable reason. */
public final class Cookies {

    private static final Pattern LIMIT = Pattern.compile("(nesting depth|String value length|Number value length).*?maximum allowed \\((\\d+)");

    private Cookies() {
    }

    public static String write(JsonMapper mapper, Object cart) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsString(cart).getBytes(StandardCharsets.UTF_8));
    }

    /** The cookie a customer's browser would send back, made from JSON text. */
    public static String fromJson(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public static <T> T read(JsonMapper mapper, Class<T> type, String cookie) throws Refused {
        try {
            return mapper.readValue(new String(Base64.getUrlDecoder().decode(cookie), StandardCharsets.UTF_8), type);
        } catch (InvalidTypeIdException e) {
            throw new Refused(e.getOriginalMessage().contains("denied") || e.getOriginalMessage().contains("not allowed") ? "class not allowed" : "unknown type id");
        } catch (InvalidDefinitionException e) {
            throw new Refused(e.getOriginalMessage().contains("denies resolution") ? "the default validator denies class names for an Object" : "the cookie is not a cart");
        } catch (UnrecognizedPropertyException e) {
            throw new Refused("unknown property " + e.getPropertyName());
        } catch (StreamConstraintsException e) {
            Matcher m = LIMIT.matcher(e.getOriginalMessage());
            if (m.find()) {
                String what = switch (m.group(1)) {
                    case "nesting depth" -> "nesting deeper than ";
                    case "String value length" -> "text longer than ";
                    default -> "number longer than ";
                };
                throw new Refused(what + m.group(2));
            }
            throw new Refused("a stream limit was exceeded");
        } catch (JacksonException | IllegalArgumentException e) {
            throw new Refused("the cookie is not a cart");
        }
    }
}
