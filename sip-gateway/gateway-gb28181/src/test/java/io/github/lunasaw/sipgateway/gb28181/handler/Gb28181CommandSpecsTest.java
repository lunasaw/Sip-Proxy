package io.github.lunasaw.sipgateway.gb28181.handler;

import io.github.lunasaw.sipgateway.core.api.CommandSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Gb28181CommandSpecsTest {

    @Test
    void testAllSpecsHaveGb28181Prefix() {
        List<CommandSpec> specs = Gb28181CommandSpecs.declare();
        assertFalse(specs.isEmpty(), "command specs must not be empty");
        for (CommandSpec spec : specs) {
            assertTrue(spec.type().startsWith("gb28181."),
                    "type must start with 'gb28181.': " + spec.type());
        }
    }

    @Test
    void testNoTypeDuplication() {
        List<CommandSpec> specs = Gb28181CommandSpecs.declare();
        long uniqueTypes = specs.stream().map(CommandSpec::type).distinct().count();
        assertEquals(specs.size(), uniqueTypes, "duplicate type detected in CommandSpecs");
    }

    @Test
    void testAllSpecsTargetServerCommandSender() {
        List<CommandSpec> specs = Gb28181CommandSpecs.declare();
        for (CommandSpec spec : specs) {
            assertEquals(
                    "io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender",
                    spec.senderClass().getName(),
                    "all GB28181 specs must target ServerCommandSender");
        }
    }

    @Test
    void testQueryCatalogSpec() {
        CommandSpec spec = findSpec("gb28181.Query.Catalog");
        assertEquals("deviceCatalogQuery", spec.methodName());
        assertEquals(1, spec.bindings().size());
        assertEquals("deviceId", spec.bindings().get(0).fieldName());
    }

    @Test
    void testInviteByeSpec() {
        CommandSpec spec = findSpec("gb28181.Invite.Bye");
        assertEquals("deviceBye", spec.methodName());
        assertEquals(1, spec.bindings().size());
        assertEquals("callId", spec.bindings().get(0).source());
    }

    private CommandSpec findSpec(String type) {
        return Gb28181CommandSpecs.declare().stream()
                .filter(s -> s.type().equals(type))
                .findFirst()
                .orElseThrow(() -> new AssertionError("spec not found: " + type));
    }
}
