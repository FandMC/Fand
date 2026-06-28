package io.fand.server.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.permission.PermissionContext;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionMeta;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class PermissionManagerTest {

    @Test
    void appliesRegisteredDefaults() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor("fand.always", PermissionDefault.TRUE));
        manager.register(new PermissionDescriptor("fand.never", PermissionDefault.FALSE));
        manager.register(new PermissionDescriptor("fand.op-only", PermissionDefault.OPERATOR));
        manager.register(new PermissionDescriptor("fand.non-op", PermissionDefault.NOT_OPERATOR));

        var nonOp = new PermissionSet(false);
        var op = new PermissionSet(true);

        assertThat(manager.hasPermission(nonOp, "fand.always")).isTrue();
        assertThat(manager.hasPermission(nonOp, "fand.never")).isFalse();
        assertThat(manager.hasPermission(nonOp, "fand.op-only")).isFalse();
        assertThat(manager.hasPermission(nonOp, "fand.non-op")).isTrue();
        assertThat(manager.hasPermission(op, "fand.op-only")).isTrue();
        assertThat(manager.hasPermission(op, "fand.non-op")).isFalse();
    }

    @Test
    void explicitValuesOverrideDefaults() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor("fand.reload", PermissionDefault.FALSE));

        var subject = new PermissionSet(false).set("fand.reload", true);

        assertThat(manager.hasPermission(subject, "fand.reload")).isTrue();
    }

    @Test
    void wildcardValuesApplyFromMostSpecificToLeastSpecific() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor("fand.command.reload", PermissionDefault.FALSE));
        manager.register(new PermissionDescriptor("fand.command.info", PermissionDefault.FALSE));

        var subject = new PermissionSet(false)
                .set("fand.*", false)
                .set("fand.command.*", true)
                .set("fand.command.reload", false);

        assertThat(manager.hasPermission(subject, "fand.command.info")).isTrue();
        assertThat(manager.hasPermission(subject, "fand.command.reload")).isFalse();
    }

    @Test
    void registeredWildcardDefaultsApplyToMatchingChildren() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor("fand.command.*", PermissionDefault.OPERATOR));

        assertThat(manager.lookup("fand.command.reload")).isPresent();
        assertThat(manager.hasPermission(new PermissionSet(false), "fand.command.reload")).isFalse();
        assertThat(manager.hasPermission(new PermissionSet(true), "fand.command.reload")).isTrue();
    }

    @Test
    void parentPermissionsExpandDeclaredChildrenWhenParentIsGranted() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor(
                "fand.admin",
                PermissionDefault.FALSE,
                Map.of(
                        "fand.command.reload", true,
                        "fand.command.danger", false
                )
        ));

        var subject = new PermissionSet(false);
        assertThat(manager.hasPermission(subject, "fand.command.reload")).isFalse();

        subject.set("fand.admin", true);

        assertThat(manager.hasPermission(subject, "fand.command.reload")).isTrue();
        assertThat(manager.hasPermission(subject, "fand.command.danger")).isFalse();
    }

    @Test
    void explicitChildPermissionOverridesInheritedParentChildren() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor(
                "fand.admin",
                PermissionDefault.FALSE,
                Map.of("fand.command.reload", true)
        ));

        var subject = new PermissionSet(false)
                .set("fand.admin", true)
                .set("fand.command.reload", false);

        assertThat(manager.hasPermission(subject, "fand.command.reload")).isFalse();
    }

    @Test
    void mostSpecificParentPermissionControlsDeclaredChildren() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor(
                "fand.admin",
                PermissionDefault.FALSE,
                Map.of("fand.command.reload", true)
        ));
        manager.register(new PermissionDescriptor(
                "fand.admin.commands",
                PermissionDefault.FALSE,
                Map.of("fand.command.reload", false)
        ));

        var subject = new PermissionSet(false)
                .set("fand.admin", true)
                .set("fand.admin.commands", true);

        assertThat(manager.hasPermission(subject, "fand.command.reload")).isFalse();
    }

    @Test
    void unregisterNamespacesRemovesDeclaredChildIndexEntries() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor(
                "demo.admin",
                PermissionDefault.FALSE,
                Map.of("demo.command.reload", true)
        ));

        var subject = new PermissionSet(false).set("demo.admin", true);
        assertThat(manager.hasPermission(subject, "demo.command.reload")).isTrue();

        manager.unregisterNamespaces(Set.of("demo"));

        assertThat(manager.hasPermission(subject, "demo.command.reload")).isFalse();
    }

    @Test
    void attachmentsOverrideSubjectValuesAndRegisteredDefaults() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor("fand.command.reload", PermissionDefault.FALSE));
        var subject = new PermissionSet(false).set("fand.command.reload", false);
        var attachment = manager.attach(subject, "fand.command.reload", true);

        assertThat(manager.hasPermission(subject, "fand.command.reload")).isTrue();

        attachment.close();

        assertThat(manager.hasPermission(subject, "fand.command.reload")).isFalse();
    }

    @Test
    void newerAttachmentsTakePriorityAndStillSupportWildcards() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor("fand.command.reload", PermissionDefault.FALSE));
        var subject = new PermissionSet(false);
        manager.attach(subject, "fand.command.*", true);
        var newer = manager.attach(subject, "fand.command.reload", false);

        assertThat(manager.hasPermission(subject, "fand.command.reload")).isFalse();

        newer.close();

        assertThat(manager.hasPermission(subject, "fand.command.reload")).isTrue();
    }

    @Test
    void returnsFalseForUnknownPermissionsWithoutExplicitGrant() {
        var manager = new PermissionManager();

        assertThat(manager.hasPermission(new PermissionSet(false), "fand.unknown")).isFalse();
    }

    @Test
    void recalculateEntrypointsKeepRealtimePermissionState() {
        var manager = new PermissionManager();
        manager.register(new PermissionDescriptor("fand.command.reload", PermissionDefault.FALSE));
        var subject = new PermissionSet(false);

        assertThat(manager.hasPermission(subject, "fand.command.reload")).isFalse();

        subject.set("fand.command.reload", true);
        manager.recalculate(subject);

        assertThat(manager.hasPermission(subject, "fand.command.reload")).isTrue();

        manager.recalculateAll();

        assertThat(manager.hasPermission(subject, "fand.command.reload")).isTrue();
    }

    @Test
    void ecosystemMetadataQueriesDefaultToEmpty() {
        var manager = new PermissionManager();
        var subject = new PermissionSet(false);
        var context = PermissionContext.of("world", "overworld");

        assertThat(manager.meta(subject, context)).isEqualTo(PermissionMeta.empty());
        assertThat(manager.prefix(subject, context)).isEmpty();
        assertThat(manager.suffix(subject, context)).isEmpty();
        assertThat(manager.metaValue(subject, context, "chat-color")).isEmpty();
        assertThat(manager.primaryGroup(subject, context)).isEmpty();
        assertThat(manager.groups(subject, context)).isEmpty();
        assertThat(manager.group("default", context)).isEmpty();
        assertThat(manager.groups(context)).isEmpty();
    }

    @Test
    void rejectsInvalidPermissionNodes() {
        var manager = new PermissionManager();

        assertThatThrownBy(() -> manager.register(new PermissionDescriptor("Bad Node", PermissionDefault.FALSE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.hasPermission(new PermissionSet(false), "bad node"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
